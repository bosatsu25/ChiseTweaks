#!/usr/bin/env python3
"""Classify pull-request file changes for CI budget-aware execution.

Scopes:
- docs-only: user/developer documentation only
- tooling-only: docs plus GitHub workflow/audit/quality tooling only
- full: any runtime/build/test/config/resource change, mixed change, or unknown input

Only FULL may run the heavy Java/Gradle/PIT/GameTest/distribution gate.
The classifier fails closed.
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
TOOLING_ONLY_PREFIXES = (
    ".github/",
    "scripts/",
    "quality/",
)
FULL_EXACT = frozenset({
    "quality/risk-register.json",
})


def normalize_path(raw: str) -> str | None:
    path = raw.strip().replace("\\", "/")
    if not path or path.startswith("/") or "\x00" in path:
        return None
    parts = path.split("/")
    if any(part in {"", ".", ".."} for part in parts):
        return None
    return path


def is_docs_only_path(raw: str) -> bool:
    path = normalize_path(raw)
    if path is None:
        return False
    if path in DOC_ONLY_EXACT:
        return True
    return any(path.startswith(prefix) for prefix in DOC_ONLY_PREFIXES)


def is_tooling_only_path(raw: str) -> bool:
    path = normalize_path(raw)
    if path is None or path in FULL_EXACT:
        return False
    if is_docs_only_path(path):
        return True
    return any(path.startswith(prefix) for prefix in TOOLING_ONLY_PREFIXES)


def classify(paths: list[str]) -> str:
    normalized = [path.strip() for path in paths if path.strip()]
    if not normalized:
        return "full"
    if all(is_docs_only_path(path) for path in normalized):
        return "docs-only"
    if all(is_tooling_only_path(path) for path in normalized):
        return "tooling-only"
    return "full"


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
