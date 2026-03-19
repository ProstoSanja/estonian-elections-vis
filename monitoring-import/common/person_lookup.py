import re
from datetime import datetime


def find_or_create_person(entries_col, full_name, birthdate=None):
    """
    Look up a person by full name (case-insensitive). Create if not found.
    When birthdate is provided, uses it to disambiguate among multiple matches.

    Lookup is always by name only. Birthdate is used for disambiguation:
    - 0 found: create new record with birthdate
    - 1 found, no birthdate in DB, birthdate provided: update record, return it
    - 1 found, has birthdate, matches: return it
    - 1 found, has birthdate, doesn't match: create new record
    - Multiple found, one+ matches birthdate: return only matching
    - Multiple found, none match, some have no birthdate: return those without birthdate
    - Multiple found, none match, all have different birthdates: create new record

    Returns: (person_ids: list[ObjectId], created: bool, updated_birthdate: bool)
    """
    name_title = full_name.strip().title()

    found = list(entries_col.find(
        {"type": "INDIVIDUAL", "name": {"$regex": f"^{re.escape(name_title)}$", "$options": "i"}},
        {"_id": 1, "birthDate": 1},
    ))

    if len(found) == 0:
        return _create_person(entries_col, name_title, birthdate), True, False

    if not birthdate:
        return [d["_id"] for d in found], False, False

    if len(found) == 1:
        rec = found[0]
        if not rec.get("birthDate"):
            entries_col.update_one({"_id": rec["_id"]}, {"$set": {"birthDate": birthdate}})
            return [rec["_id"]], False, True
        if _dates_equal(rec["birthDate"], birthdate):
            return [rec["_id"]], False, False
        return _create_person(entries_col, name_title, birthdate), True, False

    bd_match = [d for d in found if _dates_equal(d.get("birthDate"), birthdate)]
    if bd_match:
        return [d["_id"] for d in bd_match], False, False

    no_bd = [d for d in found if not d.get("birthDate")]
    if no_bd:
        return [d["_id"] for d in no_bd], False, False

    return _create_person(entries_col, name_title, birthdate), True, False


def find_or_create_org(entries_col, name, est_gov_id):
    """
    Look up an org by estGovId first, then by name (case-insensitive).
    Creates as type=BUSINESS if not found. Raises if multiple found.

    Returns: org_id: ObjectId
    """
    if est_gov_id:
        found = list(entries_col.find(
            {"ids.estGovId": est_gov_id},
            {"_id": 1, "name": 1},
        ))
        if len(found) == 1:
            return found[0]["_id"]
        if len(found) > 1:
            names = [d["name"] for d in found]
            raise RuntimeError(f"Multiple orgs found for estGovId={est_gov_id}: {names}")

    found = list(entries_col.find(
        {"type": {"$ne": "INDIVIDUAL"}, "name": {"$regex": f"^{re.escape(name.strip())}$", "$options": "i"}},
        {"_id": 1, "name": 1},
    ))
    if len(found) == 1:
        return found[0]["_id"]
    if len(found) > 1:
        names = [d["name"] for d in found]
        raise RuntimeError(f"Multiple orgs found for name '{name}': {names}")

    doc = {
        "ids": {"estGovId": est_gov_id, "ariregisterAnonId": None, "erjkId": None},
        "name": name.strip(),
        "type": "BUSINESS",
        "nameParts": {
            "firstName": None,
            "lastName": None,
            "businessName": name.strip(),
            "businessSuffix": None,
        },
        "birthDate": None,
    }
    result = entries_col.insert_one(doc)
    return result.inserted_id


def _create_person(entries_col, name_title, birthdate):
    doc = {
        "ids": {"estGovId": None, "ariregisterAnonId": None, "erjkId": None},
        "name": name_title,
        "type": "INDIVIDUAL",
        "nameParts": {
            "firstName": None,
            "lastName": None,
            "businessName": None,
            "businessSuffix": None,
        },
        "birthDate": birthdate,
    }
    result = entries_col.insert_one(doc)
    return [result.inserted_id]


def _to_date(d):
    if d is None:
        return None
    if isinstance(d, datetime):
        return d.date()
    if hasattr(d, "date") and callable(d.date):
        return d.date()
    return d


def _dates_equal(db_date, query_date):
    return _to_date(db_date) == _to_date(query_date) if db_date and query_date else False
