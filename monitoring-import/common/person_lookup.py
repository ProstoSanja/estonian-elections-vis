import re
from datetime import datetime


def find_or_create_person(entries_col, full_name, birthdate=None,
                          first_name=None, last_name=None):
    """
    Look up a person by full name (case-insensitive). Create if not found.
    When birthdate is provided, uses it to disambiguate among multiple matches.

    If first_name and last_name are provided, tries nameParts lookup first,
    falling back to full name if no results.

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
    name_clean = full_name.strip()

    found = _lookup_by_name(entries_col, name_clean, first_name, last_name)

    if len(found) == 0:
        return _create_person(entries_col, name_clean, birthdate, first_name, last_name), True, False

    if not birthdate:
        return [d["_id"] for d in found], False, False

    if len(found) == 1:
        rec = found[0]
        if not rec.get("birthDate"):
            entries_col.update_one({"_id": rec["_id"]}, {"$set": {"birthDate": birthdate}})
            return [rec["_id"]], False, True
        if _dates_equal(rec["birthDate"], birthdate):
            return [rec["_id"]], False, False
        return _create_person(entries_col, name_clean, birthdate, first_name, last_name), True, False

    bd_match = [d for d in found if _dates_equal(d.get("birthDate"), birthdate)]
    if bd_match:
        return [d["_id"] for d in bd_match], False, False

    no_bd = [d for d in found if not d.get("birthDate")]
    if no_bd:
        return [d["_id"] for d in no_bd], False, False

    return _create_person(entries_col, name_clean, birthdate, first_name, last_name), True, False


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


def _lookup_by_name(entries_col, name_clean, first_name=None, last_name=None):
    """
    Try nameParts.firstName + nameParts.lastName first if provided,
    fall back to full name match.
    """
    if first_name and last_name:
        found = list(entries_col.find(
            {
                "type": "INDIVIDUAL",
                "nameParts.firstName": {"$regex": f"^{re.escape(first_name.strip())}$", "$options": "i"},
                "nameParts.lastName": {"$regex": f"^{re.escape(last_name.strip())}$", "$options": "i"},
            },
            {"_id": 1, "birthDate": 1},
        ))
        if found:
            return found

    return list(entries_col.find(
        {"type": "INDIVIDUAL", "name": {"$regex": f"^{re.escape(name_clean)}$", "$options": "i"}},
        {"_id": 1, "birthDate": 1},
    ))


def _create_person(entries_col, name_clean, birthdate, first_name=None, last_name=None):
    doc = {
        "ids": {"estGovId": None, "ariregisterAnonId": None, "erjkId": None},
        "name": name_clean,
        "type": "INDIVIDUAL",
        "nameParts": {
            "firstName": first_name.strip() if first_name else None,
            "lastName": last_name.strip() if last_name else None,
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
