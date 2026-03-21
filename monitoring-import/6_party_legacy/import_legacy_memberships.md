# Import Legacy Party Memberships

**Source**: Ariregister member history pages at `https://ariregister.rik.ee/est/political_party/member_history/{id}`

These pages contain historical party membership records for individuals, including memberships in parties that have since been removed from the registry. This supplements the current member lists (imported in step 1) with historical data.

## ID Ranges

Defined in `ariregister_blocks.txt` as 3 blocks (start/end URL pairs separated by blank lines):

| Block | Start | End | Count |
|-------|-------|-----|-------|
| 1 | 2000000172 | 2000006719 | ~6,548 |
| 2 | 2000035228 | 2000079225 | ~43,998 |
| 3 | 9000000001 | 9000044665 | ~44,665 |
| **Total** | | | **~95,211** |

Every ID in these ranges is expected to have data. If a page loads without person data, the script halts.

## Page Structure

HTML pages (server-rendered, no JS required). Parsed with `requests` + `BeautifulSoup`.

- **Person**: `h2` heading containing `Name (DD.MM.YYYY)`
- **Memberships table**: columns Nimetus (party name), Registrikood, Erakonna olek, Liitus (joined), Lahkus (left)

## Process

### Startup
- Pre-fetches all `type=PARTY` entries from MongoDB into a dict keyed by `ids.estGovId` (registrikood) for fast lookup.
- Loads progress from `import_progress.json` to support resuming.

### Per page
1. Fetch HTML (60 req/min). On error: exponential backoff starting at 5s, +5s per failure, resets on success.
2. Parse person name + birthdate and membership rows.
3. Validate: halt if name/birthdate missing or no membership rows.
4. Look up person via `common/person_lookup.py` with name + birthdate. Halt if multiple matches.
5. Set `ids.ariregisterAnonId` on the person entry (the numeric page ID) if not already set.
6. For each membership row:
   - Look up party by registrikood from cache. Halt if not found.
   - Check if `PARTY_MEMBERSHIP` connection already exists (matched by person+party+startDate).
   - Insert new connection if not found.
7. Save progress.

### Connection fields
- `connectedIds`: [person_id, party_id]
- `name`: "Liige"
- `type`: PARTY_MEMBERSHIP
- `confirmed`: true
- `startDate`: joined date
- `endDate`: left date (null if still active)
- `sources`: [{sourceUrl: page URL}]

## Error handling

The script halts on:
- Page with no person name or birthdate
- Multiple person matches in DB
- Party registrikood not found in cache

HTTP errors are retried with exponential backoff (5s, 10s, 15s, ...), never skipped.

## Running

```bash
source ../venv/bin/activate
pip install beautifulsoup4  # if not yet installed
python import_legacy_memberships.py
```

Resumable: kill and restart safely. Progress is saved after each page in `import_progress.json`. At 60 req/min, full run takes ~26 hours.
