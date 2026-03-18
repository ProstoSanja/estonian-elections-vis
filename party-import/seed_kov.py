import csv
import sys

import psycopg

CSV_PATH = "avalik_sektor_kov.csv"

SIMPLE_TYPE_MAP = {
    "äriühingud ja tulundusasutused": "GOV_KOV_OWNED",
    "sihtasutused": "GOV_KOV_PUBLIC",
    "mittetulundusühingud": "GOV_KOV_OWNED",
}

CONDITIONAL_LIIK = {
    "valla või linna ametiasutuste hallatav asutus",
    "valla või linna ametiasutus",
}

DB_CONFIG = dict(
    host="localhost",
    port=5432,
    dbname="election-vis",
    user="postgres_user",
    password="postgres_pass",
)


def resolve_type(liik: str, name: str, parent_name: str) -> str | None:
    if liik in SIMPLE_TYPE_MAP:
        return SIMPLE_TYPE_MAP[liik]
    if liik in CONDITIONAL_LIIK:
        name_lower = name.lower()
        if name == parent_name or "valitsus" in name_lower or "amet" in name_lower:
            return "GOV_KOV"
        return "GOV_KOV_PUBLIC"
    return None


def find_group_by_code(cur, code: str):
    cur.execute("SELECT id, name FROM monitoring_group WHERE external_id_ariregister = %s", (code,))
    return cur.fetchone()


def find_group_by_name(cur, name: str):
    cur.execute("SELECT id, external_id_ariregister FROM monitoring_group WHERE name = %s", (name,))
    return cur.fetchone()


def main():
    conn = psycopg.connect(**DB_CONFIG, autocommit=False)
    inserted = 0
    skipped = 0
    errors = 0
    relationships = 0
    removed = 0

    with open(CSV_PATH, encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader)

        with conn.cursor() as cur:
            for row in reader:
                if len(row) < 5:
                    continue

                parent_name = row[1].strip()
                code = row[2].strip()
                name = row[3].strip()
                liik = row[4].strip()
                group_type = resolve_type(liik, name, parent_name)

                if not group_type:
                    print(f"  ERROR: Unknown Üksuse liik '{liik}' for {name}", file=sys.stderr)
                    errors += 1
                    continue

                existing_by_code = find_group_by_code(cur, code)
                if existing_by_code:
                    group_id = existing_by_code[0]
                    skipped += 1
                    was_inserted = False
                else:
                    existing_by_name = find_group_by_name(cur, name)
                    if existing_by_name:
                        existing_code = existing_by_name[1]
                        if existing_code and existing_code != code:
                            print(f"  ERROR: '{name}' exists with code {existing_code}, but CSV has {code}", file=sys.stderr)
                            errors += 1
                            continue
                        group_id = existing_by_name[0]
                        skipped += 1
                        was_inserted = False
                    else:
                        cur.execute(
                            """INSERT INTO monitoring_group (name, type, external_id_ariregister)
                               VALUES (%s, %s, %s) RETURNING id""",
                            (name, group_type, code),
                        )
                        group_id = cur.fetchone()[0]
                        inserted += 1
                        was_inserted = True

                if parent_name == name:
                    continue

                parent = find_group_by_name(cur, parent_name)
                if not parent:
                    print(f"  ERROR: Parent '{parent_name}' not found for '{name}' (code={code}), removing entry", file=sys.stderr)
                    if was_inserted:
                        cur.execute("DELETE FROM monitoring_group WHERE id = %s", (group_id,))
                        removed += 1
                    errors += 1
                    continue

                parent_id = parent[0]
                cur.execute(
                    """INSERT INTO monitoring_group_relationship (parent_group_id, child_group_id)
                       VALUES (%s, %s) ON CONFLICT DO NOTHING""",
                    (parent_id, group_id),
                )
                relationships += 1

    conn.commit()
    conn.close()

    print(f"\nDone.")
    print(f"  Inserted: {inserted}")
    print(f"  Removed (no parent found): {removed}")
    print(f"  Skipped (already existed): {skipped}")
    print(f"  Relationships created: {relationships}")
    print(f"  Errors: {errors}")


if __name__ == "__main__":
    main()
