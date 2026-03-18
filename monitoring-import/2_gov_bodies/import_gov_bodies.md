# Import Government Bodies

**Source**: `https://www.fin.ee/sites/default/files/documents/2025-10/Avaliku_sektori_asutused_asutuse_liikide_loikes_juuli_2025.xlsx`

Original file converted to CSV (2221 rows). Columns: `Sektor, Kõrgemal seisev asutus, Registrikood, Üksuse nimi, Üksuse liik, Muudatused`

## Manual CSV preprocessing (already applied to files in this folder)

### 1. Split by Sektor
- `Sektor = "KOV"` → `avalik_sektor_kov.csv`
- Everything else (Keskvalitsus, Muu avalik sektor, Sotsiaalkindlustusfondid) → `avalik_sektor_non_kov.csv`

### 2. Move KOV-parented items from non-KOV to KOV
Non-KOV rows where `Kõrgemal seisev asutus` matched any `Üksuse nimi` or `Kõrgemal seisev asutus` in the KOV file, or equaled `"mitme KOVi poolt asutatud"`, were moved to KOV. (99 rows moved across two passes.)

### 3. Parent org name corrections
- `"Tallinna Linnavalitsus"` → `"Tallinna Linnakantselei"` in KOV file (269 rows)
- `"Narva Linnavalitsus"` → `"Narva Linnakantselei"` in KOV file (45 rows)

### 4. Casing fixes
- `"Haridus- Ja Teadusministeerium"` → `"Haridus- ja Teadusministeerium"` in non-KOV (2 rows)
- `"Nõo Vallavalitsus"` → `"Nõo vallavalitsus"` in KOV (10 rows)
- `"Viru-Nigula Vallavalitsus"` → `"Viru-Nigula vallavalitsus"` in KOV (2 rows)

### 5. Reorder
Both files reordered so self-parent entries (`Kõrgemal seisev asutus == Üksuse nimi`) appear at the top.

### Final file sizes
- `avalik_sektor_non_kov.csv` — 328 rows
- `avalik_sektor_kov.csv` — 1893 rows

## Type mapping (Üksuse liik → MonitoringEntryType)

### Non-KOV
| Üksuse liik | type |
|---|---|
| põhiseaduslikud institutsioonid ja Riigikantselei | GOV |
| riigi ametiasutused | GOV |
| mittetulundusühingud | BUSINESS |
| äriühingud ja tulundusasutused | BUSINESS |
| avalik-õiguslikud asutused | PUBLIC_BODY |
| sihtasutused | PUBLIC_BODY |
| hallatavad riigiasutused | PUBLIC_BODY |

### KOV
| Üksuse liik | Condition | type |
|---|---|---|
| valla või linna ametiasutus | name == parent, or contains "valitsus"/"amet" | GOV_KOV |
| valla või linna ametiasutus | other | PUBLIC_BODY |
| valla või linna ametiasutuste hallatav asutus | name == parent, or contains "valitsus"/"amet" | GOV_KOV |
| valla või linna ametiasutuste hallatav asutus | other | PUBLIC_BODY |
| äriühingud ja tulundusasutused | — | BUSINESS |
| sihtasutused | — | PUBLIC_BODY |
| mittetulundusühingud | — | BUSINESS |

## Business name/suffix extraction

Estonian company suffixes stripped from `Üksuse nimi` to populate `nameParts.businessName` and `nameParts.businessSuffix`. Suffixes detected at start or end of name (case-insensitive): `aktsiaselts`, `osaühing`, `mittetulundusühing`, `sihtasutus`, `as`, `oü`, `mtü`, `sa`. Surrounding quotes also stripped from business name.

## Import logic

For each row:
1. Insert into `monitoring_entries`: `estGovId=Registrikood`, `name=Üksuse nimi`, `type=mapped`, `nameParts` with extracted business name/suffix.
2. If self-parent (`Kõrgemal seisev asutus` == `Üksuse nimi`, case-insensitive), no connection created.
3. If parent is `"mitme KOVi poolt asutatud"` or `"riigi ja KOVi poolt asutatud"`: create `monitoring_entry_connections` with `connectedIds=[self]`, `parentId=null`, `type=BUSINESS_OWNERSHIP`, `confirmed=false`.
4. Otherwise: look up parent by name (in-memory cache, case-insensitive), create `monitoring_entry_connections` with `connectedIds=[parent, self]`, `parentId=parent`, `name="Kõrgemal seisev asutus"`, `type=BUSINESS_OWNERSHIP`, `confirmed=true`.

## Result

- 2221 entries inserted (328 non-KOV + 1893 KOV)
- 2048 connections created (310 non-KOV + 1738 KOV)
- 76 CONNECTION_PENDING entries (parentless multi-KOV/state entities)
