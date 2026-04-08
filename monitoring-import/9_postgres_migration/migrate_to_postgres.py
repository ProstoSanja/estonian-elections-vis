"""
One-time migration script: MongoDB (monitoring_entries, monitoring_entry_connections)
-> PostgreSQL (monitoring_entry, monitoring_entry_external_id, monitoring_entry_name,
              monitoring_connection, monitoring_connection_candidate)

Prerequisites:
  - PostgreSQL has V2 migration applied (monitoring tables exist and are empty)
  - MongoDB is running with the election-vis database populated
  - pip install psycopg2-binary pymongo
"""

import json
import sys
from datetime import datetime, date

from pymongo import MongoClient
import psycopg2

# --- Configuration ---
MONGO_URI = "mongodb://localhost:27017"
MONGO_DB = "election-vis"

PG_DSN = "dbname=election-vis user=postgres_user password=postgres_pass host=localhost port=5432"

# Maps MongoDB field name -> PostgreSQL enum value
EXTERNAL_ID_TYPES = {
    "estGovId": "EST_GOV_ID",
    "ariregisterAnonId": "ARIREGISTER_ANON_ID",
    "erjkId": "ERJK_ID",
    "riigikoguGuid": "RIIGIKOGU_GUID",
}


def json_serial(obj):
    """Fallback serializer for json.dumps — handles datetime and ObjectId."""
    if isinstance(obj, (datetime, date)):
        return obj.isoformat()
    return str(obj)


def to_date(d):
    """Convert MongoDB datetime or date to Python date (or None)."""
    if d is None:
        return None
    if isinstance(d, datetime):
        return d.date()
    if isinstance(d, date):
        return d
    return None


def migrate_entries(db, cur):
    """
    Migrate monitoring_entries -> monitoring_entry + monitoring_entry_external_id + monitoring_entry_name.
    Returns (oid_to_bigint, entry_types) mappings.
    """
    oid_to_bigint = {}
    entry_types = {}

    entries = list(db.monitoring_entries.find())
    total = len(entries)
    print(f"Migrating {total} entries...")

    ext_id_count = 0
    name_count = 0
    ext_id_duplicates = []

    for i, entry in enumerate(entries):
        oid = str(entry["_id"])
        entry_type = entry.get("type", "BUSINESS")
        entry_name = entry.get("name", "")
        birth_date = to_date(entry.get("birthDate"))

        # Build extra JSONB from unstructuredData
        unstructured = entry.get("unstructuredData") or {}
        extra = json.dumps(unstructured, default=json_serial) if unstructured else "{}"

        # Insert monitoring_entry
        cur.execute("""
            INSERT INTO monitoring_entry (type, name, birth_date, extra)
            VALUES (%s, %s, %s, CAST(%s AS jsonb))
            RETURNING id
        """, (entry_type, entry_name, birth_date, extra))
        new_id = cur.fetchone()[0]

        oid_to_bigint[oid] = new_id
        entry_types[new_id] = entry_type

        # Insert external IDs
        ids_obj = entry.get("ids") or {}
        for mongo_field, pg_type in EXTERNAL_ID_TYPES.items():
            value = ids_obj.get(mongo_field)
            if value:
                cur.execute("SAVEPOINT sp_extid")
                try:
                    cur.execute("""
                        INSERT INTO monitoring_entry_external_id (entry_id, type, value)
                        VALUES (%s, %s, %s)
                    """, (new_id, pg_type, value))
                    cur.execute("RELEASE SAVEPOINT sp_extid")
                    ext_id_count += 1
                except psycopg2.errors.UniqueViolation:
                    cur.execute("ROLLBACK TO SAVEPOINT sp_extid")
                    ext_id_duplicates.append((oid, pg_type, value, entry_name))

        # Insert names from altNames (all entries have altNames after step 8 migration)
        alt_names = entry.get("altNames") or [{"fullName": entry_name}]

        for j, an in enumerate(alt_names):
            full = an.get("fullName") or entry_name
            cur.execute("""
                INSERT INTO monitoring_entry_name
                    (entry_id, full_name, first_name, last_name,
                     business_name, business_suffix, start_date, end_date)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """, (
                new_id,
                full,
                an.get("firstName"),
                an.get("lastName"),
                an.get("businessName"),
                an.get("businessSuffix"),
                to_date(an.get("startDate")),
                to_date(an.get("endDate")),
            ))
            name_count += 1

        if (i + 1) % 10000 == 0:
            print(f"  Progress: {i + 1}/{total} entries")

    print(f"  Entries:      {total}")
    print(f"  External IDs: {ext_id_count}")
    print(f"  Names:        {name_count}")
    if ext_id_duplicates:
        print(f"  !! Duplicate external IDs (skipped): {len(ext_id_duplicates)}")
        for oid, id_type, value, name in ext_id_duplicates[:10]:
            print(f"     {id_type}={value} for '{name}' (oid={oid})")
        if len(ext_id_duplicates) > 10:
            print(f"     ... and {len(ext_id_duplicates) - 10} more")

    return oid_to_bigint, entry_types


def migrate_connections(db, cur, oid_to_bigint, entry_types):
    """
    Migrate monitoring_entry_connections -> monitoring_connection + monitoring_connection_candidate.

    connectedIds splitting logic:
      - 1 ID:  entry_a only (e.g. gov body with unknown parent)
      - 2 IDs: entry_a + entry_b (normal confirmed connection)
      - >2 IDs: ambiguous — one known side (non-INDIVIDUAL) becomes entry_a,
                the rest become candidates in monitoring_connection_candidate
    """
    connections = list(db.monitoring_entry_connections.find())
    total = len(connections)
    print(f"\nMigrating {total} connections...")

    conn_count = 0
    candidate_count = 0
    unmapped_count = 0
    multi_id_count = 0

    for i, conn in enumerate(connections):
        connected_oids = conn.get("connectedIds") or []
        if not connected_oids:
            unmapped_count += 1
            continue

        # Map all ObjectIds to new BIGINT IDs
        mapped_ids = []
        skip = False
        for oid in connected_oids:
            bigint = oid_to_bigint.get(str(oid))
            if bigint is None:
                unmapped_count += 1
                skip = True
                break
            mapped_ids.append(bigint)
        if skip:
            continue

        # Map parentId
        parent_oid = conn.get("parentId")
        parent_id = oid_to_bigint.get(str(parent_oid)) if parent_oid else None

        # Flatten sources from [{"sourceUrl": "..."}] to ["..."]
        # Strip NUL characters — PostgreSQL rejects \x00 in string literals
        raw_sources = conn.get("sources") or []
        sources = [s.get("sourceUrl", "").replace("\x00", "") for s in raw_sources if s.get("sourceUrl")]

        # Determine entry_a, entry_b, and candidates
        entry_a = None
        entry_b = None
        candidates = []

        if len(mapped_ids) == 1:
            entry_a = mapped_ids[0]
        elif len(mapped_ids) == 2:
            entry_a = mapped_ids[0]
            entry_b = mapped_ids[1]
        else:
            # >2 IDs: separate the known side (non-INDIVIDUAL) from candidates
            multi_id_count += 1
            non_indiv = [mid for mid in mapped_ids if entry_types.get(mid) != "INDIVIDUAL"]
            individuals = [mid for mid in mapped_ids if entry_types.get(mid) == "INDIVIDUAL"]

            if len(non_indiv) != 1:
                raise ValueError(
                    f"Connection {conn['_id']} has {len(mapped_ids)} IDs "
                    f"with {len(non_indiv)} non-INDIVIDUAL entities — expected exactly 1"
                )
            # Normal ambiguous case: one known org/party, multiple candidate persons
            entry_a = non_indiv[0]
            candidates = individuals

        cur.execute("""
            INSERT INTO monitoring_connection
                (type, name, confirmed, entry_a_id, entry_b_id, parent_id,
                 monetary_value, start_date, end_date, sources)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, CAST(%s AS jsonb))
            RETURNING id
        """, (
            conn.get("type", "UNKNOWN").replace("\x00", ""),
            conn.get("name", "").replace("\x00", ""),
            conn.get("confirmed", False),
            entry_a,
            entry_b,
            parent_id,
            conn.get("monetaryValue"),
            to_date(conn.get("startDate")),
            to_date(conn.get("endDate")),
            json.dumps(sources),
        ))
        conn_id = cur.fetchone()[0]
        conn_count += 1

        for cand_id in candidates:
            cur.execute("""
                INSERT INTO monitoring_connection_candidate (connection_id, entry_id)
                VALUES (%s, %s)
            """, (conn_id, cand_id))
            candidate_count += 1

        if (i + 1) % 50000 == 0:
            print(f"  Progress: {i + 1}/{total} connections")

    print(f"  Connections:        {conn_count}")
    print(f"  Candidates:         {candidate_count}")
    print(f"  Multi-ID (>2):      {multi_id_count}")
    print(f"  Unmapped (skipped): {unmapped_count}")


def validate_duplicates(db):
    """Check for duplicate external IDs in MongoDB before migration."""
    print("Pre-migration validation: checking for duplicate external IDs...")

    duplicates_found = False
    for id_type in EXTERNAL_ID_TYPES.keys():
        pipeline = [
            {"$match": {f"ids.{id_type}": {"$ne": None}}},
            {"$group": {"_id": f"$ids.{id_type}", "count": {"$sum": 1}, "names": {"$push": "$name"}}},
            {"$match": {"count": {"$gt": 1}}},
        ]
        dups = list(db.monitoring_entries.aggregate(pipeline))
        if dups:
            duplicates_found = True
            print(f"  !! Duplicate {id_type} values:")
            for d in dups[:5]:
                print(f"     {d['_id']}: {d['count']} entries ({', '.join(d['names'][:3])})")
            if len(dups) > 5:
                print(f"     ... and {len(dups) - 5} more")

    if not duplicates_found:
        print("  No duplicates found.")

    return duplicates_found


def main():
    mongo_client = MongoClient(MONGO_URI)
    db = mongo_client[MONGO_DB]

    has_duplicates = validate_duplicates(db)
    if has_duplicates:
        print("\nWARNING: Duplicate external IDs found. These will be skipped during migration.")
        print("Continue? (y/n)")
        if input().strip().lower() != "y":
            print("Aborted.")
            sys.exit(1)

    pg = psycopg2.connect(PG_DSN)
    pg.autocommit = False
    cur = pg.cursor()

    # Idempotency check — abort if tables already have data
    cur.execute("SELECT COUNT(*) FROM monitoring_entry")
    existing = cur.fetchone()[0]
    if existing > 0:
        print(f"\nERROR: monitoring_entry already has {existing} rows. Truncate tables first or use a fresh database.")
        sys.exit(1)

    try:
        oid_to_bigint, entry_types = migrate_entries(db, cur)
        pg.commit()

        migrate_connections(db, cur, oid_to_bigint, entry_types)
        pg.commit()

        print("\nMigration complete!")

    except Exception as e:
        pg.rollback()
        print(f"\nMigration FAILED: {e}")
        raise
    finally:
        cur.close()
        pg.close()
        mongo_client.close()


if __name__ == "__main__":
    main()
