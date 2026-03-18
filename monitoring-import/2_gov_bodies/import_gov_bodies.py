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
    return NON_KOV_TYPE_MAP.get(liik)


def resolve_kov_type(liik, name, parent_name):
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

    return inserted, connections_created, parentless, errors


def main():
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    entries_col = db["monitoring_entries"]
    connections_col = db["monitoring_entry_connections"]

    print("Importing non-KOV organizations...")
    ins, conn, pless, err = process_file(
        "avalik_sektor_non_kov.csv",
        lambda liik, name, parent_name: resolve_non_kov_type(liik),
        entries_col, connections_col,
    )
    print(f"  Inserted: {ins}, Connections: {conn}, Parentless: {pless}, Errors: {err}\n")

    print("Importing KOV organizations...")
    ins2, conn2, pless2, err2 = process_file(
        "avalik_sektor_kov.csv",
        resolve_kov_type,
        entries_col, connections_col,
    )
    print(f"  Inserted: {ins2}, Connections: {conn2}, Parentless: {pless2}, Errors: {err2}\n")

    print("Done.")
    print(f"  Total entries: {ins + ins2}")
    print(f"  Total connections: {conn + conn2}")
    print(f"  Total parentless (CONNECTION_PENDING): {pless + pless2}")
    print(f"  Total errors: {err + err2}")

    client.close()


if __name__ == "__main__":
    main()
