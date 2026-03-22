"""
Migrate nameParts -> altNames on all monitoring_entries documents.

For each document, creates an altNames array with a single entry containing:
  - fullName: copied from root `name`
  - firstName, lastName, businessName, businessSuffix: copied from `nameParts`
Only non-null nameParts fields are included.
"""

from pymongo import MongoClient

client = MongoClient("mongodb://localhost:27017")
db = client["election-vis"]
entries = db["monitoring_entries"]

total = entries.count_documents({})
print(f"Total documents: {total}")

already_migrated = entries.count_documents({"altNames": {"$exists": True, "$ne": []}})
print(f"Already migrated (altNames non-empty): {already_migrated}")

to_migrate = entries.count_documents({"$or": [{"altNames": {"$exists": False}}, {"altNames": []}]})
print(f"To migrate: {to_migrate}")

if to_migrate == 0:
    print("Nothing to migrate.")
    exit(0)

result = entries.update_many(
    {"$or": [{"altNames": {"$exists": False}}, {"altNames": []}]},
    [
        {
            "$set": {
                "altNames": [
                    {
                        "$mergeObjects": [
                            {
                                "$arrayToObject": {
                                    "$filter": {
                                        "input": {"$objectToArray": "$nameParts"},
                                        "cond": {"$ne": ["$$this.v", None]}
                                    }
                                }
                            },
                            {"fullName": "$name"}
                        ]
                    }
                ]
            }
        },
        {
            "$unset": "nameParts"
        }
    ],
)

print(f"Matched: {result.matched_count}, Modified: {result.modified_count}")
print("Migration complete.")
