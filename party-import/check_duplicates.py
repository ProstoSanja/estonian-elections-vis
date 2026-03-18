import csv
from datetime import datetime
from collections import defaultdict

CSV_PATH = "/Users/alex/Downloads/tableConvert.com_p9kagb.csv"

ALLOWED_PAIRS = [
    ("Päästeamet", "Sisekaitseakadeemia"),
]


def parse_period(period_str):
    parts = period_str.strip().split("-")
    if len(parts) != 2:
        return None, None
    try:
        start = datetime.strptime(parts[0], "%d.%m.%Y").date()
        end = datetime.strptime(parts[1], "%d.%m.%Y").date()
        return start, end
    except ValueError:
        return None, None


def periods_overlap(a_start, a_end, b_start, b_end):
    return a_start <= b_end and b_start <= a_end


def parse_koormus(s):
    s = s.strip().replace(",", ".")
    try:
        return float(s)
    except ValueError:
        return 0.0


def is_excluded_entry(entry):
    unit = entry["unit"].lower()
    pos = entry["position"].lower()
    return ("fraktsioon" in unit or "ministeerium" in unit) and "nõunik" in pos


def is_allowed_pair(a, b):
    orgs = {a["org"], b["org"]}
    for org1, org2 in ALLOWED_PAIRS:
        if orgs == {org1, org2}:
            units = [a["unit"], b["unit"]]
            if any("õppetool" in u.lower() for u in units):
                return True
    return False


def main():
    entries = defaultdict(list)

    with open(CSV_PATH, encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader)
        next(reader)
        for row in reader:
            if len(row) >= 11:
                first = row[3].strip()
                last = row[4].strip()
                start, end = parse_period(row[10].strip())
                if start and end:
                    entries[(first, last)].append({
                        "start": start,
                        "end": end,
                        "org": row[0].strip(),
                        "unit": row[1].strip(),
                        "position": row[2].strip(),
                        "koormus": parse_koormus(row[5]),
                    })

    dupes = []
    for (first, last), records in entries.items():
        if len(records) < 2:
            continue

        eligible = [r for r in records if not is_excluded_entry(r)]
        if len(eligible) < 2:
            continue

        has_real_dupe = False
        for i in range(len(eligible)):
            for j in range(i + 1, len(eligible)):
                a, b = eligible[i], eligible[j]
                if not periods_overlap(a["start"], a["end"], b["start"], b["end"]):
                    continue
                if (a["unit"] == b["unit"] and a["position"] == b["position"]
                        and a["koormus"] + b["koormus"] <= 1.85):
                    continue
                if is_allowed_pair(a, b):
                    continue
                has_real_dupe = True
                break
            if has_real_dupe:
                break

        if has_real_dupe:
            dupes.append((first, last, len(records), records))

    dupes.sort(key=lambda x: -x[2])

    print(f"Found {len(dupes)} duplicate name pairs:\n")
    for first, last, cnt, records in dupes:
        print(f"  {cnt}x  {first} {last}")
        for r in records:
            print(f"       {r['start']} - {r['end']}  |  {r['org']:45}  |  {r['unit']:25}  |  {r['position']:40}  |  koormus={r['koormus']}")
        print()


if __name__ == "__main__":
    main()
