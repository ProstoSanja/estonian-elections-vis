import re
import sys
import os
from datetime import datetime

import requests
from pymongo import MongoClient

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from common.person_lookup import find_or_create_person, find_or_create_org

API_BASE = "https://erjk.ee/api/quarterly-reports"
SOURCE_URL = "https://erjk.ee/avaandmed/#/"

MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "election-vis"

SKIP_PARTIES = {
    "Eesti Iseseisvuspartei",
    "Eesti Vabaerakond",
    "Elurikkuse Erakond",
    "Erakond Rahva Tahe",
}

CATEGORY_TYPE_MAP = {
    "Liikmemaks": "PARTY_MEMBERSHIP",
    "Rahaline annetus": "DONATION",
    "Mitterahaline annetus": "DONATION",
    "Isiklikud vahendid": "DONATION",
    "Riigitoetus": "DONATION",
    "Tulu erakonna varalt": "DONATION",
    "Pangalaen": "BANK_LOAN",
}

def is_org_birthdate(birthdate_str):
    return birthdate_str and "." not in birthdate_str and birthdate_str.strip().isdigit()


def parse_date(date_str):
    date_str = date_str.strip()
    if not date_str:
        return None
    try:
        return datetime.strptime(date_str, "%d.%m.%Y")
    except ValueError:
        return None


def fetch_json(url):
    resp = requests.get(url, timeout=30)
    resp.raise_for_status()
    return resp.json()


def main():
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    entries_col = db["monitoring_entries"]
    connections_col = db["monitoring_entry_connections"]

    # --- Phase 1: Validate parties ---
    print("Phase 1: Validating parties...")
    erjk_parties = fetch_json(f"{API_BASE}/parties")

    party_map = {}  # erjk_party_id -> mongo ObjectId
    missing = []

    for p in erjk_parties:
        name = p["party_name"]
        erjk_id = str(p["party_id"])

        if name in SKIP_PARTIES:
            print(f"  SKIP: {name}")
            continue

        doc = entries_col.find_one(
            {"type": "PARTY", "name": {"$regex": f"^{re.escape(name)}$", "$options": "i"}},
            {"_id": 1, "ids": 1},
        )
        if not doc:
            missing.append(name)
            continue

        if not doc.get("ids", {}).get("erjkId"):
            entries_col.update_one(
                {"_id": doc["_id"]},
                {"$set": {"ids.erjkId": erjk_id}},
            )
            print(f"  Updated erjkId: {name} -> {erjk_id}")

        party_map[p["party_id"]] = doc["_id"]
        print(f"  OK: {name} (erjk_id={erjk_id})")

    if missing:
        print(f"\n  FATAL: {len(missing)} parties not found in DB:")
        for m in missing:
            print(f"    - {m}")
        print("\nAborting.")
        client.close()
        sys.exit(1)

    print(f"\n  Matched {len(party_map)} parties.\n")

    # --- Phase 2 & 3: Fetch quarters and process receipts ---
    stats = {
        "persons_found_1": 0,
        "persons_birthdate_updated": 0,
        "persons_created": 0,
        "persons_multi": 0,
        "orgs_found_by_id": 0,
        "orgs_found_by_name": 0,
        "orgs_skipped_multi": 0,
        "orgs_created": [],
        "connections_created": 0,
        "confirmed_true": 0,
        "confirmed_false": 0,
        "by_category": {},
        "by_type": {},
    }

    for erjk_party_id, party_mongo_id in party_map.items():
        party_doc = entries_col.find_one({"_id": party_mongo_id}, {"name": 1})
        party_name = party_doc["name"]
        print(f"Processing: {party_name} (erjk_id={erjk_party_id})...")

        quarters = fetch_json(f"{API_BASE}/quarters/{erjk_party_id}")
        print(f"  {len(quarters)} quarters found")

        for q in quarters:
            report_id = q["report_id"]
            report_date = q["report_date"]
            report_url = f"{API_BASE}/{report_id}?report_type=receipts"

            try:
                receipts = fetch_json(report_url)
            except Exception as e:
                print(f"  WARN: Failed to fetch report {report_id} ({report_date}): {e}")
                continue

            print(f"    {report_date} (id={report_id}): {len(receipts)} receipts")

            if not receipts:
                continue

            for row in receipts:
                category = row.get("receipt_category", "")
                name = row.get("name", "").strip()
                birthdate_raw = row.get("birthdate", "").strip()
                amount = row.get("amount")
                date_str = row.get("date", "").strip()

                if not name:
                    continue

                conn_type = CATEGORY_TYPE_MAP.get(category, "DONATION")
                start_date = parse_date(date_str)

                stats["by_category"][category] = stats["by_category"].get(category, 0) + 1
                stats["by_type"][conn_type] = stats["by_type"].get(conn_type, 0) + 1

                # Determine donor
                if is_org_birthdate(birthdate_raw):
                    est_gov_id = birthdate_raw
                    try:
                        org_found_by = _find_org_tracking(entries_col, name, est_gov_id)

                        if org_found_by == "created":
                            stats["orgs_created"].append(f"{name} ({est_gov_id})")
                        elif org_found_by == "by_id":
                            stats["orgs_found_by_id"] += 1
                        elif org_found_by == "by_name":
                            stats["orgs_found_by_name"] += 1

                        donor_id = find_or_create_org(entries_col, name, est_gov_id)
                        donor_ids = [donor_id]
                        confirmed = True
                    except RuntimeError as e:
                        print(f"    ERROR: {e}")
                        stats["orgs_skipped_multi"] += 1
                        continue
                else:
                    birthdate = parse_date(birthdate_raw)
                    person_ids, created, updated_bd = find_or_create_person(
                        entries_col, name, birthdate
                    )
                    donor_ids = person_ids

                    if created:
                        stats["persons_created"] += 1
                    elif updated_bd:
                        stats["persons_birthdate_updated"] += 1
                        stats["persons_found_1"] += 1
                    elif len(person_ids) == 1:
                        stats["persons_found_1"] += 1
                    else:
                        stats["persons_multi"] += 1

                    confirmed = len(person_ids) == 1

                connection_doc = {
                    "connectedIds": donor_ids + [party_mongo_id],
                    "parentId": None,
                    "name": category,
                    "type": conn_type,
                    "confirmed": confirmed,
                    "monetaryValue": amount,
                    "startDate": start_date,
                    "endDate": None,
                    "sources": [
                        {"sourceUrl": SOURCE_URL},
                        {"sourceUrl": report_url},
                    ],
                }
                connections_col.insert_one(connection_doc)
                stats["connections_created"] += 1
                if confirmed:
                    stats["confirmed_true"] += 1
                else:
                    stats["confirmed_false"] += 1

        print(f"  Done: {party_name}\n")

    client.close()

    # --- Summary ---
    print("=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"\nPerson stats:")
    print(f"  Found (exactly 1):       {stats['persons_found_1']}")
    print(f"    Updated with birthdate:{stats['persons_birthdate_updated']}")
    print(f"  Created:                 {stats['persons_created']}")
    print(f"  Multiple found:          {stats['persons_multi']}")
    print(f"\nOrg stats:")
    print(f"  Found by estGovId:       {stats['orgs_found_by_id']}")
    print(f"  Found by name:           {stats['orgs_found_by_name']}")
    print(f"  Skipped (multiple):      {stats['orgs_skipped_multi']}")
    print(f"  Created:                 {len(stats['orgs_created'])}")
    for org in stats["orgs_created"]:
        print(f"    - {org}")
    print(f"\nConnection stats:")
    print(f"  Total created:           {stats['connections_created']}")
    print(f"    confirmed=true:        {stats['confirmed_true']}")
    print(f"    confirmed=false:       {stats['confirmed_false']}")
    print(f"\n  By category:")
    for cat, cnt in sorted(stats["by_category"].items()):
        print(f"    {cat:30s} {cnt}")
    print(f"\n  By type:")
    for t, cnt in sorted(stats["by_type"].items()):
        print(f"    {t:30s} {cnt}")


def _find_org_tracking(entries_col, name, est_gov_id):
    """Check how an org would be found, for stats tracking only."""
    if est_gov_id:
        found = list(entries_col.find({"ids.estGovId": est_gov_id}, {"_id": 1}))
        if len(found) == 1:
            return "by_id"
        if len(found) > 1:
            return "multi"

    found = list(entries_col.find(
        {"name": {"$regex": f"^{re.escape(name.strip())}$", "$options": "i"}},
        {"_id": 1},
    ))
    if len(found) == 1:
        return "by_name"
    if len(found) > 1:
        return "multi"

    return "created"


if __name__ == "__main__":
    main()
