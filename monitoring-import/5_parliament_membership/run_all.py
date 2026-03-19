import subprocess
import sys
import os

SCRIPTS = [
    "extract_attendance_votings.py",
    "fetch_membership_timeline.py",
    "enrich_member_details.py",
    "import_parliament_members.py",
]

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))

    for i, script in enumerate(SCRIPTS, 1):
        print(f"\n=== Step {i}: {script} ===\n")
        result = subprocess.run(
            [sys.executable, script],
            cwd=script_dir,
        )
        if result.returncode != 0:
            print(f"\nERROR: {script} failed with exit code {result.returncode}")
            sys.exit(result.returncode)

    print("\n=== All steps completed ===")


if __name__ == "__main__":
    main()
