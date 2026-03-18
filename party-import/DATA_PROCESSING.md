# Data Processing Steps

## Source file

`Avaliku_sektori_asutused_asutuse_liikide_loikes_juuli_2025 (1).csv` — 2221 rows of Estonian public sector institutions, downloaded from the Ministry of Finance public sector registry.

Columns: `Sektor, Kõrgemal seisev asutus, Registrikood, Üksuse nimi, Üksuse liik, Muudatused`

## Step 1: Split by Sektor

Split the original CSV into two files based on the `Sektor` column:

- `avalik_sektor_kov.csv` — rows where `Sektor = "KOV"` (1794 rows)
- `avalik_sektor_non_kov.csv` — all other sectors: Keskvalitsus, Muu avalik sektor, Sotsiaalkindlustusfondid (427 rows)

## Step 2: Move KOV-parented items from non-KOV to KOV

Rows in the non-KOV file where `Kõrgemal seisev asutus` matched any `Üksuse nimi` in the KOV file, or equaled `"mitme KOVi poolt asutatud"`, were moved to the KOV file. This moved 90 rows.

## Step 3: Second pass with parent org names

The previous step only matched against `Üksuse nimi` values from the KOV file. Some parent organizations (e.g. "Tallinna Linnavalitsus") only appeared as `Kõrgemal seisev asutus` values in KOV rows, never as `Üksuse nimi`. A second pass also collected `Kõrgemal seisev asutus` values from KOV rows into the lookup set. This moved 9 more rows.

## Step 4: Rename parent org

The CSV listed "Tallinna Linnavalitsus" as the parent organization, but the correct name is "Tallinna Linnakantselei". Updated `Kõrgemal seisev asutus` from `"Tallinna Linnavalitsus"` to `"Tallinna Linnakantselei"` in the KOV file (269 rows affected).

## Step 5: Reorder files

Both files were reordered so that top-level entries (where `Kõrgemal seisev asutus == Üksuse nimi`) appear at the top.

- KOV: 78 top-level entries
- Non-KOV: 18 top-level entries

## Step 6: Fix casing inconsistency

Two rows in the non-KOV file had `Kõrgemal seisev asutus = "Haridus- Ja Teadusministeerium"` (capital "Ja") instead of the correct `"Haridus- ja Teadusministeerium"`. Fixed to match the canonical name.

Affected entries: Rapla Gümnaasium, Viimsi Gümnaasium.

## Step 7: Fix casing and rename Narva parent in KOV file

Similar to step 4 and 6, fixed casing and parent name issues in the KOV file:

- `"Nõo Vallavalitsus"` → `"Nõo vallavalitsus"` (10 rows) — matched DB casing
- `"Viru-Nigula Vallavalitsus"` → `"Viru-Nigula vallavalitsus"` (2 rows) — matched DB casing
- `"Narva Linnavalitsus"` → `"Narva Linnakantselei"` (45 rows) — same fix as Tallinn in step 4

File was reordered again to keep top-level entries at the top.

## Final result

- `avalik_sektor_kov.csv` — 1893 rows
- `avalik_sektor_non_kov.csv` — 328 rows

## Database import (non-KOV)

Imported `avalik_sektor_non_kov.csv` into `monitoring_group` and `monitoring_group_relationship` tables using `seed_non_kov.py`.

### Type mapping (Üksuse liik → monitoring_group.type)

| Üksuse liik | type |
|---|---|
| põhiseaduslikud institutsioonid ja Riigikantselei | GOV |
| riigi ametiasutused | GOV |
| mittetulundusühingud | GOV_OWNED |
| äriühingud ja tulundusasutused | GOV_OWNED |
| avalik-õiguslikud asutused | GOV_PUBLIC |
| sihtasutused | GOV_PUBLIC |
| hallatavad riigiasutused | GOV_PUBLIC |

### Import logic

1. Look up existing entry by `Registrikood` (`external_id_ariregister`). If found, skip insert.
2. If not found by code, look up by `Üksuse nimi`. If found with a different registry code, log as error. If found with matching code, skip insert.
3. If not found at all, insert into `monitoring_group`.
4. If `Kõrgemal seisev asutus == Üksuse nimi` (top-level entry), skip relationship creation.
5. Otherwise, look up the parent organization by name and create a `monitoring_group_relationship` entry.

### Result

- 247 new groups inserted
- 81 skipped (already existed from prior `seed_institutions.py` import)
- 310 relationships created (308 from script + 2 manually fixed)
- 0 errors

## Database import (KOV)

Imported `avalik_sektor_kov.csv` into `monitoring_group` and `monitoring_group_relationship` tables using `seed_kov.py`.

### Type mapping (Üksuse liik → monitoring_group.type)

| Üksuse liik | Condition | type |
|---|---|---|
| valla või linna ametiasutus | name = parent, or contains "valitsus"/"amet" (case-insensitive) | GOV_KOV |
| valla või linna ametiasutus | other cases | GOV_KOV_PUBLIC |
| valla või linna ametiasutuste hallatav asutus | name = parent, or contains "valitsus"/"amet" (case-insensitive) | GOV_KOV |
| valla või linna ametiasutuste hallatav asutus | other cases | GOV_KOV_PUBLIC |
| äriühingud ja tulundusasutused | — | GOV_KOV_OWNED |
| sihtasutused | — | GOV_KOV_PUBLIC |
| mittetulundusühingud | — | GOV_KOV_OWNED |

### Import logic

Same lookup/insert logic as non-KOV, with one difference: when a parent is not found and the entry is not a self-parent, the entry is logged as an error and removed from the database.

### Result

- 1942 groups inserted (1811 first run + 131 after casing/Narva fixes)
- 82 skipped (already existed)
- 2049 relationships created (after deduplication)
- 76 entries removed (no parent found — "mitme KOVi poolt asutatud" and "riigi ja KOVi poolt asutatud" entities)

## Deduplication and unique constraint

After the KOV import, 1683 duplicate `monitoring_group_relationship` entries were found (caused by re-running the script). Duplicates were removed (keeping lowest id per pair), and a `UNIQUE (parent_group_id, child_group_id)` constraint was added to the table. The migration file and import scripts were updated with `ON CONFLICT DO NOTHING` to prevent future duplicates.
