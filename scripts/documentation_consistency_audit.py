#!/usr/bin/env python3
"""Keep current user/developer docs, runtime translations, and metadata synchronized."""
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
    "docs/warden-risk-analyzer-feasibility.md",
)

RETIRED_TRANSLATION_FRAGMENTS = (
    "airplacement",
    "air_placement",
    "localancientdebrisanalyzer",
    "ancient_debris_analyzer",
    "settings.debris_",
    "settings.section.ancient_debris",
)


def require(pattern: str, text: str, label: str, failures: list[str]) -> None:
    if re.search(pattern, text, flags=re.MULTILINE) is None:
        failures.append(label)


def audit_language(
        locale: str,
        values: dict[str, object],
        expected_subtitle: str,
        failures: list[str]) -> None:
    for key in values:
        normalized = key.lower()
        for retired in RETIRED_TRANSLATION_FRAGMENTS:
            if retired in normalized:
                failures.append(f"{locale} runtime translation still contains retired key: {key}")

    subtitle = values.get("screen.chisetweaks.help.subtitle")
    if subtitle != expected_subtitle:
        failures.append(f"{locale} help subtitle must describe the 16-toggle / 14-default-off scope")

    # These translation keys remain migration-compatible. Product-facing Lava/Hidden/Villager
    # names are now owned by FeatureDefinition/TweaksProductSettingsRows instead of legacy help copy.
    expected_names = {
        "config.name.localfirevisibility": "Low Fire",
        "config.name.materialhighlights": "Ore Highlights",
        "config.name.handheldsize": "Handheld Size",
    }
    for key, expected in expected_names.items():
        if values.get(key) != expected:
            failures.append(f"{locale} canonical retained name drift: {key} must be {expected}")


def main() -> int:
    failures: list[str] = []
    props = read_properties(ROOT / "gradle.properties")
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    development = (ROOT / "DEVELOPMENT.md").read_text(encoding="utf-8")
    fabric = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    en_us = json.loads((ROOT / "src/main/resources/assets/chisetweaks/lang/en_us.json").read_text(encoding="utf-8"))
    ja_jp = json.loads((ROOT / "src/main/resources/assets/chisetweaks/lang/ja_jp.json").read_text(encoding="utf-8"))

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
    require(r"chise-tweaks-<version>\.jar", readme,
            "README must document the version-derived runtime JAR naming contract", failures)
    require(r"\[`DEVELOPMENT\.md`\]\(DEVELOPMENT\.md\)", readme,
            "README must link to DEVELOPMENT.md", failures)
    require(r"16個のON/OFF可能なruntime機能", readme,
            "README must explain the current sixteen-toggle scope", failures)

    for product_group in (
        "Visual Tweaks",
        "Builder Highlights",
        "Technical Visualization",
        "Scene Filter",
        "Builder Assist",
        "Integrations",
    ):
        if product_group not in readme:
            failures.append(f"README is missing Tweaks product group: {product_group}")

    for retained_visual_name in (
        "Lava Source Highlight",
        "Hidden Material Highlight",
        "Villager Job Site Links",
    ):
        if retained_visual_name not in readme:
            failures.append(f"README is missing visualization name: {retained_visual_name}")

    require(r"Bright系はResource Pack切替を持ちません", readme,
            "README must explain Bright rendering without resource-pack switching", failures)
    require(r"White Concrete spriteをChestへ貼らない", readme,
            "README must preserve the dedicated Bright Chest texture contract", failures)
    require(r"Resource Pack selection / reloadへ依存しない", development,
            "DEVELOPMENT.md must document Bright non-pack rendering ownership", failures)
    require(r"Handheld Size.*Resource Pack", development,
            "DEVELOPMENT.md must document Handheld Size as code-based resource-pack-preserving rendering", failures)

    for retired_heading in (
        "### Air Placement",
        "### Ancient Debris Analyzer",
        "### Warden Risk Analyzer",
    ):
        if retired_heading in readme:
            failures.append(f"README still documents retired feature: {retired_heading[4:]}")

    for marker in (
        "python scripts/bump_version.py patch",
        "python scripts/bump_version.py minor",
        "exact CI-verified runtime JAR",
        f"`{jar_goal} bytes`",
        f"`{jar_max} bytes`",
        "toggle可能なruntime featureは現在16個",
        "GitHub Issues",
        "scripts/ci_scope.py",
        "scripts/ci_provenance.py",
        "tooling-only",
        "tree SHA",
        "release / Publish verified runtime JAR",
        "CHISE_CI_RUNS_ON",
        "workflow_dispatch",
    ):
        if marker not in development:
            failures.append(f"DEVELOPMENT.md is missing current contract marker: {marker}")

    for retired_marker in ("Air Placement", "Ancient Debris Analyzer", "Warden Risk Analyzer"):
        if retired_marker in development:
            failures.append(f"DEVELOPMENT.md still treats retired feature as active: {retired_marker}")

    audit_language(
        "en_us",
        en_us,
        "Review 16 toggleable runtime features. Bright Chest / Bright Concrete default ON; the other 14 default OFF.",
        failures,
    )
    audit_language(
        "ja_jp",
        ja_jp,
        "16個の切り替え可能なruntime機能を確認できます。Bright Chest / Bright Concreteは初期ON、ほか14機能は初期OFFです。",
        failures,
    )

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

    description = str(fabric.get("description", ""))
    if "tweaks" not in description.lower() or "builders" not in description.lower():
        failures.append("fabric.mod.json description must identify the builder Tweaks product scope")

    if failures:
        print("DOCUMENTATION CONSISTENCY AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print("DOCUMENTATION CONSISTENCY AUDIT: PASS")
    print("user_doc=README.md")
    print("development_doc=DEVELOPMENT.md")
    print("runtime_translation_scope=16_retained_features")
    print("product_groups=6")
    print("retired_translation_residue=false")
    print("retired_duplicate_docs=false")
    print(f"minecraft={minecraft}")
    print(f"runtime_jar_goal={jar_goal}")
    print(f"runtime_jar_max={jar_max}")
    print("metadata_placeholders=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
