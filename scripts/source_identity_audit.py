#!/usr/bin/env python3
"""Reject stale external-project identity tokens from repository text files."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {".git", ".gradle", "build", "out", "run", "runs"}
BINARY_SUFFIXES = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".jar", ".class", ".zip"}

# Numeric construction keeps the audit rule itself free of the tokens it rejects.
def token(*codepoints: int) -> bytes:
    return bytes(codepoints)

LONG_TOKENS = (
    token(116, 97, 105, 99, 104, 105),
    token(97, 109, 97, 116, 101, 114, 97, 115),
    token(97, 115, 116, 114, 97, 108),
)
SHORT_TOKENS = (
    token(97, 109, 97),
    token(97, 115, 116),
)


def is_text_candidate(path: Path) -> bool:
    if path.suffix.lower() in BINARY_SUFFIXES:
        return False
    return not any(part in SKIP_DIRS for part in path.relative_to(ROOT).parts)


def contains_stale_identity(data: bytes) -> bool:
    lowered = data.lower()
    if any(value in lowered for value in LONG_TOKENS):
        return True
    for value in SHORT_TOKENS:
        pattern = rb"(?<![a-z0-9_])" + re.escape(value) + rb"(?![a-z0-9_])"
        if re.search(pattern, lowered):
            return True
    return False


failures: list[str] = []
for path in sorted(ROOT.rglob("*")):
    if not path.is_file() or not is_text_candidate(path):
        continue
    try:
        data = path.read_bytes()
    except OSError as exc:
        failures.append(f"cannot read {path.relative_to(ROOT)}: {exc}")
        continue
    if contains_stale_identity(data):
        failures.append(str(path.relative_to(ROOT)))

if failures:
    print("SOURCE IDENTITY AUDIT: FAIL")
    for item in failures:
        print(f" - stale external-project identity token: {item}")
    sys.exit(1)

print("SOURCE IDENTITY AUDIT: PASS")
