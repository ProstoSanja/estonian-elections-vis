# Import Salary Data

**Source**: `https://www.fin.ee/sites/default/files/documents/2025-05/Ametnike%20p%C3%B5hipalgad%20seisuga%2001.04.2025%20ja%20kogupalgad%202024_0.xlsx`

Two CSV sheets extracted from the XLSX: `salary_non_kov.csv` (central government, 12,958 rows) and `salary_kov.csv` (local government, 3,964 rows).

## Preprocessing

### KOV data quality notes
- 408 KOV rows have trailing whitespace in Eesnimi — trimmed during import
- Date format issues (dashes instead of dots, multi-period entries, missing periods) are handled automatically by the import script's `clean_period()` function — no manual CSV fixes required

### Merge into `salary_merged.csv` (16,922 rows)
KOV column `Kohalik omavalitsus[0]` dropped (unused), remaining columns aligned with non-KOV. Unified column layout:

`Asutus[0], Struktuuriüksus[1], Ametikoht[2], Eesnimi[3], Perekonnanimi[4], Ametniku koormus[5], Põhipalk[6], Muutuvpalk[7], Muu tulu[8], Kokku[9], Periood[10], Ametikoht muutus[11]`

### Org name aliases (all lowercase for matching)
| CSV name | Correct name (in monitoring_entries) |
|---|---|
| maa-amet | maa- ja ruumiamet |
| justiitsministeerium | justiits- ja digiministeerium |
| tartu linnavolikogu | tartu linnavalitsus |
| strateegiakeskus | tallinna strateegiakeskus |
| häädemeeste vald | häädemeeste vallavalitsus |
| kesklinna valitsus | tallinna kesklinna valitsus |
| taebla/kullamaa/martna/noarootsi/oru/palivere/risti osavalla valitsus | lääne-nigula vallavalitsus |
| emmaste/käina/kärdla/kärdla ja pühalepa/kõrgessaare/pühalepa osavalla valitsus | hiiumaa vallavalitsus |

### When Struktuuriüksus is empty
Use just `Ametikoht` for connection name instead of `Struktuuriüksus - Ametikoht`.

## Overworked duplicate detection (on merged file)

A `(Eesnimi, Perekonnanimi)` pair is flagged as "overworked" when:
1. 2+ entries exist for that name pair
2. After excluding entries where Struktuuriüksus contains "fraktsioon" or "ministeerium" AND Ametikoht contains "nõunik"
3. At least two remaining entries have overlapping time periods
4. AND are NOT the same (Struktuuriüksus, Ametikoht) with combined koormus <= 1.85
5. AND are NOT (Päästeamet, Sisekaitseakadeemia) when one Struktuuriüksus contains "õppetool"

Overworked rows are NOT skipped — they are imported with `confirmed=false`.

## Import logic

### Phase 1: Pre-validate all organizations
Before any inserts, every unique `Asutus` value (after alias resolution) is checked against `monitoring_entries`. Script aborts if any org is missing or matches multiple entries.

### Phase 2: Process each row
1. **Org lookup**: resolved from pre-validated cache (guaranteed 1:1).
2. **Person lookup** (case-insensitive firstname + surname in `monitoring_entries`):
   - 0 found → create `monitoring_entries` with `type=INDIVIDUAL`, `name="First Last"`, `nameParts`, `birthDate=null`, IDs empty. Results cached in-memory.
   - 1+ found → reuse all matched IDs
3. **Insert** `monitoring_entry_connections`:
   - `connectedIds=[org_id] + person_ids` (all matched person IDs)
   - `parentId=null`
   - `name="Struktuuriüksus - Ametikoht"` (or just `Ametikoht` if empty)
   - `type=EMPLOYMENT`
   - `confirmed=true` only if exactly 1 person matched/created AND not an overworked duplicate; `false` otherwise
   - `monetaryValue=Kokku` as double (commas stripped)
   - `startDate`, `endDate` from Periood (`dd.MM.yyyy-dd.MM.yyyy`)
   - `sources=[source URL]`

### Salary parsing
`"35,670.88"` → remove commas → `35670.88` (stored as double)

### Period parsing and cleanup (in script)
Before parsing, the script automatically fixes known date format issues:
- `"-"` or empty → defaults to `01.01.2024-31.12.2024`
- `"31-12.2024"` → `"31.12.2024"` (dash instead of dot within date)
- `"02.01-2024"` → `"02.01.2024"` (dash instead of dot within date)
- `"01.01.2024.31.12.2024"` → `"01.01.2024-31.12.2024"` (dot instead of dash separator)
- Multi-period entries (comma/semicolon separated) → first start to last end
- Any remaining unparseable period → defaults to `01.01.2024-31.12.2024`

After cleanup: split on `-` → parse each half as `dd.MM.yyyy`
