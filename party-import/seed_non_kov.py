import csv
import sys

import psycopg

CSV_PATH = "avalik_sektor_non_kov.csv"

TYPE_MAP = {
    "põhiseaduslikud institutsioonid ja Riigikantselei": "GOV",
    "mittetulundusühingud": "GOV_OWNED",
    "avalik-õiguslikud asutused": "GOV_PUBLIC",
    "riigi ametiasutused": "GOV",
    "äriühingud ja tulundusasutused": "GOV_OWNED",
    "sihtasutused": "GOV_PUBLIC",
    "hallatavad riigiasutused": "GOV_PUBLIC",
}

DB_CONFIG = dict(
    host="localhost",
    port=5432,
    dbname="election-vis",
    user="postgres_user",
    password="postgres_pass",
)


def find_group_by_code(cur, code: str):
    cur.execute("SELECT id, name FROM monitoring_group WHERE external_id_ariregister = %s", (code,))
    return cur.fetchone()


def find_group_by_name(cur, name: str):
    cur.execute("SELECT id, external_id_ariregister FROM monitoring_group WHERE name = %s", (name,))
    return cur.fetchone()


def main():
    conn = psycopg.connect(**DB_CONFIG, autocommit=False)
    inserted = 0
    skipped = 0
    errors = 0
    relationships = 0

    with open(CSV_PATH, encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader)

        with conn.cursor() as cur:
            for row in reader:
                if len(row) < 5:
                    continue

                parent_name = row[1].strip()
                code = row[2].strip()
                name = row[3].strip()
                liik = row[4].strip()
                group_type = TYPE_MAP.get(liik)

                if not group_type:
                    print(f"  ERROR: Unknown Üksuse liik '{liik}' for {name}", file=sys.stderr)
                    errors += 1
                    continue

                # Check by registrikood
                existing_by_code = find_group_by_code(cur, code)
                if existing_by_code:
                    group_id = existing_by_code[0]
                    skipped += 1
                else:
                    # Check by name
                    existing_by_name = find_group_by_name(cur, name)
                    if existing_by_name:
                        existing_code = existing_by_name[1]
                        if existing_code and existing_code != code:
                            print(f"  ERROR: '{name}' exists with code {existing_code}, but CSV has {code}", file=sys.stderr)
                            errors += 1
                            continue
                        group_id = existing_by_name[0]
                        skipped += 1
                    else:
                        cur.execute(
                            """INSERT INTO monitoring_group (name, type, external_id_ariregister)
                               VALUES (%s, %s, %s) RETURNING id""",
                            (name, group_type, code),
                        )
                        group_id = cur.fetchone()[0]
                        inserted += 1

                # If top-level (parent == self), no relationship needed
                if parent_name == name:
                    continue

                # Look up parent by name
                parent = find_group_by_name(cur, parent_name)
                if not parent:
                    print(f"  WARN: Parent '{parent_name}' not found for '{name}'", file=sys.stderr)
                    continue

                parent_id = parent[0]
                cur.execute(
                    """INSERT INTO monitoring_group_relationship (parent_group_id, child_group_id)
                       VALUES (%s, %s) ON CONFLICT DO NOTHING""",
                    (parent_id, group_id),
                )
                relationships += 1

    conn.commit()
    conn.close()

    print(f"\nDone.")
    print(f"  Inserted: {inserted}")
    print(f"  Skipped (already existed): {skipped}")
    print(f"  Relationships created: {relationships}")
    print(f"  Errors: {errors}")


if __name__ == "__main__":
    main()
