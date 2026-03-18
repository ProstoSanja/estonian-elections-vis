import csv
import io
import sys
from datetime import datetime

import requests
from pymongo import MongoClient

PARTIES = [
    ("Eesti Keskerakond", "80053370"),
    ("Eesti Rahvuslased ja Konservatiivid", "80643657"),
    ("Eesti Reformierakond", "80043147"),
    ("Eesti Vabaduspartei - Põllumeeste Kogu", "80051862"),
    ("Eesti Vasakliit", "80060022"),
    ("EKRE - Eesti Konservatiivne Rahvaerakond", "80040344"),
    ("Erakond Eesti 200", "80551335"),
    ("Erakond Eestimaa Rohelised", "80223139"),
    ("Erakond Parempoolsed", "80608655"),
    ("ISAMAA Erakond", "80243584"),
    ("KOOS organisatsioon osutab suveräänsusele", "80626222"),
    ("Sotsiaaldemokraatlik Erakond", "80052459"),
    ("Vabaerakond Aru Pähe", "80589722"),
    ("Rahva Ühtsuse Erakond", "80374223"),
]

CSV_URL = "https://ariregister.rik.ee/est/political_party/members/{code}?download=CSV"

MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "election-vis"


def parse_date(date_str: str):
    date_str = date_str.strip()
    if not date_str:
        return None
    return datetime.strptime(date_str, "%d.%m.%Y")


def download_csv(code: str) -> list[list[str]]:
    url = CSV_URL.format(code=code)
    resp = requests.get(url, timeout=60)
    resp.raise_for_status()

    for encoding in ("utf-8", "latin-1"):
        try:
            text = resp.content.decode(encoding)
            break
        except UnicodeDecodeError:
            continue
    else:
        text = resp.content.decode("utf-8", errors="replace")

    reader = csv.reader(io.StringIO(text), delimiter=";")
    next(reader)
    return [row for row in reader if len(row) >= 4]


def main():
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    entries = db["monitoring_entries"]
    connections = db["monitoring_entry_connections"]

    total_members = 0

    for party_name, code in PARTIES:
        print(f"Processing: {party_name} ({code})...")
        source_url = CSV_URL.format(code=code)

        party_doc = {
            "ids": {"estGovId": code, "ariregisterAnonId": None},
            "name": party_name,
            "type": "PARTY",
            "nameParts": {
                "firstName": None,
                "lastName": None,
                "businessName": party_name,
                "businessSuffix": None,
            },
            "birthDate": None,
        }
        party_result = entries.insert_one(party_doc)
        party_id = party_result.inserted_id

        try:
            rows = download_csv(code)
        except Exception as e:
            print(f"  WARN: Failed to download CSV for {party_name}: {e}", file=sys.stderr)
            continue

        member_count = 0
        for row in rows:
            firstname = row[0].strip()
            surname = row[1].strip()
            birthdate = parse_date(row[2])
            start_date = parse_date(row[3])

            if not birthdate or not start_date:
                continue

            person_doc = {
                "ids": {"estGovId": None, "ariregisterAnonId": None},
                "name": f"{firstname} {surname}",
                "type": "INDIVIDUAL",
                "nameParts": {
                    "firstName": firstname,
                    "lastName": surname,
                    "businessName": None,
                    "businessSuffix": None,
                },
                "birthDate": birthdate,
            }
            person_result = entries.insert_one(person_doc)
            person_id = person_result.inserted_id

            connection_doc = {
                "connectedIds": [person_id, party_id],
                "parentId": None,
                "name": "Liige",
                "type": "PARTY_MEMBERSHIP",
                "confirmed": True,
                "monetaryValue": None,
                "startDate": start_date,
                "endDate": None,
                "sources": [{"sourceUrl": source_url}],
            }
            connections.insert_one(connection_doc)
            member_count += 1

        total_members += member_count
        print(f"  Inserted {member_count} members")

    client.close()
    print(f"\nDone. Total members inserted: {total_members}")


if __name__ == "__main__":
    main()
