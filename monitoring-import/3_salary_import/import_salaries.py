import csv
import sys
from datetime import datetime
from collections import defaultdict

from pymongo import MongoClient

CSV_PATH = "salary_merged.csv"
SOURCE_URL = "https://www.fin.ee/sites/default/files/documents/2025-05/Ametnike%20p%C3%B5hipalgad%20seisuga%2001.04.2025%20ja%20kogupalgad%202024_0.xlsx"

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
    "emmaste osavalla valitsus": "hiiumaa vallavalitsus",
    "käina osavalla valitsus": "hiiumaa vallavalitsus",
    "kärdla osavalla valitsus": "hiiumaa vallavalitsus",
    "kärdla ja pühalepa osavalla valitsus": "hiiumaa vallavalitsus",
    "kõrgessaare osavalla valitsus": "hiiumaa vallavalitsus",
    "pühalepa osavalla valitsus": "hiiumaa vallavalitsus",
}


DEFAULT_START = datetime(2024, 1, 1)
DEFAULT_END = datetime(2024, 12, 31)


def clean_period(period_str):
    """Fix known date format issues before parsing."""
    import re
    s = period_str.strip()

    if not s or s == "-":
        return None

    # Multi-period (comma or semicolon separated) -> first start to last end
    if "," in s or ";" in s:
        parts = re.split(r"[,;]", s)
        parts = [p.strip() for p in parts if p.strip()]
        if parts:
            first_start = parts[0].split("-")[0].strip()
            last_end = parts[-1].split("-")[-1].strip()
            s = f"{first_start}-{last_end}"

    # Fix "31-12.2024" -> "31.12.2024" (dash instead of dot in date part)
    s = re.sub(r"(\d{2})-(\d{2})\.(\d{4})", r"\1.\2.\3", s)
    # Fix "02.01-2024" -> "02.01.2024" (dash instead of dot in date part)
    s = re.sub(r"(\d{2})\.(\d{2})-(\d{4})", r"\1.\2.\3", s)
    # Fix "01.01.2024.31.12.2024" -> "01.01.2024-31.12.2024" (dot instead of dash separator)
    s = re.sub(r"(\d{2}\.\d{2}\.\d{4})\.(\d{2}\.\d{2}\.\d{4})", r"\1-\2", s)

    return s


def parse_period(period_str):
    cleaned = clean_period(period_str)
    if not cleaned:
        return DEFAULT_START, DEFAULT_END

    parts = cleaned.split("-")
    if len(parts) != 2:
        return DEFAULT_START, DEFAULT_END
    try:
        start = datetime.strptime(parts[0].strip(), "%d.%m.%Y")
        end = datetime.strptime(parts[1].strip(), "%d.%m.%Y")
        return start, end
    except ValueError:
        return DEFAULT_START, DEFAULT_END


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
        all_rows = [row for row in reader if len(row) >= 11]
    print(f"  {len(all_rows)} rows loaded\n")

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

    for row in all_rows:
        org_name = row[0].strip()
        unit = row[1].strip()
        position = row[2].strip()
        first = row[3].strip()
        last = row[4].strip()
        salary = parse_salary(row[9])
        start_date, end_date = parse_period(row[10].strip())

        if not first or not last:
            continue

        if start_date == DEFAULT_START and end_date == DEFAULT_END:
            raw = row[10].strip()
            if raw != "01.01.2024-31.12.2024":
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

        if unit:
            pos_str = f"{unit} - {position}"
        else:
            pos_str = position

        connection_doc = {
            "connectedIds": [org_id] + person_ids,
            "parentId": None,
            "name": pos_str,
            "type": "EMPLOYMENT",
            "confirmed": confirmed,
            "monetaryValue": salary,
            "startDate": start_date,
            "endDate": end_date,
            "sources": [{"sourceUrl": SOURCE_URL}],
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
    print(f"  Persons created:         {persons_created}")
    print(f"  Persons reused (1):      {persons_reused}")
    print(f"  Persons multi-match:     {persons_multi}")
    print(f"  Overworked duplicates:   {len(dupe_names)}")
    print(f"  Connections created:     {connections_created}")
    print(f"    confirmed=true:        {confirmed_count}")
    print(f"    confirmed=false:       {unconfirmed_count}")


if __name__ == "__main__":
    main()
