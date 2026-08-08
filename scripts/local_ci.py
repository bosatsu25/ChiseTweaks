#!/usr/bin/env python3
"""Run the same build and post-build verification used by GitHub Actions."""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPTS = ROOT / "scripts"


def run(command: list[str]) -> None:
    print("+", " ".join(command), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)


def gradle_command() -> list[str]:
    if os.name == "nt":
        return [str(ROOT / "gradlew.bat")]
    return [str(ROOT / "gradlew")]


def main() -> int:
    run(gradle_command() + ["--no-daemon", "--stacktrace", "clean", "qualityGate", "build"])
    run([sys.executable, str(SCRIPTS / "quality_summary.py")])
    run([sys.executable, str(SCRIPTS / "artifact_audit.py")])
    print("LOCAL CI: PASS", flush=True)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except subprocess.CalledProcessError as exc:
        raise SystemExit(exc.returncode) from exc
