# Common Lookup Library

Shared functions for looking up and creating persons and organizations in MongoDB.

## `find_or_create_person(entries_col, full_name, birthdate=None)`

Looks up a person by full name (case-insensitive) in `monitoring_entries`. Uses birthdate for disambiguation when provided, but never as a search filter — only name is queried.

**Returns**: `(person_ids: list[ObjectId], created: bool, updated_birthdate: bool)`

### Resolution logic

| Found | Birthdate provided | DB has birthdate | Match? | Action |
|---|---|---|---|---|
| 0 | any | — | — | Create new record |
| 1 | no | any | — | Return it |
| 1 | yes | no | — | Update DB with birthdate, return it |
| 1 | yes | yes | yes | Return it |
| 1 | yes | yes | no | Create new record (different person) |
| N | no | any | — | Return all |
| N | yes | mixed | any match | Return only matching |
| N | yes | mixed | none match | Return those without birthdate |
| N | yes | all have BD | none match | Create new record |

### Person document structure
```json
{
  "ids": {"estGovId": null, "ariregisterAnonId": null, "erjkId": null},
  "name": "Title Cased Name",
  "type": "INDIVIDUAL",
  "nameParts": {"firstName": null, "lastName": null, "businessName": null, "businessSuffix": null},
  "birthDate": "<datetime or null>"
}
```

## `find_or_create_org(entries_col, name, est_gov_id)`

Looks up an organization by `ids.estGovId` first, then by name (case-insensitive, excluding INDIVIDUAL entries). Creates as `type=BUSINESS` if not found. Raises `RuntimeError` if multiple entries match at any step.

**Returns**: `org_id: ObjectId`
