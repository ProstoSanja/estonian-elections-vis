import csv
import sys

import psycopg

CSV_PATH = sys.argv[1] if len(sys.argv) > 1 else "/Users/alex/Downloads/padevad_asutused_20260316.csv"

TYPE_MAP = {
    "Valla või linna ametiasutus": "GOV_KOV",
    "Valla või linna ametiasutuste hallatav asutus": "GOV_KOV",
    "Avalik-õiguslik juriidiline isik": "GOV_PUBLIC",
}

DB_CONFIG = dict(
    host="localhost",
    port=5432,
    dbname="election-vis",
    user="postgres_user",
    password="postgres_pass",
)


def resolve_type(alaliik: str) -> str:
    return TYPE_MAP.get(alaliik, "GOV")


def main():
    conn = psycopg.connect(**DB_CONFIG, autocommit=False)
    count = 0

    with open(CSV_PATH, encoding="utf-8") as f:
        reader = csv.reader(f, delimiter=";")
        next(reader)

        with conn.cursor() as cur:
            for row in reader:
                if len(row) < 5:
                    continue
                name = row[0].strip()
                registrikood = row[1].strip()
                alaliik = row[4].strip()
                group_type = resolve_type(alaliik)

                cur.execute(
                    """INSERT INTO monitoring_group (name, type, external_id_ariregister)
                       VALUES (%s, %s, %s)""",
                    (name, group_type, registrikood),
                )
                count += 1

    conn.commit()
    conn.close()
    print(f"Done. Inserted {count} institutions into monitoring_group.")


if __name__ == "__main__":
    main()
