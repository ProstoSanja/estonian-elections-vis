# Import Party Contributions (ERJK)

**Source**: `https://erjk.ee/avaandmed/#/`

**API**: `https://erjk.ee/api/quarterly-reports/`

## Process

### Phase 1: Validate parties
- Fetch party list from ERJK API
- Match each against `monitoring_entries` (type=PARTY) by name, case-insensitive
- Store `erjkId` on party entry if not already set
- Skip 4 defunct parties not in our DB: Eesti Iseseisvuspartei, Eesti Vabaerakond, Elurikkuse Erakond, Erakond Rahva Tahe

### Phase 2: Fetch quarterly reports
For each matched party:
- Fetch quarter list from `/api/quarterly-reports/quarters/{party_id}`
- For each quarter, fetch receipts from `/api/quarterly-reports/{report_id}?report_type=receipts`

### Phase 3: Process receipts
Each receipt row: `{date, receipt_category, name, birthdate, amount}`

**Donor type detection**: If `birthdate` is digits-only (registrikood format, no dots), treat as organization. Otherwise treat as person.

**Person lookup**: Case-insensitive full name match via `common/person_lookup.py`. Creates if not found, patches birthdate if exactly 1 found without one. No firstName/lastName splitting.

**Org lookup**: By `ids.estGovId` first, then by name. Creates as `type=BUSINESS` if not found. Raises error if multiple found.

### Connection type mapping

| receipt_category | type |
|---|---|
| Liikmemaks | PARTY_MEMBERSHIP |
| Pangalaen | BANK_LOAN |
| Everything else | DONATION |

### Connection fields
- `connectedIds=[donor_id(s), party_id]`
- `name=receipt_category`
- `confirmed=true` if exactly 1 donor, `false` if multiple matches
- `monetaryValue=amount` (integer euros)
- `startDate=date` from receipt
- `sources=[erjk avaandmed URL, specific report URL]`
