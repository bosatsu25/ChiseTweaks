#!/usr/bin/env python3
"""Run the repository-only audits shared by local development and CI."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
AUDITS = (
    ROOT / "scripts" / "source_identity_audit.py",
    ROOT / "scripts" / "pre_java25_audit.py",
    ROOT / "scripts" / "ci_contract_audit.py",
)


def run(command: list[str]) -> None:
    print("+", " ".join(command), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)


def main() -> int:
    for audit in AUDITS:
        run([sys.executable, str(audit)])
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except subprocess.CalledProcessError as exc:
        raise SystemExit(exc.returncode) from exc
