# Estonian Elections & Political Monitoring Platform

## Project Overview

This project builds a comprehensive database of Estonian political and governmental data — tracking individuals, organizations, political parties, government bodies, and the relationships between them (employment, ownership, administration, party membership, donations, etc.). The data is sourced from multiple Estonian government APIs, public datasets, and web scraping.

The system has two main components:
1. **Data ingestion pipeline** (`monitoring-import/`) — Python scripts that originally imported data into MongoDB. The data has been migrated to PostgreSQL.
2. **Spring Boot application** (`src/`) — Kotlin/Spring Boot backend that serves election visualization data and provides runtime data enrichment via the Ariregister SOAP API

## Tech Stack

### Backend (Kotlin/Spring Boot)
- **Framework**: Spring Boot 3.5.6, Kotlin 2.2.20, Java 21
- **Database**: PostgreSQL (`election-vis` database) — used for both election data and monitoring data
- **Build**: Gradle (Kotlin DSL), `./gradlew bootRun`
- **Key dependencies**: Spring Data JDBC, Flyway, Jackson (Kotlin + XML), WebPush (VAPID), PostgreSQL driver
- **Server port**: 12345

### Data Import Pipeline (Python)
- **Runtime**: Python 3 with venv at `monitoring-import/venv/`
- **Dependencies**: `pymongo`, `requests`, `beautifulsoup4`, `psycopg2-binary` (see `monitoring-import/requirements.txt`)
- **Original database**: MongoDB at `mongodb://localhost:27017`, database `election-vis` (data migrated to PostgreSQL)

### Configuration
- `src/main/resources/application.yml` — Spring Boot config
- Environment variables: `ARIREGISTER_URL`, `ARIREGISTER_USERNAME`, `ARIREGISTER_PASSWORD`, `SPRING_DB_*`, `VAPID_*`
- `.envrc` — local env var overrides (direnv)

## PostgreSQL Schema (Monitoring)

### `monitoring_entry`

```kotlin
@Table("monitoring_entry")
data class MonitoringEntryEntity(
    @Id val id: Long? = null,
    val type: MonitoringEntryType,      // INDIVIDUAL, PARTY, GOV, GOV_KOV, BUSINESS, PUBLIC_BODY
    val name: String,                   // canonical display name
    val birthDate: LocalDate? = null,   // individuals only
    val extra: Jsonb? = null,           // contacts, photo, other unstructured data
)
```

- `extra` is a JSONB column using a custom `Jsonb` wrapper type with Spring Data JDBC converters
- `extra.contacts`: `[{"type": "phone|email|website|social|other", "value": "..."}]`
- `extra.photo`: URL string

### `monitoring_entry_external_id`

External IDs from various systems (extensible — new ID types need no schema migration):
- Types: `estGovId`, `ariregisterAnonId`, `erjkId`, `riigikoguGuid`
- UNIQUE constraint on `(type, value)` — no two entries can share the same external ID

### `monitoring_entry_name`

Historical/alternative names for entries. Separate table enables trigram fuzzy search via `pg_trgm`:
- `full_name`, `first_name`, `last_name` — for individuals
- `business_name`, `business_suffix` — for organizations
- `start_date`, `end_date` — for historical name ranges
- `is_primary` — marks the canonical/current name

### `monitoring_connection`

```kotlin
@Table("monitoring_connection")
data class MonitoringConnectionEntity(
    @Id val id: Long? = null,
    val type: MonitoringEntryConnectionType,  // PARTY_MEMBERSHIP, EMPLOYMENT, etc.
    val name: String,                         // human-readable role/relationship name
    val confirmed: Boolean = false,           // false if ambiguous match or data quality issue
    val entryAId: Long,                       // always set — one side of the relationship
    val entryBId: Long? = null,               // NULL when ambiguous (candidates in separate table)
    val parentId: Long? = null,               // ownership directionality (BUSINESS_OWNERSHIP only)
    val monetaryValue: BigDecimal? = null,    // for salary/donation amounts
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val sources: Jsonb? = null,               // ["url1", "url2", ...]
)
```

Connection types: `PARTY_MEMBERSHIP`, `BUSINESS_OWNERSHIP`, `EMPLOYMENT`, `ADMINISTRATION`, `DONATION`, `BANK_LOAN`, `UNKNOWN`

### `monitoring_connection_candidate`

When a connection is ambiguous (person couldn't be uniquely identified), `entry_b_id` is NULL and the candidate persons are stored here. Resolution: pick the correct candidate, set `entry_b_id`, delete candidates, set `confirmed = true`.

### `monitoring_entity_connections` (view)

Convenience view that unions a-side, b-side, and candidate connections so you can query "all connections for entity X" with a single query.

### Fuzzy Name Search

PostgreSQL extensions `pg_trgm` and `unaccent` are installed with GIN trigram indexes on name columns. Use for similarity search:

```sql
SELECT name, similarity(f_unaccent(name), f_unaccent('search term')) AS score
FROM monitoring_entry
WHERE f_unaccent(name) % f_unaccent('search term')
ORDER BY score DESC;
```

## Kotlin Architecture

### Package: `com.thatguyalex.monitoring`

```
monitoring/
├── application/
│   ├── MonitoringEntryLookupService.kt.bak  — shelved, pending refactor to JDBC
│   └── AriregisterSyncService.kt.bak        — shelved, pending refactor to JDBC
└── infrastructure/
    ├── MonitoringTypes.kt                    — shared enums (MonitoringEntryType, MonitoringEntryConnectionType)
    ├── jdbc/
    │   ├── Jsonb.kt                          — JSONB wrapper type + Spring Data converters
    │   ├── JdbcMonitoringConfiguration.kt    — registers JSONB converters
    │   ├── MonitoringEntryEntities.kt        — entry, external ID, name entity classes
    │   ├── MonitoringConnectionEntities.kt   — connection + candidate entity classes
    │   └── MonitoringRepositories.kt         — all Spring Data JDBC repositories
    └── rest/
        ├── AriregisterClient.kt              — SOAP HTTP client (RestTemplate + DOM XML parsing)
        ├── AriregisterMappings.kt            — static mapping tables (roles, contacts, legal forms)
        └── AriregisterProperties.kt          — @ConfigurationProperties for ariregister.*
```

### Shelved Services (pending refactor)

`MonitoringEntryLookupService.kt.bak` and `AriregisterSyncService.kt.bak` contain the original MongoDB-based implementations. They need to be rewritten to use the new JDBC repositories. Key query translations:

| Old (MongoDB) | New (JDBC) |
|---|---|
| `ids.estGovId` exact match | `MonitoringEntryExternalIdRepo.findEntriesByExternalId("estGovId", value)` |
| `altNames.firstName/lastName` regex | `MonitoringEntryNameRepo.findIndividualsByNameParts(first, last)` |
| `name` regex | `MonitoringEntryRepo.findIndividualByNameIgnoreCase(name)` |
| `connectedIds.all(ids)` + type + startDate | `MonitoringConnectionRepo.findByPairAndTypeAndStartDate(a, b, type, date)` |

### AriregisterMappings

Static mapping constants:
- `ARIREGISTER_LEGAL_FORM_TYPE_MAPPINGS` — legal form code -> `MonitoringEntryType` (KOVAS->GOV_KOV, TRAS/AVOIG->GOV, ERAK->PARTY, SA->PUBLIC_BODY, default BUSINESS)
- `ARIREGISTER_ROLE_MAPPINGS` — 57 role codes -> `MonitoringEntryConnectionType?` (BUSINESS_OWNERSHIP, ADMINISTRATION, EMPLOYMENT, or null to skip)
- `ARIREGISTER_CONTACT_TYPE_MAPPINGS` — 9 contact codes -> type string (phone, email, website, other)
- `ARIREGISTER_LEGAL_FORM_SUFFIXES` — set of legal form codes and names for stripping from business names

### Package: `com.thatguyalex.rk2023`

The election visualization app — handles election results processing, caching, push notifications, and REST API endpoints. Not directly related to the monitoring/import pipeline.

## Data Import Pipeline

All scripts live in `monitoring-import/` with a shared Python venv. These scripts originally wrote to MongoDB. The data has been migrated to PostgreSQL via `9_postgres_migration/migrate_to_postgres.py`.

```bash
cd monitoring-import
source venv/bin/activate
python <folder>/<script>.py
```

### Common Library (`common/person_lookup.py`)

Shared functions used by multiple import scripts (MongoDB-based):
- `find_or_create_person(entries_col, full_name, birthdate, first_name, last_name)` — case-insensitive lookup with birthdate disambiguation
- `find_or_create_org(entries_col, name, est_gov_id)` — org lookup excluding INDIVIDUAL entries
- Names are trimmed but casing is preserved (no `.title()` transformation)

### 1_parties — Current Party Members

**Source**: Ariregister CSV downloads for 14 active parties
**Creates**: PARTY entries, INDIVIDUAL entries, PARTY_MEMBERSHIP connections
**Result**: 14 parties, ~47K members, ~47K connections
**Docs**: `1_parties/import_parties.md`

### 2_gov_bodies — Government Body Hierarchy

**Source**: Ministry of Finance XLSX (converted to CSV), split into KOV and non-KOV
**Creates**: GOV, GOV_KOV, BUSINESS, PUBLIC_BODY entries with BUSINESS_OWNERSHIP hierarchy connections
**Manual preprocessing**: CSV splitting, parent name corrections, casing fixes
**Result**: 2221 entries, 2048 connections
**Docs**: `2_gov_bodies/import_gov_bodies.md`

### 3_salary_import — Public Sector Salaries

**Source**: Annual XLSX files from fin.ee (2015-2024), merged into per-year CSVs
**Creates**: INDIVIDUAL entries, EMPLOYMENT connections with monetary values
**Features**: Org name aliasing (100+ aliases), "overworked" detection (overlapping periods), robust period parsing
**Result**: ~250K connections across 10 years
**Docs**: `3_salary_import/import_salaries.md`

### 4_party_contributions — ERJK Donations & Fees

**Source**: ERJK API (`erjk.ee/api/quarterly-reports/`)
**Creates**: INDIVIDUAL/BUSINESS donor entries, DONATION/PARTY_MEMBERSHIP/BANK_LOAN connections
**Features**: Donor classification by birthdate format (digits-only = org registrikood)
**Docs**: `4_party_contributions/import_contributions.md`

### 5_parliament_membership — Riigikogu Members

**Source**: Riigikogu API (`api.riigikogu.ee`), 20MB votings.json dump
**Pipeline**: 4 sequential scripts (extract_attendance_votings -> fetch_membership_timeline -> enrich_member_details -> import_parliament_members)
**Creates**: INDIVIDUAL entries with contacts/photo, ADMINISTRATION connections to "Riigikogu Kantselei"
**Features**: Weekly attendance check sampling, membership timeline construction, profile enrichment (birthdate, contacts, photo), 15 req/min throttling, resumable progress
**Result**: ~494 unique members with historical spans
**Docs**: `5_parliament_membership/import_parliament_membership.md`
**Run all**: `python 5_parliament_membership/run_all.py`

### 6_party_legacy — Historical Party Memberships

**Source**: Ariregister HTML pages (`ariregister.rik.ee/est/political_party/member_history/{id}`)
**Creates**: PARTY_MEMBERSHIP connections for historical (including defunct) parties
**Features**: HTML scraping with BeautifulSoup, 60 req/min throttling, progress tracking, skipped IDs logging
**ID ranges**: ~95K pages across 3 blocks (defined in `ariregister_blocks.txt`)
**Result**: Historical memberships supplementing step 1's current data
**Docs**: `6_party_legacy/import_legacy_memberships.md`

### 7_name_corrections — Data Cleanup

**No documentation file**
- `broken_names.json` — records with malformed names + `corrected_name` field
- `apply_corrections.py` — renames entries, merges duplicates, updates connections
- `needs_review.json` — entries requiring manual disambiguation

### 8_government_ids — Ariregister Integration Prep

**No documentation file**
- `migrate_alt_names.py` — one-time migration: `nameParts` -> `altNames` array with `fullName`
- `unique_last_names.txt` — all unique last names from DB (case-merged, sorted by length)
- `ariregister_classifiers.xml` — full classifier dump from Ariregister API (role codes, legal forms, contact types, etc.)
- `example_details.xml` — example SOAP response for `detailandmed_v2`

### 9_postgres_migration — MongoDB to PostgreSQL Migration

**No documentation file**
- `migrate_to_postgres.py` — one-time migration script: reads all data from MongoDB `monitoring_entries` and `monitoring_entry_connections`, writes to PostgreSQL monitoring tables
- Handles ObjectId-to-BIGINT mapping, altNames/nameParts conversion, connectedIds splitting into entry_a/entry_b/candidates

## Current Work

The monitoring data has been migrated from MongoDB to PostgreSQL. The JDBC infrastructure (entities, repositories, JSONB converters) is in place. Two services still need refactoring:

1. **MonitoringEntryLookupService** — unified entity lookup/create with disambiguation (shelved as `.kt.bak`)
2. **AriregisterSyncService** — Ariregister SOAP sync orchestration (shelved as `.kt.bak`)

These need to be rewritten to use the new JDBC repositories instead of MongoTemplate.

## Important Conventions

- **Names**: Trimmed but casing preserved — never use `.title()` or other case transformations
- **Connections**: Always between exactly 2 entities when confirmed. `entry_a_id` and `entry_b_id` are symmetric (no enforced convention for which side is which). `confirmed=false` can mean ambiguous match OR data quality issue (e.g., "overworked" salary entries)
- **parentId**: Only set on `BUSINESS_OWNERSHIP` connections, pointing to the parent/owner entity
- **JSONB columns**: Use the `Jsonb` wrapper type in Kotlin entities — plain `String` will fail with PostgreSQL type mismatch
- **Enums**: Stored as TEXT in PostgreSQL, defined as Kotlin enums in `MonitoringTypes.kt`
- **Python venv**: Located at `monitoring-import/venv/`
- **Database name**: `election-vis`
