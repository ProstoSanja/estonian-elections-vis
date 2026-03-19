# Import Salary Data

**Sources** (2017–2024): XLSX files from `fin.ee`, each containing KOV (local government) and RIIK (central government) kogupalk sheets. See `SOURCE_URLS` in `import_salaries.py` for exact URLs.

## Usage

```bash
source ../venv/bin/activate
python import_salaries.py salary_merged_2023.csv
```

CSV path defaults to `salary_merged.csv` if not provided. Data year is derived automatically from the first row's period column.

## Merged CSV files

Each year has a `salary_merged_{year}.csv` with unified 12-column layout:

`Asutus[0], Struktuuriüksus[1], Ametikoht[2], Eesnimi[3], Perekonnanimi[4], Ametniku koormus[5], Põhipalk[6], Muutuvpalk[7], Muu tulu[8], Kokku[9], Periood[10], Ametikoht muutus[11]`

KOV files for 2020–2024 had an extra `Kohalik omavalitsus` first column — dropped during merge. KOV files for 2017–2019 already matched the RIK layout. XLSX files for 2017–2021 were extracted with `openpyxl` (`data_only=True`, floats rounded to 2dp).

## Preprocessing (handled automatically by the script)

- All cell values are `.strip()`-ed on CSV load
- Struktuuriüksus of `"-"` or equal to Asutus is treated as empty

### Period parsing

Extracts all digit groups from the period string:
- 0 groups or empty/`"-"` → defaults to `01.01.{year}-31.12.{year}`
- 6+ groups where count is a multiple of 3 → takes first 3 as start date (dd, MM, yyyy), last 3 as end date
- This handles any mix of `.` and `-` separators, multi-period entries (semicolon/comma/newline separated), and extra whitespace
- Any other count → pre-check aborts with line numbers

### Org name aliases

See `ORG_ALIASES` dict in script. Covers:
- Typos (e.g. `vinni vallavaitsus` → `vinni vallavalitsus`)
- Renamed ministries (e.g. `keskkonnaministeerium` → `kliimaministeerium`)
- Volikogu → valitsus (e.g. `tartu linnavolikogu` → `tartu linnavalitsus`)
- Osavalla/osavallavalitsus variants → merged municipality (e.g. `→ lääne-nigula vallavalitsus`, `→ hiiumaa vallavalitsus`)
- `KEEP_ORIGINAL_NAME_ALIASES`: for aliases like `vigala osavallavalitsus` and `nõva osavallavalitsus`, the original org name is prepended to the connection name

### Connection name

`"Struktuuriüksus - Ametikoht"`, or just `"Ametikoht"` when Struktuuriüksus is empty, `"-"`, or equals Asutus. For `KEEP_ORIGINAL_NAME_ALIASES` entries: `"OriginalOrg - Struktuuriüksus - Ametikoht"`.

## Import phases

### Pre-check: Validate periods and derive year
Parses all periods, derives data year from first valid row, validates source URL exists. Aborts if any period is unparseable.

### Phase 1: Detect overworked duplicates
A `(Eesnimi, Perekonnanimi)` pair is flagged as "overworked" when:
1. 2+ entries exist for that name pair
2. After excluding entries where Struktuuriüksus contains "fraktsioon" or "ministeerium" AND Ametikoht contains "nõunik"
3. At least two remaining entries have overlapping time periods
4. AND are NOT the same (Struktuuriüksus, Ametikoht) with combined koormus <= 1.85
5. AND are NOT (Päästeamet, Sisekaitseakadeemia) when one Struktuuriüksus contains "õppetool"

Overworked rows are NOT skipped — they are imported with `confirmed=false`.

### Phase 2: Pre-validate all organizations
Every unique `Asutus` value (after alias resolution) is checked against `monitoring_entries`. Script aborts if any org is missing or matches multiple entries.

### Phase 3: Import salary data
1. **Org lookup**: resolved from pre-validated cache (guaranteed 1:1)
2. **Person lookup** (case-insensitive firstname + surname in `monitoring_entries`):
   - 0 found → create `monitoring_entries` with `type=INDIVIDUAL`, `name="First Last"`, `nameParts`, `birthDate=null`, IDs empty. Results cached in-memory.
   - 1+ found → reuse all matched IDs
3. **Insert** `monitoring_entry_connections`:
   - `connectedIds=[org_id] + person_ids`
   - `name` = connection name (see above)
   - `type=EMPLOYMENT`
   - `confirmed=true` only if exactly 1 person matched/created AND not an overworked duplicate
   - `monetaryValue=Kokku` as double (commas stripped)
   - `startDate`, `endDate` from Periood
   - `sources=[source URL for year]`
