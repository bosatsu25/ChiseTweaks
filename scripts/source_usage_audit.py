#!/usr/bin/env python3
"""Detect orphaned production Java sources while respecting Fabric reflective entry points."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN_JAVA = ROOT / "src" / "main" / "java"
RESOURCES = ROOT / "src" / "main" / "resources"

# Anchor type detection to a Java declaration line. The previous word-boundary expression could
# mistake prose such as "This class owns ..." inside Javadoc for a top-level declaration.
DECLARATION = re.compile(
    r"(?m)^[ \t]*(?:public[ \t]+)?"
    r"(?:(?:final|abstract|sealed|non-sealed)[ \t]+)?"
    r"(?:class|interface|enum|record)[ \t]+([A-Za-z_$][A-Za-z0-9_$]*)\b"
)


def class_name(path: Path) -> str:
    source = path.read_text(encoding="utf-8")
    match = DECLARATION.search(source)
    if not match:
        raise RuntimeError(f"No top-level Java declaration found: {path.relative_to(ROOT)}")
    return match.group(1)


def fqcn_for(path: Path) -> str:
    relative = path.relative_to(MAIN_JAVA).with_suffix("")
    return ".".join(relative.parts)


def read_json(path: Path) -> dict:
    if not path.is_file():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def fabric_reflective_classes() -> set[str]:
    result: set[str] = set()
    metadata = read_json(RESOURCES / "fabric.mod.json")
    for entries in metadata.get("entrypoints", {}).values():
        for entry in entries if isinstance(entries, list) else [entries]:
            if isinstance(entry, str):
                result.add(entry.split("::", 1)[0])
            elif isinstance(entry, dict):
                value = entry.get("value")
                if isinstance(value, str):
                    result.add(value.split("::", 1)[0])

    for mixin_config in metadata.get("mixins", []):
        config_name = mixin_config if isinstance(mixin_config, str) else mixin_config.get("config")
        if not isinstance(config_name, str):
            continue
        config = read_json(RESOURCES / config_name)
        package = config.get("package", "")
        plugin = config.get("plugin")
        if isinstance(plugin, str) and plugin.strip():
            result.add(plugin.strip())
        for section in ("mixins", "client", "server"):
            for item in config.get(section, []):
                if isinstance(item, str):
                    result.add(f"{package}.{item}" if package else item)
    return result


def count_external_references(name: str, owner: Path, sources: list[Path]) -> int:
    token = re.compile(rf"\b{re.escape(name)}\b")
    count = 0
    for path in sources:
        if path == owner:
            continue
        count += len(token.findall(path.read_text(encoding="utf-8")))
    return count


def main() -> int:
    try:
        sources = sorted(MAIN_JAVA.rglob("*.java"))
        reflective = fabric_reflective_classes()
        candidates: list[str] = []

        for path in sources:
            relative = path.relative_to(ROOT).as_posix()
            fqcn = fqcn_for(path)
            name = class_name(path)
            if "/api/" in f"/{relative}":
                continue
            if fqcn in reflective:
                continue
            if count_external_references(name, path, sources) == 0:
                candidates.append(relative)

        if candidates:
            print("SOURCE USAGE AUDIT: FAIL", file=sys.stderr)
            print("Unreferenced production Java sources:", file=sys.stderr)
            for candidate in candidates:
                print(f"- {candidate}", file=sys.stderr)
            return 1

        print("SOURCE USAGE AUDIT: PASS")
        print(f"production_java_sources={len(sources)}")
        print(f"fabric_reflective_classes={len(reflective)}")
        print("unreferenced_production_sources=0")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, RuntimeError) as error:
        print(f"SOURCE USAGE AUDIT: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
