import csv
import re
import sys
from datetime import datetime
from collections import defaultdict

from pymongo import MongoClient

CSV_PATH = sys.argv[1] if len(sys.argv) > 1 else "salary_merged.csv"
SOURCE_URLS = {
    2022: "https://www.fin.ee/sites/default/files/documents/2024-06/Ametnike%20p%C3%B5hipalgad%20seisuga%2001.04.2023%20ja%20kogupalgad%202022.xlsx",
    2023: "https://www.fin.ee/sites/default/files/documents/2025-04/Ametnike%20p%C3%B5hipalgad%20seisuga%2001.04.2024%20ja%20kogupalgad%202023.xlsx",
    2024: "https://www.fin.ee/sites/default/files/documents/2025-05/Ametnike%20p%C3%B5hipalgad%20seisuga%2001.04.2025%20ja%20kogupalgad%202024_0.xlsx",
}

MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "election-vis"

ALLOWED_PAIRS = [
    ("Päästeamet", "Sisekaitseakadeemia"),
]

ORG_ALIASES = {
    "maa-amet": "maa- ja ruumiamet",
    "justiitsministeerium": "justiits- ja digiministeerium",
    "tartu linnavolikogu": "tartu linnavalitsus",
    "strateegiakeskus": "tallinna strateegiakeskus",
    "häädemeeste vald": "häädemeeste vallavalitsus",
    "kesklinna valitsus": "tallinna kesklinna valitsus",
    "taebla osavalla valitsus": "lääne-nigula vallavalitsus",
    "kullamaa osavalla valitsus": "lääne-nigula vallavalitsus",
    "martna osavalla valitsus": "lääne-nigula vallavalitsus",
    "noarootsi osavalla valitsus": "lääne-nigula vallavalitsus",
    "oru osavalla valitsus": "lääne-nigula vallavalitsus",
    "palivere osavalla valitsus": "lääne-nigula vallavalitsus",
    "risti osavalla valitsus": "lääne-nigula vallavalitsus",
    "taebla osavallavalitsus": "lääne-nigula vallavalitsus",
    "kullamaa osavallavalitsus": "lääne-nigula vallavalitsus",
    "martna osavallavalitsus": "lääne-nigula vallavalitsus",
    "noarootsi osavallavalitsus": "lääne-nigula vallavalitsus",
    "oru osavallavalitsus": "lääne-nigula vallavalitsus",
    "palivere osavallavalitsus": "lääne-nigula vallavalitsus",
    "risti osavallavalitsus": "lääne-nigula vallavalitsus",
    "jõhvi vallavolikogu": "jõhvi vallavalitsus",
    "toila vallavolikogu": "toila vallavalitsus",
    "vinni vallavaitsus": "vinni vallavalitsus",
    "vigala osavallavalitsus": "märjamaa vallavalitsus",
    "nõva osavallavalitsus": "lääne-nigula vallavalitsus",
    "linnavolikogu kantselei": "tallinna linnavolikogu kantselei",
    "emmaste osavalla valitsus": "hiiumaa vallavalitsus",
    "käina osavalla valitsus": "hiiumaa vallavalitsus",
    "kärdla osavalla valitsus": "hiiumaa vallavalitsus",
    "kärdla ja pühalepa osavalla valitsus": "hiiumaa vallavalitsus",
    "kõrgessaare osavalla valitsus": "hiiumaa vallavalitsus",
    "pühalepa osavalla valitsus": "hiiumaa vallavalitsus",
    "kastre vallavlitsus": "kastre vallavalitsus",
    "keskkonnaministeerium": "kliimaministeerium",
    "maaeluministeerium": "regionaal- ja põllumajandusministeerium",
    "tallinna linnaplaneerimisamet": "tallinna linnaplaneerimise amet",
    "tallinna sotsiaal-jatervishoiuamet": "tallinna sotsiaal- ja tervishoiuamet",
}

KEEP_ORIGINAL_NAME_ALIASES = {
    "vigala osavallavalitsus",
    "nõva osavallavalitsus",
}


DEFAULT_START = None
DEFAULT_END = None


def parse_period(period_str):
    s = period_str.strip()
    if not s or s == "-":
        return DEFAULT_START, DEFAULT_END

    parts = re.findall(r"\d+", s)
    if len(parts) == 0:
        return DEFAULT_START, DEFAULT_END
    if len(parts) % 3 != 0 or len(parts) < 6:
        return None, None

    try:
        d1, m1, y1 = parts[0], parts[1], parts[2]
        d2, m2, y2 = parts[-3], parts[-2], parts[-1]
        start = datetime(int(y1), int(m1), int(d1))
        end = datetime(int(y2), int(m2), int(d2))
        return start, end
    except ValueError:
        return None, None


def parse_koormus(s):
    s = s.strip().replace(",", ".")
    try:
        return float(s)
    except ValueError:
        return 0.0


def parse_salary(s):
    s = s.strip().replace(",", "")
    try:
        return float(s)
    except ValueError:
        return None


def periods_overlap(a_start, a_end, b_start, b_end):
    return a_start <= b_end and b_start <= a_end


def is_excluded_entry(entry):
    unit = entry["unit"].lower()
    pos = entry["position"].lower()
    return ("fraktsioon" in unit or "ministeerium" in unit) and "nõunik" in pos


def is_allowed_pair(a, b):
    orgs = {a["org"], b["org"]}
    for org1, org2 in ALLOWED_PAIRS:
        if orgs == {org1, org2}:
            if any("õppetool" in e["unit"].lower() for e in [a, b]):
                return True
    return False


# --- Phase 1: Detect overworked duplicates ---

def detect_duplicates(rows):
    entries = defaultdict(list)
    for row in rows:
        first = row[3].strip()
        last = row[4].strip()
        start, end = parse_period(row[10].strip())
        if start and end:
            entries[(first.lower(), last.lower())].append({
                "start": start,
                "end": end,
                "org": row[0].strip(),
                "unit": row[1].strip(),
                "position": row[2].strip(),
                "koormus": parse_koormus(row[5]),
            })

    dupe_names = set()
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
            dupe_names.add((first, last))

    return dupe_names


# --- Phase 2: Validate all orgs exist ---

def validate_orgs(rows, org_cache):
    missing = set()
    multi = set()
    for row in rows:
        org_name = row[0].strip()
        org_key = ORG_ALIASES.get(org_name.lower().strip(), org_name.lower().strip())
        matches = org_cache.get(org_key, [])
        if len(matches) == 0:
            missing.add(org_name)
        elif len(matches) > 1:
            multi.add(org_name)
    return missing, multi


def load_org_cache(entries_col):
    cache = defaultdict(list)
    for doc in entries_col.find(
        {"type": {"$in": ["GOV", "GOV_KOV", "PUBLIC_BODY", "BUSINESS"]}},
        {"_id": 1, "name": 1},
    ):
        cache[doc["name"].lower().strip()].append(doc["_id"])
    return cache


def main():
    print("Loading CSV...")
    with open(CSV_PATH, encoding="utf-8") as f:
        reader = csv.reader(f)
        header = next(reader)
        all_rows = [[cell.strip() for cell in row] for row in reader if len(row) >= 11]
    print(f"  {len(all_rows)} rows loaded\n")

    print("Pre-check: Validating all periods...")
    global DEFAULT_START, DEFAULT_END
    bad_periods = []
    for i, row in enumerate(all_rows):
        start, end = parse_period(row[10])
        if start is None:
            bad_periods.append((i + 2, row[10]))
            continue
        if DEFAULT_START is None:
            data_year = start.year
            DEFAULT_START = datetime(data_year, 1, 1)
            DEFAULT_END = datetime(data_year, 12, 31)
            source_url = SOURCE_URLS.get(data_year)
            if not source_url:
                print(f"FATAL: No source URL configured for year {data_year}.")
                sys.exit(1)
            print(f"  Data year: {data_year}")
    if bad_periods:
        print(f"\n  FATAL: {len(bad_periods)} rows with unparseable periods:")
        for line_no, val in bad_periods:
            print(f"    - line {line_no}: '{val}'")
        print("\nAborting. Fix these periods in the CSV first.")
        sys.exit(1)
    print(f"  All periods valid.\n")

    print("Phase 1: Detecting overworked duplicates...")
    dupe_names = detect_duplicates(all_rows)
    print(f"  Found {len(dupe_names)} overworked name pairs\n")

    print("Connecting to MongoDB...")
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    entries_col = db["monitoring_entries"]
    connections_col = db["monitoring_entry_connections"]

    print("Phase 2: Validating all organizations exist...")
    org_cache = load_org_cache(entries_col)
    missing_orgs, multi_orgs = validate_orgs(all_rows, org_cache)

    if missing_orgs:
        print(f"\n  FATAL: {len(missing_orgs)} organizations not found:")
        for org in sorted(missing_orgs):
            print(f"    - {org}")
        print("\nAborting. Fix org aliases or import missing orgs first.")
        client.close()
        sys.exit(1)

    if multi_orgs:
        print(f"\n  FATAL: {len(multi_orgs)} organizations matched multiple entries:")
        for org in sorted(multi_orgs):
            print(f"    - {org}")
        print("\nAborting. Fix duplicate org entries first.")
        client.close()
        sys.exit(1)

    print("  All organizations validated.\n")

    print("Phase 3: Importing salary data...")
    person_cache = {}
    persons_created = 0
    persons_reused = 0
    persons_multi = 0
    connections_created = 0
    unconfirmed_count = 0
    defaulted_period = 0
    bad_period = 0

    total = len(all_rows)
    for idx, row in enumerate(all_rows):
        if idx % 500 == 0:
            print(f"  {idx}/{total}... ({row[3]} {row[4]} @ {row[0]})")
        org_name = row[0].strip()
        unit = row[1].strip()
        position = row[2].strip()
        first = row[3].strip()
        last = row[4].strip()
        salary = parse_salary(row[9])
        start_date, end_date = parse_period(row[10].strip())

        if not first or not last:
            continue

        if start_date is None:
            bad_period += 1
            start_date, end_date = DEFAULT_START, DEFAULT_END

        raw_parts = re.findall(r"\d+", row[10])
        if len(raw_parts) == 0 and row[10].strip() not in ("", "-"):
            defaulted_period += 1

        key = (first.lower(), last.lower())
        is_overworked = key in dupe_names

        # Resolve org (guaranteed to exist from Phase 2)
        org_key = ORG_ALIASES.get(org_name.lower().strip(), org_name.lower().strip())
        org_id = org_cache[org_key][0]

        # Lookup or create person
        if key in person_cache:
            person_ids = person_cache[key]
        else:
            found = list(entries_col.find(
                {"type": "INDIVIDUAL", "nameParts.firstName": {"$regex": f"^{first}$", "$options": "i"},
                 "nameParts.lastName": {"$regex": f"^{last}$", "$options": "i"}},
                {"_id": 1},
            ))
            if len(found) == 0:
                person_doc = {
                    "ids": {"estGovId": None, "ariregisterAnonId": None},
                    "name": f"{first} {last}",
                    "type": "INDIVIDUAL",
                    "nameParts": {
                        "firstName": first,
                        "lastName": last,
                        "businessName": None,
                        "businessSuffix": None,
                    },
                    "birthDate": None,
                }
                result = entries_col.insert_one(person_doc)
                person_ids = [result.inserted_id]
                person_cache[key] = person_ids
                persons_created += 1
            else:
                person_ids = [d["_id"] for d in found]
                person_cache[key] = person_ids
                if len(found) == 1:
                    persons_reused += 1
                else:
                    persons_multi += 1

        confirmed = not is_overworked and len(person_ids) == 1

        if unit and unit != "-" and unit.lower() != org_name.lower():
            pos_str = f"{unit} - {position}"
        else:
            pos_str = position
        if org_name.lower().strip() in KEEP_ORIGINAL_NAME_ALIASES:
            pos_str = f"{org_name} - {pos_str}"

        connection_doc = {
            "connectedIds": [org_id] + person_ids,
            "parentId": None,
            "name": pos_str,
            "type": "EMPLOYMENT",
            "confirmed": confirmed,
            "monetaryValue": salary,
            "startDate": start_date,
            "endDate": end_date,
            "sources": [{"sourceUrl": source_url}],
        }
        connections_col.insert_one(connection_doc)
        connections_created += 1
        if not confirmed:
            unconfirmed_count += 1

    client.close()

    confirmed_count = connections_created - unconfirmed_count
    print("\nDone.\n")
    print(f"  Total rows:              {len(all_rows)}")
    print(f"  Defaulted period:        {defaulted_period}")
    print(f"  Bad period (!=6 parts):  {bad_period}")
    print(f"  Persons created:         {persons_created}")
    print(f"  Persons reused (1):      {persons_reused}")
    print(f"  Persons multi-match:     {persons_multi}")
    print(f"  Overworked duplicates:   {len(dupe_names)}")
    print(f"  Connections created:     {connections_created}")
    print(f"    confirmed=true:        {confirmed_count}")
    print(f"    confirmed=false:       {unconfirmed_count}")


if __name__ == "__main__":
    main()
