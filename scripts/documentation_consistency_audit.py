#!/usr/bin/env python3
"""Keep the two-document ownership model and runtime metadata synchronized."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

from versioning_core import read_properties

ROOT = Path(__file__).resolve().parents[1]

RETIRED_DOCUMENTS = (
    "AUDIT_2026-08-23.md",
    "CI_ENVIRONMENT.md",
    "TEST_DESIGN_JSTQB.md",
    "VERSIONING.md",
    "docs/CI_ACCEPTANCE.md",
    "docs/CI_FIX_NOTES.md",
    "docs/CI_JAR_POLICY.md",
    "docs/CI_PIT_SCOPE.md",
    "docs/CI_QUALITY_MODEL.md",
    "docs/CI_RELEASE_FLOW.md",
    "docs/CI_SCOPE_SUMMARY.md",
    "docs/acceptance/0.9.4-prism.md",
    "docs/performance/0.9.4-baseline.md",
)


def require(pattern: str, text: str, label: str, failures: list[str]) -> None:
    if re.search(pattern, text, flags=re.MULTILINE) is None:
        failures.append(label)


def main() -> int:
    failures: list[str] = []
    props = read_properties(ROOT / "gradle.properties")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    development = (ROOT / "DEVELOPMENT.md").read_text(encoding="utf-8")
    fabric = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))

    minecraft = props["minecraft_version"]
    loader = props["loader_version"]
    fabric_api = props["fabric_api_version"]
    jar_goal = props["runtime_jar_target_bytes"]
    jar_max = props["runtime_jar_max_bytes"]

    require(r"^# ChiseTweaks$", readme, "README must remain the user-facing product document", failures)
    require(
        rf"^\| Minecraft \| `{re.escape(minecraft)}` \|$",
        readme,
        f"README Minecraft row must be {minecraft}",
        failures,
    )
    require(
        rf"^\| Fabric Loader \| `{re.escape(loader)}` 以上 \|$",
        readme,
        f"README Fabric Loader row must be {loader} or newer",
        failures,
    )
    require(
        rf"^\| Fabric API \| `{re.escape(fabric_api)}` 以上 \|$",
        readme,
        f"README Fabric API row must be {fabric_api} or newer",
        failures,
    )
    require(r"^\| Java \| `25` 以上 \|$", readme, "README Java row must require Java 25 or newer", failures)
    require(
        r"chise-tweaks-<version>\.jar",
        readme,
        "README must document the version-derived runtime JAR naming contract",
        failures,
    )
    require(
        r"\[`DEVELOPMENT\.md`\]\(DEVELOPMENT\.md\)",
        readme,
        "README must link to DEVELOPMENT.md",
        failures,
    )
    require(
        r"14個のON/OFF機能",
        readme,
        "README must explain the fourteen-toggle scope in beginner-facing language",
        failures,
    )
    require(
        r"### Air Placement",
        readme,
        "README must document Air Placement usage and boundaries",
        failures,
    )
    require(
        r"Bright系は専用PNGを持ちません",
        readme,
        "README must explain the texture-free Bright rendering architecture",
        failures,
    )
    require(
        r"Resource Pack selection / reloadへ依存しない",
        development,
        "DEVELOPMENT.md must document Bright non-pack rendering ownership",
        failures,
    )

    for marker in (
        "python scripts/bump_version.py patch",
        "python scripts/bump_version.py minor",
        "exact CI-verified runtime JAR",
        f"`{jar_goal} bytes`",
        f"`{jar_max} bytes`",
        "toggle可能なruntime featureは現在14個",
        "Air Placement",
        "GitHub Issues",
    ):
        if marker not in development:
            failures.append(f"DEVELOPMENT.md is missing current contract marker: {marker}")

    for relative in RETIRED_DOCUMENTS:
        if (ROOT / relative).exists():
            failures.append(f"retired duplicate documentation returned: {relative}")

    if fabric.get("version") != "${version}":
        failures.append("fabric.mod.json version must remain ${version}")
    depends = fabric.get("depends", {})
    expected_depends = {
        "minecraft": "${minecraft_version}",
        "fabricloader": ">=${loader_version}",
        "fabric-api": ">=${fabric_api_version}",
        "java": ">=25",
    }
    for key, expected in expected_depends.items():
        if depends.get(key) != expected:
            failures.append(f"fabric.mod.json dependency {key} must be {expected}")

    if failures:
        print("DOCUMENTATION CONSISTENCY AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print("DOCUMENTATION CONSISTENCY AUDIT: PASS")
    print("user_doc=README.md")
    print("development_doc=DEVELOPMENT.md")
    print("retired_duplicate_docs=false")
    print(f"minecraft={minecraft}")
    print(f"runtime_jar_goal={jar_goal}")
    print(f"runtime_jar_max={jar_max}")
    print("metadata_placeholders=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
