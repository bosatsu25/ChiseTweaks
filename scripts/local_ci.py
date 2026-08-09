#!/usr/bin/env python3
"""Run the rebuilt retained-scope verification locally using the same gates as GitHub Actions."""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def run(*command: str) -> None:
    print("+", " ".join(command), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)


def main() -> int:
    python = sys.executable
    gradle = "gradlew.bat" if os.name == "nt" else "./gradlew"
    try:
        run(python, "scripts/repository_audit.py")
        run(gradle, "--no-daemon", "--stacktrace", "clean", "qualityGate", "build")
        run(python, "scripts/quality_summary.py")
        run(python, "scripts/artifact_audit.py")
    except subprocess.CalledProcessError as error:
        print(f"LOCAL CI: FAIL (exit {error.returncode})", file=sys.stderr)
        return error.returncode or 1
    print("LOCAL CI: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
