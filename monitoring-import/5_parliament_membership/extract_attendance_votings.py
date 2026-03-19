import json
from datetime import datetime, timedelta

INPUT_FILE = "votings.json"
OUTPUT_FILE = "attendance_votings.json"


def next_monday(dt):
    days_ahead = 7 - dt.weekday()  # Monday is 0
    return (dt + timedelta(days=days_ahead)).replace(hour=0, minute=0, second=0, microsecond=0)


def main():
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        sessions = json.load(f)

    results = []
    skip_until = None

    for session in sessions:
        if not session.get("sittingDateTime"):
            continue

        for voting in session.get("votings", []):
            if voting.get("type", {}).get("code") == "KOHALOLEKU_KONTROLL":
                voting_dt = datetime.fromisoformat(voting["startDateTime"])
                voting_date = voting_dt.replace(hour=0, minute=0, second=0, microsecond=0)

                if skip_until and voting_date < skip_until:
                    break

                link = voting.get("_links", {}).get("self", {}).get("href", "")
                results.append({
                    "date": voting_dt.strftime("%Y-%m-%d"),
                    "datetime": voting["startDateTime"],
                    "votingUrl": link,
                    "uuid": voting["uuid"],
                    "sessionTitle": session["title"],
                    "present": voting.get("present"),
                    "absent": voting.get("absent"),
                })
                skip_until = next_monday(voting_date)
                break

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    print(f"Extracted {len(results)} attendance votings")
    print(f"Date range: {results[0]['date']} — {results[-1]['date']}")

    weekday_counts = {}
    for r in results:
        day = datetime.fromisoformat(r["datetime"]).strftime("%A")
        weekday_counts[day] = weekday_counts.get(day, 0) + 1
    print("By weekday:", weekday_counts)


if __name__ == "__main__":
    main()
