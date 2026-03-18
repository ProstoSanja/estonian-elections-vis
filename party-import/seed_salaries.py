import csv
import sys
from datetime import datetime
from collections import defaultdict

import psycopg

CSV_PATH = "/Users/alex/Downloads/tableConvert.com_p9kagb.csv"

RETRY_DUPLICATES = "retry_duplicates.csv"
RETRY_AMBIGUOUS = "retry_ambiguous.csv"
RETRY_NO_ORG = "retry_no_org.csv"
RETRY_MULTI_ORG = "retry_multi_org.csv"

ALLOWED_PAIRS = [
    ("Päästeamet", "Sisekaitseakadeemia"),
]

ORG_ALIASES = {
    "maa-amet": "maa- ja ruumiamet",
    "justiitsministeerium": "justiits- ja digiministeerium",
}

DB_CONFIG = dict(
    host="localhost",
    port=5432,
    dbname="election-vis",
    user="postgres_user",
    password="postgres_pass",
)


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


def parse_koormus(s):
    s = s.strip().replace(",", ".")
    try:
        return float(s)
    except ValueError:
        return 0.0


def parse_salary(s):
    s = s.strip().replace(",", "")
    try:
        return round(float(s))
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


# --- Phase 1: Duplicate detection ---

def detect_duplicates():
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
            dupe_names.add((first.lower(), last.lower()))

    return dupe_names


# --- Phase 2: Import ---

def load_org_cache(cur):
    cur.execute("SELECT id, LOWER(TRIM(name)) FROM monitoring_group")
    cache = defaultdict(list)
    for row in cur.fetchall():
        cache[row[1]].append(row[0])
    return cache


def main():
    input_file = sys.argv[1] if len(sys.argv) > 1 else CSV_PATH
    is_retry = input_file != CSV_PATH

    if is_retry:
        print(f"Retry mode: importing from {input_file}")
        print("  Skipping duplicate detection\n")
        dupe_names = set()
    else:
        print("Phase 1: Detecting duplicates...")
        dupe_names = detect_duplicates()
        print(f"  Found {len(dupe_names)} duplicate name pairs to skip\n")

    conn = psycopg.connect(**DB_CONFIG, autocommit=False)

    with open(input_file, encoding="utf-8") as f:
        reader = csv.reader(f)
        header = next(reader)
        if not is_retry:
            next(reader)  # column numbers row (only in original file)
        all_rows = [row for row in reader if len(row) >= 11]

    # person cache: (lower_first, lower_last) -> individual_id
    person_cache = {}
    with conn.cursor() as cur:
        org_cache = load_org_cache(cur)

    retry_dupes = []
    retry_ambiguous = []
    retry_no_org = []
    retry_multi_org = []

    persons_created = 0
    persons_reused = 0
    memberships_inserted = 0
    rows_processed = 0

    with conn.cursor() as cur:
        for row in all_rows:
            org_name = row[0].strip()
            unit = row[1].strip()
            position = row[2].strip()
            first = row[3].strip()
            last = row[4].strip()
            salary = parse_salary(row[9])
            start_date, end_date = parse_period(row[10].strip())

            if not start_date or not end_date:
                continue

            rows_processed += 1
            key = (first.lower(), last.lower())

            if key in dupe_names:
                retry_dupes.append(row)
                continue

            # Look up or create person
            if key in person_cache:
                person_id = person_cache[key]
                persons_reused += 1
            else:
                cur.execute(
                    "SELECT id FROM monitoring_individual WHERE LOWER(firstname) = %s AND LOWER(surname) = %s",
                    (first.lower(), last.lower()),
                )
                results = cur.fetchall()
                if len(results) == 0:
                    cur.execute(
                        """INSERT INTO monitoring_individual (name, firstname, surname)
                           VALUES (%s, %s, %s) RETURNING id""",
                        (f"{first} {last}", first, last),
                    )
                    person_id = cur.fetchone()[0]
                    person_cache[key] = person_id
                    persons_created += 1
                elif len(results) == 1:
                    person_id = results[0][0]
                    person_cache[key] = person_id
                    persons_reused += 1
                else:
                    retry_ambiguous.append(row)
                    continue

            # Look up organization
            org_key = org_name.lower().strip()
            org_key = ORG_ALIASES.get(org_key, org_key)
            org_ids = org_cache.get(org_key, [])
            if len(org_ids) == 0:
                retry_no_org.append(row)
                continue
            elif len(org_ids) > 1:
                retry_multi_org.append(row)
                continue

            group_id = org_ids[0]

            cur.execute(
                """INSERT INTO monitoring_group_membership
                   (group_id, individual_id, position, position_income, position_confirmed, start_date, end_date)
                   VALUES (%s, %s, %s, %s, TRUE, %s, %s)""",
                (group_id, person_id, f"{unit} - {position}", salary, start_date, end_date),
            )
            memberships_inserted += 1

    conn.commit()
    conn.close()

    # Write retry files
    def write_retry(filename, rows):
        if not rows:
            return
        with open(filename, "w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f)
            writer.writerow(header)
            writer.writerows(rows)

    prefix = "re_" if is_retry else ""
    dupes_file = prefix + RETRY_DUPLICATES
    ambig_file = prefix + RETRY_AMBIGUOUS
    no_org_file = prefix + RETRY_NO_ORG
    multi_org_file = prefix + RETRY_MULTI_ORG

    write_retry(dupes_file, retry_dupes)
    write_retry(ambig_file, retry_ambiguous)
    write_retry(no_org_file, retry_no_org)
    write_retry(multi_org_file, retry_multi_org)

    print("Done.\n")
    print(f"  Total rows processed:    {rows_processed}")
    print(f"  Persons created:         {persons_created}")
    print(f"  Persons reused:          {persons_reused}")
    print(f"  Memberships inserted:    {memberships_inserted}")
    print(f"  Duplicates skipped:      {len(retry_dupes):>5}  -> {dupes_file}")
    print(f"  Ambiguous persons:       {len(retry_ambiguous):>5}  -> {ambig_file}")
    print(f"  Org not found:           {len(retry_no_org):>5}  -> {no_org_file}")
    print(f"  Multiple orgs found:     {len(retry_multi_org):>5}  -> {multi_org_file}")


if __name__ == "__main__":
    main()
