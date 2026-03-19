import json
import os
import time
import requests

MEMBERS_FILE = "parliament_members.json"
PROGRESS_FILE = "enrich_progress.json"
API_BASE = "https://api.riigikogu.ee/api/plenary-members"
REQUEST_INTERVAL = 60 / 20

last_request_time = 0


def fetch_member(uuid):
    global last_request_time
    url = f"{API_BASE}/{uuid}"
    while True:
        elapsed = time.time() - last_request_time
        if elapsed < REQUEST_INTERVAL:
            time.sleep(REQUEST_INTERVAL - elapsed)
        try:
            last_request_time = time.time()
            resp = requests.get(url, timeout=30)
            if resp.status_code == 404:
                return None
            resp.raise_for_status()
            return resp.json()
        except Exception as e:
            print(f"  ERROR fetching {url}: {e}")
            print("  Retrying in 5 seconds...")
            time.sleep(5)


def extract_contacts(data):
    contacts = []

    if data.get("phone"):
        contacts.append({"type": "phone", "value": data["phone"]})
    if data.get("email"):
        contacts.append({"type": "email", "value": data["email"]})
    if data.get("web"):
        contacts.append({"type": "website", "value": data["web"]})

    for entry in data.get("media", []):
        url = entry.get("url", "")
        if url:
            contacts.append({"type": "social", "value": url})

    return contacts


def extract_photo_url(data):
    try:
        return data["photoBig"]["_links"]["download"]["href"]
    except (KeyError, TypeError):
        return None


def load_progress():
    try:
        with open(PROGRESS_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return {}


def save_progress(enriched):
    with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
        json.dump(enriched, f, ensure_ascii=False)


def main():
    with open(MEMBERS_FILE, "r", encoding="utf-8") as f:
        members = json.load(f)

    enriched = load_progress()

    if enriched:
        print(f"Resuming — {len(enriched)}/{len(members)} already enriched")

    for i, member in enumerate(members):
        uuid = member["uuid"]

        if uuid in enriched:
            continue

        print(f"[{i + 1}/{len(members)}] Fetching {member['fullName']} ({uuid})")
        data = fetch_member(uuid)

        if data is None:
            last_end = max(s["endDate"] for s in member["spans"])
            print(f"  404 — not found (last active: {last_end})")
            enriched[uuid] = {"notFound": True, "lastEndDate": last_end}
            save_progress(enriched)
            continue

        contacts = extract_contacts(data)
        photo = extract_photo_url(data)
        dob = data.get("dateOfBirth")

        enriched[uuid] = {
            "dateOfBirth": dob,
            "contacts": contacts,
            "photo": photo,
        }

        contact_types = [c["type"] for c in contacts]
        print(f"  born={dob}  contacts=[{', '.join(contact_types)}]  photo={'yes' if photo else 'no'}")

        save_progress(enriched)

    not_found = []
    for member in members:
        uuid = member["uuid"]
        extra = enriched.get(uuid, {})
        if extra.get("notFound"):
            not_found.append((member["fullName"], extra["lastEndDate"]))
            continue
        if extra.get("dateOfBirth"):
            member["dateOfBirth"] = extra["dateOfBirth"]
        if extra.get("contacts"):
            member["contacts"] = extra["contacts"]
        if extra.get("photo"):
            member["photo"] = extra["photo"]

    with open(MEMBERS_FILE, "w", encoding="utf-8") as f:
        json.dump(members, f, indent=2, ensure_ascii=False)

    os.remove(PROGRESS_FILE)

    stats_dob = sum(1 for e in enriched.values() if e.get("dateOfBirth"))
    stats_photo = sum(1 for e in enriched.values() if e.get("photo"))
    stats_contacts = sum(1 for e in enriched.values() if e.get("contacts"))
    print(f"\nDone! {len(members)} members enriched")
    print(f"  With dateOfBirth: {stats_dob}")
    print(f"  With photo: {stats_photo}")
    print(f"  With contacts: {stats_contacts}")
    if not_found:
        print(f"  Not found (404): {len(not_found)}")
        for name, last_end in sorted(not_found, key=lambda x: x[1]):
            print(f"    {name} (last active: {last_end})")
    print(f"Updated {MEMBERS_FILE}")


if __name__ == "__main__":
    main()
