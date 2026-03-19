# Import Parliament Membership Data

**Source**: Riigikogu Open API (`https://api.riigikogu.ee/api/`)

**Input**: `votings.json` — full dump of all parliamentary sessions and their votings since January 1998 (~20MB, ~713K lines).

Fetched with:
```bash
curl -X 'GET' \
  'https://api.riigikogu.ee/api/votings?startDate=1992-01-01&endDate=2026-04-01&lang=ET' \
  -H 'accept: application/json' \
  -o votings.json
```

## Pipeline overview

The import runs in 4 sequential steps:

1. **Extract attendance votings** — filter weekly attendance checks from the full votings dump
2. **Fetch membership timeline** — call the API for each voting to build per-member time spans
3. **Enrich member details** — fetch individual member profiles for dateOfBirth, contacts, photo
4. **Import to MongoDB** — look up/create persons and insert ADMINISTRATION connections

## Step 1: Extract attendance votings

**Script**: `extract_attendance_votings.py`

**Input**: `votings.json`
**Output**: `attendance_votings.json`

Scans all sessions for votings with `type.code == "KOHALOLEKU_KONTROLL"`. When one is found, records it and skips to the next Monday (based on the voting's local `startDateTime`, not the session's UTC `sittingDateTime`). This produces one attendance check per week.

Result: ~790 entries from August 2000 to present, primarily on Mondays. Gaps of 10–12 weeks each summer correspond to parliament's recess (mid-June to early September).

Each entry contains: `date`, `datetime`, `votingUrl`, `uuid`, `sessionTitle`, `present`, `absent`.

## Step 2: Fetch membership timeline

**Script**: `fetch_membership_timeline.py`

**Input**: `attendance_votings.json`
**Output**: `parliament_members.json`

For each attendance voting, fetches the full voting record from `https://api.riigikogu.ee/api/votings/{uuid}`. The response includes a `voters[]` array listing all 101 parliament members (both present and absent — the decision code is ignored).

**Timeline construction**: processes votings chronologically, tracking which UUIDs appeared in the previous voting:
- New UUID → create member with a new span `{startDate, endDate}`
- UUID in previous voting → extend last span's `endDate`
- UUID seen before but not in previous voting → start a new span (member returned after absence)

**Rate limiting**: 15 requests/minute (4s interval). On any error, retries after 5s indefinitely. Progress saved after each voting to `parliament_members_progress.json` for resume support.

Result: ~494 unique members with multiple time spans each.

## Step 3: Enrich member details

**Script**: `enrich_member_details.py`

**Input/Output**: `parliament_members.json` (updated in-place)

For each member, fetches `https://api.riigikogu.ee/api/plenary-members/{uuid}` and extracts:
- `dateOfBirth`
- `contacts[]`: phone, email, website (type=`phone`/`email`/`website`), social media URLs (type=`social`)
- `photo`: `photoBig._links.download.href`

404 responses are not retried — these are historical members whose profiles have been removed. They are logged separately with their latest span end date.

**Rate limiting**: 15 requests/minute. Progress saved to `enrich_progress.json`; deleted on successful completion.

## Step 4: Import to MongoDB

**Script**: `import_parliament_members.py`

**Input**: `parliament_members.json`
**Target**: MongoDB `election-vis` database

### Pre-lookup
Finds the "Riigikogu Kantselei" entry in `monitoring_entries` and caches its ObjectId. Aborts if not found.

### Person lookup
Uses `common/person_lookup.py` with `first_name` + `last_name` parameters:
1. Try `nameParts.firstName` + `nameParts.lastName` (case-insensitive)
2. Fall back to full name match
3. Birthdate used for disambiguation per standard logic

### Data updates (single match only)
If exactly one person is found/created, updates the entry with (only if not already set):
- `ids.riigikoguGuid` = member's Riigikogu UUID
- `birthDate`
- `unstructuredData.contacts` — array of `{type, value}`
- `unstructuredData.photo` — download URL
- `nameParts.firstName` / `nameParts.lastName`

If multiple persons match, no data updates are performed.

### Connections
For every member (including multi-match), creates one `monitoring_entry_connections` document per time span:
- `connectedIds`: person ID(s) + Riigikogu Kantselei ID
- `name`: "Liige"
- `type`: `ADMINISTRATION`
- `confirmed`: `true` if single person match, `false` if multiple
- `startDate` / `endDate` from span
- `sources`: `[{sourceUrl: "https://api.riigikogu.ee/api/plenary-members/{uuid}"}]`

## Running

```bash
source ../venv/bin/activate
python run_all.py
```

Or run individual steps:

```bash
python extract_attendance_votings.py    # Step 1: ~1 second
python fetch_membership_timeline.py      # Step 2: ~53 minutes (790 API calls)
python enrich_member_details.py          # Step 3: ~33 minutes (494 API calls)
python import_parliament_members.py      # Step 4: ~seconds (local DB)
```

Steps 2 and 3 are resumable — kill and restart safely.
