import json
import re
import sys
import os

from bson import ObjectId
from pymongo import MongoClient

CORRECTIONS_FILE = "broken_names.json"
REVIEW_FILE = "needs_review.json"

MONGO_URI = "mongodb://localhost:27017"
DB_NAME = "election-vis"

ID_FIELDS = [
    "ids.estGovId",
    "ids.ariregisterAnonId",
    "ids.erjkId",
    "ids.riigikoguGuid",
]


def get_nested(doc, dotted_key):
    parts = dotted_key.split(".")
    val = doc
    for p in parts:
        if not isinstance(val, dict):
            return None
        val = val.get(p)
    return val


def main():
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    entries = db["monitoring_entries"]
    connections = db["monitoring_entry_connections"]

    with open(CORRECTIONS_FILE, "r", encoding="utf-8") as f:
        corrections = json.load(f)

    stats = {"renamed": 0, "merged": 0, "needs_review": 0, "not_found": 0}
    needs_review = []

    for item in corrections:
        broken_id = ObjectId(item["id"])
        old_name = item["name"]
        new_name = item["corrected_name"]

        broken = entries.find_one({"_id": broken_id})
        if not broken:
            print(f"  SKIP (not found): {old_name}")
            stats["not_found"] += 1
            continue

        candidates = list(entries.find(
            {
                "_id": {"$ne": broken_id},
                "type": "INDIVIDUAL",
                "name": {"$regex": f"^{re.escape(new_name)}$", "$options": "i"},
            },
        ))

        if len(candidates) == 0:
            entries.update_one(
                {"_id": broken_id},
                {"$set": {
                    "name": new_name,
                    "nameParts.firstName": None,
                    "nameParts.lastName": None,
                }},
            )
            print(f"  RENAMED: {old_name} -> {new_name}")
            stats["renamed"] += 1

        elif len(candidates) == 1:
            target = candidates[0]
            target_id = target["_id"]

            conflicts = []
            for field in ["birthDate"] + ID_FIELDS:
                broken_val = get_nested(broken, field)
                target_val = get_nested(target, field)
                if broken_val is not None and target_val is not None and broken_val != target_val:
                    conflicts.append({"field": field, "broken": str(broken_val), "target": str(target_val)})

            if conflicts:
                print(f"  REVIEW (conflicts): {old_name} -> {new_name}: {conflicts}")
                needs_review.append({
                    "id": item["id"],
                    "name": old_name,
                    "corrected_name": new_name,
                    "target_id": str(target_id),
                    "reason": "field_conflicts",
                    "conflicts": conflicts,
                })
                stats["needs_review"] += 1
                continue

            patch = {}
            if target.get("birthDate") is None and broken.get("birthDate") is not None:
                patch["birthDate"] = broken["birthDate"]
            broken_ids = broken.get("ids", {})
            target_ids = target.get("ids", {})
            for field in ID_FIELDS:
                key = field.split(".", 1)[1]
                if target_ids.get(key) is None and broken_ids.get(key) is not None:
                    patch[field] = broken_ids[key]

            if patch:
                entries.update_one({"_id": target_id}, {"$set": patch})

            conn_count = 0
            for conn in connections.find({"connectedIds": broken_id}):
                new_ids = [target_id if x == broken_id else x for x in conn["connectedIds"]]
                connections.update_one({"_id": conn["_id"]}, {"$set": {"connectedIds": new_ids}})
                conn_count += 1

            entries.delete_one({"_id": broken_id})
            patched_fields = list(patch.keys()) if patch else []
            print(f"  MERGED: {old_name} -> {new_name} (patched: {patched_fields}, connections: {conn_count})")
            stats["merged"] += 1

        else:
            print(f"  REVIEW (multiple): {old_name} -> {new_name} ({len(candidates)} candidates)")
            needs_review.append({
                "id": item["id"],
                "name": old_name,
                "corrected_name": new_name,
                "candidate_ids": [str(c["_id"]) for c in candidates],
                "reason": "multiple_candidates",
            })
            stats["needs_review"] += 1

    if needs_review:
        with open(REVIEW_FILE, "w", encoding="utf-8") as f:
            json.dump(needs_review, f, indent=2, ensure_ascii=False)

    client.close()

    print(f"\n--- Results ---")
    print(f"Renamed:      {stats['renamed']}")
    print(f"Merged:       {stats['merged']}")
    print(f"Needs review: {stats['needs_review']}")
    print(f"Not found:    {stats['not_found']}")
    if needs_review:
        print(f"\nSee {REVIEW_FILE} for entries requiring manual review.")


if __name__ == "__main__":
    main()
