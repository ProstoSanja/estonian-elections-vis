import csv
import io
import sys
from datetime import datetime

import psycopg
import requests

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

DB_CONFIG = dict(
    host="localhost",
    port=5432,
    dbname="election-vis",
    user="postgres_user",
    password="postgres_pass",
)


def parse_date(date_str: str):
    date_str = date_str.strip()
    if not date_str:
        return None
    return datetime.strptime(date_str, "%d.%m.%Y").date()


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
    next(reader)  # skip header
    return [row for row in reader if len(row) >= 4]


def find_or_create_individual(cur, firstname: str, surname: str, birthdate):
    cur.execute(
        "SELECT id FROM monitoring_individual WHERE firstname = %s AND surname = %s AND birthdate = %s",
        (firstname, surname, birthdate),
    )
    result = cur.fetchone()
    if result:
        return result[0]

    cur.execute(
        """INSERT INTO monitoring_individual (name, firstname, surname, birthdate)
           VALUES (%s, %s, %s, %s) RETURNING id""",
        (f"{firstname} {surname}", firstname, surname, birthdate),
    )
    return cur.fetchone()[0]


def main():
    conn = psycopg.connect(**DB_CONFIG, autocommit=False)
    total_members = 0

    try:
        for party_name, code in PARTIES:
            print(f"Processing: {party_name} ({code})...")

            with conn.cursor() as cur:
                cur.execute(
                    """INSERT INTO monitoring_group (name, type, external_id_ariregister)
                       VALUES (%s, 'PARTY', %s) RETURNING id""",
                    (party_name, code),
                )
                group_id = cur.fetchone()[0]

            try:
                rows = download_csv(code)
            except Exception as e:
                print(f"  WARN: Failed to download CSV for {party_name}: {e}", file=sys.stderr)
                conn.rollback()
                continue

            member_count = 0
            with conn.cursor() as cur:
                for row in rows:
                    firstname = row[0].strip()
                    surname = row[1].strip()
                    birthdate = parse_date(row[2])
                    start_date = parse_date(row[3])

                    if not birthdate or not start_date:
                        continue

                    ind_id = find_or_create_individual(cur, firstname, surname, birthdate)

                    cur.execute(
                        """INSERT INTO monitoring_group_membership (group_id, individual_id, position, start_date)
                           VALUES (%s, %s, 'Member', %s)""",
                        (group_id, ind_id, start_date),
                    )
                    member_count += 1

            conn.commit()
            total_members += member_count
            print(f"  Inserted {member_count} members (group_id={group_id})")

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()

    print(f"\nDone. Total members inserted: {total_members}")


if __name__ == "__main__":
    main()
