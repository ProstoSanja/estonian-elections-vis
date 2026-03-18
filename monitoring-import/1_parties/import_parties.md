# Import Parties

**Source**: Live download from `https://ariregister.rik.ee/est/political_party/members/{code}?download=CSV` for 14 hardcoded parties.

**CSV format**: Semicolon-delimited, columns: `Eesnimi;Perekonnanimi;Sünniaeg;Liikmeks astumise aeg;Erakondliku kuuluvuse peatamine`. Encoding: UTF-8 with Latin-1 fallback.

## Process

For each of 14 parties:

1. Insert party into `monitoring_entries`: `type=PARTY`, `estGovId=registrikood`, `nameParts.businessName=party name`.
2. Download member CSV.
3. For each member row:
   - Parse `Sünniaeg` and `Liikmeks astumise aeg` as `dd.MM.yyyy`. Skip if either is missing.
   - Insert person into `monitoring_entries`: `type=INDIVIDUAL`, `name="First Last"`, `nameParts.firstName/lastName`, `birthDate`. IDs left empty.
   - Insert `monitoring_entry_connections`: `connectedIds=[person, party]`, `name="Liige"`, `type=PARTY_MEMBERSHIP`, `confirmed=true`, `startDate` from CSV, `sources=[CSV download URL]`.

## Result

- 14 parties, 47,230 members, 47,230 connections.
