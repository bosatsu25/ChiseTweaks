#!/usr/bin/env python3
"""Classify pull-request file changes for CI v2.

Only documentation-only changes may skip the heavy Java/Gradle/runtime gate.
All unknown, empty, infrastructure, source, config, test, workflow, and audit
changes fail closed to FULL.
"""
from __future__ import annotations

import argparse
from pathlib import Path

DOC_ONLY_EXACT = frozenset({
    "README.md",
    "DEVELOPMENT.md",
})
DOC_ONLY_PREFIXES = (
    "docs/",
)


def is_docs_only_path(raw: str) -> bool:
    path = raw.strip().replace("\\", "/")
    if not path or path.startswith("/") or "\x00" in path:
        return False
    parts = path.split("/")
    if any(part in {"", ".", ".."} for part in parts):
        return False
    if path in DOC_ONLY_EXACT:
        return True
    return any(path.startswith(prefix) for prefix in DOC_ONLY_PREFIXES)


def classify(paths: list[str]) -> str:
    normalized = [path.strip() for path in paths if path.strip()]
    if not normalized:
        return "full"
    return "docs-only" if all(is_docs_only_path(path) for path in normalized) else "full"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--file-list", type=Path, required=True)
    args = parser.parse_args()
    try:
        paths = args.file_list.read_text(encoding="utf-8").splitlines()
    except OSError:
        print("full")
        return 0
    print(classify(paths))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
