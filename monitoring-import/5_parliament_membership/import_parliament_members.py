import json
import sys
import os
from datetime import datetime

from pymongo import MongoClient

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from common.person_lookup import find_or_create_person

MEMBERS_FILE = "parliament_members.json"
SOURCE_URL_TEMPLATE = "https://api.riigikogu.ee/api/plenary-members/{uuid}"

MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "election-vis"
RIIGIKOGU_NAME = "Riigikogu Kantselei"


def parse_date(date_str):
    if not date_str:
        return None
    return datetime.strptime(date_str, "%Y-%m-%d")


def main():
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    entries = db["monitoring_entries"]
    connections = db["monitoring_entry_connections"]

    with open(MEMBERS_FILE, "r", encoding="utf-8") as f:
        members = json.load(f)

    rk = entries.find_one(
        {"name": {"$regex": f"^{RIIGIKOGU_NAME}$", "$options": "i"}, "type": {"$ne": "INDIVIDUAL"}},
        {"_id": 1, "name": 1},
    )
    if not rk:
        print(f"ERROR: Could not find '{RIIGIKOGU_NAME}' in monitoring_entries. Aborting.")
        sys.exit(1)
    rk_id = rk["_id"]
    print(f"Riigikogu entry: {rk['name']} ({rk_id})")

    stats = {
        "found": 0,
        "created": 0,
        "multi_match": 0,
        "updated": 0,
        "connections_created": 0,
    }

    for i, member in enumerate(members):
        uuid = member["uuid"]
        full_name = member["fullName"]
        first_name = member.get("firstName")
        last_name = member.get("lastName")
        dob = parse_date(member.get("dateOfBirth"))
        contacts = member.get("contacts")
        photo = member.get("photo")

        print(f"[{i + 1}/{len(members)}] {full_name}")

        person_ids, created, updated_bd = find_or_create_person(
            entries, full_name, birthdate=dob,
            first_name=first_name, last_name=last_name,
        )

        if created:
            stats["created"] += 1
            print(f"  Created new person")
        elif updated_bd:
            stats["updated"] += 1

        confirmed = len(person_ids) == 1

        if len(person_ids) > 1:
            stats["multi_match"] += 1
            print(f"  WARNING: {len(person_ids)} matches, skipping data updates")
        else:
            stats["found"] += 1
            person_id = person_ids[0]

            existing = entries.find_one({"_id": person_id}, {"ids": 1, "unstructuredData": 1, "birthDate": 1, "nameParts": 1})
            update_fields = {}

            if dob and not existing.get("birthDate"):
                update_fields["birthDate"] = dob

            if not existing.get("ids", {}).get("riigikoguGuid"):
                update_fields["ids.riigikoguGuid"] = uuid

            existing_ud = existing.get("unstructuredData") or {}
            if contacts and not existing_ud.get("contacts"):
                update_fields["unstructuredData.contacts"] = contacts
            if photo and not existing_ud.get("photo"):
                update_fields["unstructuredData.photo"] = photo

            np = existing.get("nameParts", {})
            if first_name and not np.get("firstName"):
                update_fields["nameParts.firstName"] = first_name.strip()
            if last_name and not np.get("lastName"):
                update_fields["nameParts.lastName"] = last_name.strip()

            if update_fields:
                entries.update_one({"_id": person_id}, {"$set": update_fields})
                stats["updated"] += 1

        source_url = SOURCE_URL_TEMPLATE.format(uuid=uuid)
        for span in member.get("spans", []):
            connection_doc = {
                "connectedIds": person_ids + [rk_id],
                "parentId": None,
                "name": "Liige",
                "type": "ADMINISTRATION",
                "confirmed": confirmed,
                "monetaryValue": None,
                "startDate": parse_date(span.get("startDate")),
                "endDate": parse_date(span.get("endDate")),
                "sources": [{"sourceUrl": source_url}],
            }
            connections.insert_one(connection_doc)
            stats["connections_created"] += 1

    print(f"\n--- Results ---")
    print(f"Total members: {len(members)}")
    print(f"  Found (single match): {stats['found']}")
    print(f"  Created:              {stats['created']}")
    print(f"  Multi-match (skipped):{stats['multi_match']}")
    print(f"  Updated:              {stats['updated']}")
    print(f"  Connections created:  {stats['connections_created']}")


if __name__ == "__main__":
    main()
