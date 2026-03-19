import json
import time
import requests
from datetime import datetime

INPUT_FILE = "attendance_votings.json"
PROGRESS_FILE = "parliament_members_progress.json"
OUTPUT_FILE = "parliament_members.json"


REQUEST_INTERVAL = 60 / 15  # 4 seconds between requests
last_request_time = 0


def fetch_voting(url):
    global last_request_time
    while True:
        elapsed = time.time() - last_request_time
        if elapsed < REQUEST_INTERVAL:
            time.sleep(REQUEST_INTERVAL - elapsed)
        try:
            last_request_time = time.time()
            resp = requests.get(url, timeout=30)
            resp.raise_for_status()
            return resp.json()
        except Exception as e:
            print(f"  ERROR fetching {url}: {e}")
            print("  Retrying in 5 seconds...")
            time.sleep(5)


def load_progress():
    try:
        with open(PROGRESS_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data["processed_count"], data["members"], set(data["prev_member_uuids"])
    except (FileNotFoundError, KeyError, json.JSONDecodeError):
        return 0, {}, set()


def save_progress(processed_count, members, prev_member_uuids):
    with open(PROGRESS_FILE, "w", encoding="utf-8") as f:
        json.dump({
            "processed_count": processed_count,
            "members": members,
            "prev_member_uuids": list(prev_member_uuids),
        }, f, ensure_ascii=False)


def main():
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        votings = json.load(f)

    processed_count, members, prev_member_uuids = load_progress()

    if processed_count > 0:
        print(f"Resuming from voting {processed_count + 1}/{len(votings)} "
              f"({len(members)} members tracked so far)")

    for i, voting_entry in enumerate(votings):
        if i < processed_count:
            continue

        url = voting_entry["votingUrl"]
        date = voting_entry["date"]
        print(f"[{i + 1}/{len(votings)}] Fetching {date} — {url}")

        data = fetch_voting(url)
        voters = data.get("voters", [])
        print(f"  Got {len(voters)} voters")

        current_uuids = set()

        for voter in voters:
            uuid = voter["uuid"]
            current_uuids.add(uuid)

            if uuid not in members:
                members[uuid] = {
                    "uuid": uuid,
                    "fullName": voter["fullName"],
                    "firstName": voter.get("firstName", ""),
                    "lastName": voter.get("lastName", ""),
                    "spans": [{"startDate": date, "endDate": date}],
                }
            elif uuid in prev_member_uuids:
                members[uuid]["spans"][-1]["endDate"] = date
                members[uuid]["fullName"] = voter["fullName"]
                members[uuid]["firstName"] = voter.get("firstName", "")
                members[uuid]["lastName"] = voter.get("lastName", "")
            else:
                members[uuid]["spans"].append({"startDate": date, "endDate": date})
                members[uuid]["fullName"] = voter["fullName"]
                members[uuid]["firstName"] = voter.get("firstName", "")
                members[uuid]["lastName"] = voter.get("lastName", "")

        prev_member_uuids = current_uuids
        save_progress(i + 1, members, prev_member_uuids)

    result = sorted(members.values(), key=lambda m: m["fullName"])
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    total_spans = sum(len(m["spans"]) for m in result)
    print(f"\nDone! {len(result)} unique members, {total_spans} total spans")
    print(f"Written to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
