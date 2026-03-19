import csv
import sys

from pymongo import MongoClient

SOURCE_URL = "https://www.fin.ee/sites/default/files/documents/2025-10/Avaliku_sektori_asutused_asutuse_liikide_loikes_juuli_2025.xlsx"

MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "election-vis"

BUSINESS_SUFFIXES = [
    "aktsiaselts", "osaühing", "mittetulundusühing", "sihtasutus",
    "as", "oü", "mtü", "sa",
]
BUSINESS_SUFFIXES_BY_LEN = sorted(BUSINESS_SUFFIXES, key=len, reverse=True)

DIRECT_TYPES = {"GOV", "GOV_KOV", "PUBLIC_BODY", "BUSINESS"}

NON_KOV_TYPE_MAP = {
    "põhiseaduslikud institutsioonid ja Riigikantselei": "GOV",
    "riigi ametiasutused": "GOV",
    "mittetulundusühingud": "BUSINESS",
    "äriühingud ja tulundusasutused": "BUSINESS",
    "avalik-õiguslikud asutused": "PUBLIC_BODY",
    "sihtasutused": "PUBLIC_BODY",
    "hallatavad riigiasutused": "PUBLIC_BODY",
}

KOV_SIMPLE_TYPE_MAP = {
    "äriühingud ja tulundusasutused": "BUSINESS",
    "sihtasutused": "PUBLIC_BODY",
    "mittetulundusühingud": "BUSINESS",
}

KOV_CONDITIONAL_LIIK = {
    "valla või linna ametiasutuste hallatav asutus",
    "valla või linna ametiasutus",
}

PARENTLESS_CATEGORIES = {
    "mitme KOVi poolt asutatud",
    "riigi ja KOVi poolt asutatud",
}


def extract_suffix(name):
    name_lower = name.lower().strip()
    for suffix in BUSINESS_SUFFIXES_BY_LEN:
        if name_lower.startswith(suffix + " "):
            biz_name = name[len(suffix):].strip().strip('"')
            return biz_name, suffix
        if name_lower.endswith(" " + suffix):
            biz_name = name[:-(len(suffix))].strip().strip('"')
            return biz_name, suffix
    return name, None


def resolve_non_kov_type(liik):
    if liik in DIRECT_TYPES:
        return liik
    return NON_KOV_TYPE_MAP.get(liik)


def resolve_kov_type(liik, name, parent_name):
    if liik in DIRECT_TYPES:
        return liik
    if liik in KOV_SIMPLE_TYPE_MAP:
        return KOV_SIMPLE_TYPE_MAP[liik]
    if liik in KOV_CONDITIONAL_LIIK:
        name_lower = name.lower()
        if name == parent_name or "valitsus" in name_lower or "amet" in name_lower:
            return "GOV_KOV"
        return "PUBLIC_BODY"
    return None


def process_file(csv_path, resolve_type_fn, entries_col, connections_col):
    name_to_id = {}  # lowercase name -> ObjectId
    inserted = 0
    skipped = 0
    connections_created = 0
    parentless = 0
    errors = 0

    with open(csv_path, encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader)

        for row in reader:
            if len(row) < 5:
                continue

            parent_name = row[1].strip()
            code = row[2].strip()
            name = row[3].strip()
            liik = row[4].strip()

            entry_type = resolve_type_fn(liik, name, parent_name)

            if not entry_type:
                print(f"  ERROR: Unknown Üksuse liik '{liik}' for {name}", file=sys.stderr)
                errors += 1
                continue

            existing = entries_col.find_one(
                {"name": {"$regex": f"^{name}$", "$options": "i"}},
                {"_id": 1},
            )
            if existing:
                name_to_id[name.lower()] = existing["_id"]
                skipped += 1
                continue
            else:
                biz_name, biz_suffix = extract_suffix(name)
                entry_doc = {
                    "ids": {"estGovId": code, "ariregisterAnonId": None},
                    "name": name,
                    "type": entry_type,
                    "nameParts": {
                        "firstName": None,
                        "lastName": None,
                        "businessName": biz_name,
                        "businessSuffix": biz_suffix,
                    },
                    "birthDate": None,
                }
                result = entries_col.insert_one(entry_doc)
                entry_id = result.inserted_id
                name_to_id[name.lower()] = entry_id
                inserted += 1

            if parent_name.lower() == name.lower():
                continue

            if parent_name in PARENTLESS_CATEGORIES:
                connection_doc = {
                    "connectedIds": [entry_id],
                    "parentId": None,
                    "name": parent_name,
                    "type": "BUSINESS_OWNERSHIP",
                    "confirmed": False,
                    "monetaryValue": None,
                    "startDate": None,
                    "endDate": None,
                    "sources": [{"sourceUrl": SOURCE_URL}],
                }
                connections_col.insert_one(connection_doc)
                parentless += 1
                continue

            parent_id = name_to_id.get(parent_name.lower())
            if not parent_id:
                parent_doc = entries_col.find_one(
                    {"name": {"$regex": f"^{parent_name}$", "$options": "i"}},
                    {"_id": 1},
                )
                if parent_doc:
                    parent_id = parent_doc["_id"]
                    name_to_id[parent_name.lower()] = parent_id
                else:
                    print(f"  ERROR: Parent '{parent_name}' not found for '{name}'", file=sys.stderr)
                    errors += 1
                    continue

            connection_doc = {
                "connectedIds": [parent_id, entry_id],
                "parentId": parent_id,
                "name": "Kõrgemal seisev asutus",
                "type": "BUSINESS_OWNERSHIP",
                "confirmed": True,
                "monetaryValue": None,
                "startDate": None,
                "endDate": None,
                "sources": [{"sourceUrl": SOURCE_URL}],
            }
            connections_col.insert_one(connection_doc)
            connections_created += 1

    return inserted, skipped, connections_created, parentless, errors


def main():
    csv_files = sys.argv[1:] if len(sys.argv) > 1 else ["avalik_sektor_non_kov.csv", "avalik_sektor_kov.csv"]

    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    entries_col = db["monitoring_entries"]
    connections_col = db["monitoring_entry_connections"]

    total_ins = total_conn = total_pless = total_err = 0
    for csv_path in csv_files:
        print(f"Importing {csv_path}...")
        resolve_fn = resolve_kov_type if "kov" in csv_path.lower() else lambda liik, name, parent_name: resolve_non_kov_type(liik)
        ins, skip, conn, pless, err = process_file(csv_path, resolve_fn, entries_col, connections_col)
        print(f"  Inserted: {ins}, Skipped: {skip}, Connections: {conn}, Parentless: {pless}, Errors: {err}\n")
        total_ins += ins
        total_conn += conn
        total_pless += pless
        total_err += err

    print("Done.")
    print(f"  Total entries: {total_ins}")
    print(f"  Total connections: {total_conn}")
    print(f"  Total parentless (CONNECTION_PENDING): {total_pless}")
    print(f"  Total errors: {total_err}")

    client.close()


if __name__ == "__main__":
    main()
