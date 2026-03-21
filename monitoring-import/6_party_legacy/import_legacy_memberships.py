import json
import random
import re
import sys
import os
import time
from datetime import datetime, timedelta

import requests
from bs4 import BeautifulSoup
from pymongo import MongoClient

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from common.person_lookup import find_or_create_person

BASE_URL = "https://ariregister.rik.ee/est/political_party/member_history"
BLOCKS_FILE = "ariregister_blocks.txt"
PROGRESS_FILE = "import_progress.json"
SKIPPED_FILE = "skipped_ids.json"

MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "election-vis"

REQUEST_INTERVAL = 60 / 60
REQUEST_JITTER = 0.2


def parse_blocks():
    with open(BLOCKS_FILE, "r") as f:
        content = f.read().strip()

    blocks = content.split("\n\n")
    ranges = []
    for block in blocks:
        lines = [l.strip() for l in block.strip().splitlines() if l.strip()]
        if len(lines) != 2:
            print(f"ERROR: Expected 2 URLs per block, got {len(lines)}: {lines}")
            sys.exit(1)
        start_id = int(lines[0].rstrip("/").split("/")[-1])
        end_id = int(lines[1].rstrip("/").split("/")[-1])
        ranges.append((start_id, end_id))
    return ranges


def generate_ids(ranges):
    for start, end in ranges:
        for id_ in range(start, end + 1):
            yield id_


def load_progress():
    try:
        with open(PROGRESS_FILE, "r") as f:
            return json.load(f).get("last_id")
    except (FileNotFoundError, json.JSONDecodeError):
        return None


def save_progress(last_id):
    with open(PROGRESS_FILE, "w") as f:
        json.dump({"last_id": last_id}, f)


def load_skipped():
    try:
        with open(SKIPPED_FILE, "r") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return []


def save_skipped(skipped):
    with open(SKIPPED_FILE, "w") as f:
        json.dump(skipped, f, indent=2, ensure_ascii=False)


last_request_time = 0


def fetch_page(url):
    global last_request_time
    interval = REQUEST_INTERVAL + random.uniform(-REQUEST_JITTER, REQUEST_JITTER)
    elapsed = time.time() - last_request_time
    if elapsed < interval:
        time.sleep(interval - elapsed)
    try:
        last_request_time = time.time()
        resp = requests.get(url, timeout=30)
        resp.raise_for_status()
        return resp.text, None
    except Exception as e:
        return None, f"HTTP error: {e}"


def parse_page(html):
    soup = BeautifulSoup(html, "html.parser")

    h2_tags = soup.find_all("h2")
    name = None
    birthdate_str = None
    for h2 in h2_tags:
        text = h2.get_text(strip=True)
        match = re.match(r"^(.+?)\s*\((\d{2}\.\d{2}\.\d{4})\)$", text)
        if match:
            name = match.group(1).strip()
            birthdate_str = match.group(2)
            break

    if not name or not birthdate_str:
        return None, None, None

    birthdate = datetime.strptime(birthdate_str, "%d.%m.%Y")

    rows = []
    table = soup.find("table")
    if table:
        for tr in table.find_all("tr")[1:]:
            cells = tr.find_all("td")
            if len(cells) >= 6:
                party_name = cells[1].get_text(strip=True)
                registrikood = cells[2].get_text(strip=True)
                joined_str = cells[4].get_text(strip=True)
                left_str = cells[5].get_text(strip=True)

                joined = datetime.strptime(joined_str, "%d.%m.%Y") if joined_str else None
                left = datetime.strptime(left_str, "%d.%m.%Y") if left_str else None

                rows.append({
                    "party_name": party_name,
                    "registrikood": registrikood,
                    "joined": joined,
                    "left": left,
                })

    return name, birthdate, rows


def skip_id(skipped, current_id, reason):
    url = f"{BASE_URL}/{current_id}"
    print(f"  SKIPPED ID={current_id}: {reason}")
    skipped.append({"id": current_id, "url": url, "reason": reason})
    save_skipped(skipped)


def main():
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    entries = db["monitoring_entries"]
    connections = db["monitoring_entry_connections"]

    print("Loading party cache...")
    party_cache = {}
    for doc in entries.find({"type": "PARTY"}, {"_id": 1, "name": 1, "ids.estGovId": 1}):
        est_gov_id = doc.get("ids", {}).get("estGovId")
        if est_gov_id:
            party_cache[est_gov_id] = doc["_id"]
    print(f"  Cached {len(party_cache)} parties")

    ranges = parse_blocks()
    all_ids = list(generate_ids(ranges))

    last_id = load_progress()
    if last_id:
        print(f"  Resuming after ID {last_id}")
        try:
            resume_idx = all_ids.index(last_id)
            all_ids = all_ids[resume_idx + 1:]
        except ValueError:
            pass

    total = len(all_ids)
    print(f"  IDs remaining: {total}")

    skipped = load_skipped()
    stats = {
        "persons_found": 0,
        "persons_created": 0,
        "persons_updated_bd": 0,
        "anon_id_set": 0,
        "connections_existing": 0,
        "connections_created": 0,
        "no_data": 0,
        "skipped": 0,
    }

    processed = 0
    start_time = time.time()

    for current_id in all_ids:
        processed += 1
        url = f"{BASE_URL}/{current_id}"

        html, fetch_err = fetch_page(url)
        if fetch_err:
            skip_id(skipped, current_id, fetch_err)
            stats["skipped"] += 1
            save_progress(current_id)
            continue

        try:
            name, birthdate, rows = parse_page(html)
        except Exception as e:
            skip_id(skipped, current_id, f"Parse error: {e}")
            stats["skipped"] += 1
            save_progress(current_id)
            continue

        if not name or birthdate is None or not rows:
            skip_id(skipped, current_id, "No data on page")
            stats["no_data"] += 1
            save_progress(current_id)
            continue

        person_ids, created, updated_bd = find_or_create_person(
            entries, name, birthdate=birthdate,
        )

        if len(person_ids) > 1:
            skip_id(skipped, current_id, f"Multiple persons for '{name}' ({birthdate.strftime('%d.%m.%Y')}): {person_ids}")
            stats["skipped"] += 1
            save_progress(current_id)
            continue

        if created:
            stats["persons_created"] += 1
        elif updated_bd:
            stats["persons_updated_bd"] += 1
        else:
            stats["persons_found"] += 1

        person_id = person_ids[0]

        existing = entries.find_one({"_id": person_id}, {"ids": 1})
        if not existing.get("ids", {}).get("ariregisterAnonId"):
            entries.update_one(
                {"_id": person_id},
                {"$set": {"ids.ariregisterAnonId": str(current_id)}},
            )
            stats["anon_id_set"] += 1

        new_count = 0
        party_skip = False
        for row in rows:
            party_id = party_cache.get(row["registrikood"])
            if not party_id:
                skip_id(skipped, current_id, f"Party not in cache: '{row['party_name']}' (registrikood={row['registrikood']})")
                stats["skipped"] += 1
                party_skip = True
                break

            existing_conn = connections.find_one({
                "connectedIds": {"$all": [person_id, party_id]},
                "type": "PARTY_MEMBERSHIP",
                "startDate": row["joined"],
            })

            if existing_conn:
                stats["connections_existing"] += 1
                continue

            connection_doc = {
                "connectedIds": [person_id, party_id],
                "parentId": None,
                "name": "Liige",
                "type": "PARTY_MEMBERSHIP",
                "confirmed": True,
                "monetaryValue": None,
                "startDate": row["joined"],
                "endDate": row["left"],
                "sources": [{"sourceUrl": url}],
            }
            connections.insert_one(connection_doc)
            stats["connections_created"] += 1
            new_count += 1

        if not party_skip:
            bd_str = birthdate.strftime("%d.%m.%Y")
            elapsed = time.time() - start_time
            avg_per_item = elapsed / processed
            remaining = (total - processed) * avg_per_item
            eta = datetime.now() + timedelta(seconds=remaining)
            eta_str = eta.astimezone().strftime("%Y-%m-%d %H:%M %Z")
            print(f"[{processed}/{total} ETA {eta_str}] ID={current_id} {name} ({bd_str}) — {len(rows)} memberships, {new_count} new")

        save_progress(current_id)

    client.close()

    print(f"\n--- Results ---")
    print(f"Pages processed:        {processed}")
    print(f"Persons found:          {stats['persons_found']}")
    print(f"Persons created:        {stats['persons_created']}")
    print(f"Persons updated (bd):   {stats['persons_updated_bd']}")
    print(f"AnonId set:             {stats['anon_id_set']}")
    print(f"Connections existing:   {stats['connections_existing']}")
    print(f"Connections created:    {stats['connections_created']}")
    print(f"No data on page:        {stats['no_data']}")
    print(f"Skipped (errors):       {stats['skipped']}")
    if skipped:
        print(f"  See {SKIPPED_FILE} for details")


if __name__ == "__main__":
    main()
