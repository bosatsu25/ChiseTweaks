#!/usr/bin/env python3
"""Fail CI when optimization silently changes the reviewed ChiseTweaks runtime contract."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def load_json(path: str) -> Any:
    return json.loads(read(path))


def camel_case(feature_id: str) -> str:
    head, *tail = feature_id.split("_")
    return head + "".join(part[:1].upper() + part[1:] for part in tail)


def enum_string_map(text: str) -> dict[str, str]:
    return dict(re.findall(
        r"^\s*([A-Z][A-Z0-9_]*)\(\s*(?:\r?\n\s*)?\"([^\"]+)\"",
        text,
        re.MULTILINE,
    ))


def int_constants(text: str) -> dict[str, int]:
    return {
        name: int(value)
        for name, value in re.findall(
            r"public\s+static\s+final\s+int\s+([A-Z0-9_]+)\s*=\s*(-?\d+)\s*;",
            text,
        )
    }


def scalar(value: str, constants: dict[str, int]) -> Any:
    token = value.strip()
    if token == "true": return True
    if token == "false": return False
    if re.fullmatch(r"-?\d+", token): return int(token)
    return constants.get(token.rsplit(".", 1)[-1], token)


def diff(label: str, expected: Any, actual: Any, failures: list[str]) -> None:
    if expected != actual:
        failures.append(f"{label}: expected={expected!r}, actual={actual!r}")


def list_body(text: str, name: str) -> str:
    match = re.search(rf"\b{name}\s*=\s*List\.of\((.*?)\);", text, re.DOTALL)
    return match.group(1) if match else ""


def audit() -> list[str]:
    baseline = load_json("quality/functional-parity-baseline.json")
    failures: list[str] = []
    if not baseline.get("baseline", {}).get("version") or not baseline.get("baseline", {}).get("commit"):
        failures.append("functional parity baseline provenance is incomplete")

    definition_text = read("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java")
    definition_map = enum_string_map(definition_text)
    diff("feature ids", baseline["featureIds"], list(definition_map.values()), failures)

    switches = read("src/main/java/dev/chise/chisetweaks/config/FeatureSwitches.java")
    feature_body = list_body(switches, "FEATURE_CONFIG_VALUES")
    feature_constants = re.findall(r"\b([A-Z][A-Z0-9_]+)\b", feature_body)
    global_settings = {
        camel_case(definition_map[name]): False
        for name in feature_constants if name in definition_map
    }
    diff("global feature switches", baseline["settings"]["globalFeatureSwitches"], global_settings, failures)

    local_pairs = re.findall(
        r"public\s+static\s+final\s+FeatureSwitch\s+[A-Z0-9_]+\s*=\s*local\(\s*"
        r"FeatureDefinition\.[A-Z0-9_]+\s*,\s*\"([^\"]+)\"\s*,\s*(true|false)",
        switches,
        re.DOTALL,
    )
    local_switches = {name: default == "true" for name, default in local_pairs}
    diff("local feature switches", baseline["settings"]["localFeatureSwitches"], local_switches, failures)

    values_body = list_body(switches, "VALUES")
    value_constants = re.findall(r"\b([A-Z][A-Z0-9_]+)\b", values_body)
    if len(value_constants) != 14 or set(value_constants) != set(definition_map):
        failures.append("FeatureSwitches.VALUES must contain each of the 14 FeatureDefinition entries exactly once")

    profile_policy = read("src/main/java/dev/chise/chisetweaks/core/policy/WorksiteHighlightProfilePolicy.java")
    debris_policy = read("src/main/java/dev/chise/chisetweaks/core/policy/AncientDebrisAnalyzerPolicy.java")
    constants = int_constants(profile_policy) | int_constants(debris_policy)
    local_settings_text = read("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java")
    local_setting_pairs = re.findall(
        r"\b(?:bool|integer)\(\s*\"([^\"]+)\"\s*,\s*([^,\r\n]+)",
        local_settings_text,
        re.MULTILINE,
    )
    local_settings = {name: scalar(default, constants) for name, default in local_setting_pairs}
    diff("local setting defaults", baseline["settings"]["localSettings"], local_settings, failures)

    visual_targets_text = read("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java")
    visual_target_keys = re.findall(
        r"entry\(Target\.[A-Z0-9_]+\s*,\s*\"([^\"]+)\"", visual_targets_text, re.DOTALL)
    visual_targets = {name: True for name in visual_target_keys}
    if not re.search(r"new\s+SimpleBooleanSetting\(\s*configName\s*,\s*true\s*,", visual_targets_text):
        failures.append("visual target default is no longer true")
    diff("visual target defaults", baseline["settings"]["visualTargets"], visual_targets, failures)

    builder_text = read("src/main/java/dev/chise/chisetweaks/config/BuilderFocusConfig.java")
    builder: dict[str, Any] = {}
    for name, default in re.findall(
        r"new\s+SimpleBooleanSetting\(\s*\"([^\"]+)\"\s*,\s*(true|false)", builder_text, re.DOTALL):
        builder[name] = default == "true"
    for name, mode in re.findall(
        r"new\s+ChiseRuleModeSetting\(\s*\"([^\"]+)\"\s*,\s*ChiseRuleMode\.([A-Z_]+)", builder_text, re.DOTALL):
        builder[name] = mode
    for name in re.findall(
        r"new\s+ChiseStringListSetting\(\s*\"([^\"]+)\"\s*,\s*List\.of\(\)", builder_text, re.DOTALL):
        builder[name] = []
    diff("Builder Focus defaults", baseline["settings"]["builderFocus"], builder, failures)

    pack_root = ROOT / "src/main/resources/resourcepacks"
    pack_ids = [] if not pack_root.exists() else sorted(
        f"chisetweaks:{path.name}" for path in pack_root.iterdir() if path.is_dir())
    diff("built-in resource pack ids", baseline["resourcePackIds"], pack_ids, failures)

    for removed in (
        "src/main/java/dev/chise/chisetweaks/feature/resource",
        "src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitches.java",
        "src/main/java/dev/chise/chisetweaks/config/ChestVisibilitySetting.java",
        "src/main/java/dev/chise/chisetweaks/config/WhiteConcreteVisibilitySetting.java",
    ):
        if (ROOT / removed).exists():
            failures.append(f"obsolete visual feature foundation returned: {removed}")

    mixins = load_json("src/main/resources/chisetweaks.features.mixins.json")
    diff("Mixin list", baseline["mixins"], mixins.get("client", []), failures)
    if mixins.get("required") is not False:
        failures.append("Mixin config required must remain false")
    if mixins.get("injectors", {}).get("defaultRequire") != 0:
        failures.append("Mixin defaultRequire must remain 0")

    fabric = load_json("src/main/resources/fabric.mod.json")
    fabric_expected = baseline["fabric"]
    diff("Fabric environment", fabric_expected["environment"], fabric.get("environment"), failures)
    diff("Fabric entrypoints", fabric_expected["entrypoints"], fabric.get("entrypoints"), failures)
    diff("Fabric dependencies", fabric_expected["depends"], fabric.get("depends"), failures)
    diff("Fabric client-only custom contract", fabric_expected["custom"],
         fabric.get("custom", {}).get("chisetweaks"), failures)

    row_definition = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingRowDefinition.java")
    action_match = re.search(r"enum\s+Action\s*\{(.*?)\n\s*\}", row_definition, re.DOTALL)
    actions = re.findall(r"\b([A-Z][A-Z0-9_]+)\b", action_match.group(1)) if action_match else []
    diff("UI actions", baseline["uiActions"], actions, failures)

    diagnostic_text = read("src/main/java/dev/chise/chisetweaks/runtime/RuntimeDiagnosticEvent.java")
    diff("diagnostic events", baseline["diagnosticEvents"], list(enum_string_map(diagnostic_text).values()), failures)

    missing_fixtures = [path for path in baseline["migrationFixtures"] if not (ROOT / path).is_file()]
    if missing_fixtures:
        failures.append(f"migration fixtures missing: {missing_fixtures}")

    analyzer_constants = int_constants(debris_policy)
    analyzer_budget = {name: analyzer_constants.get(name) for name in baseline["analyzerBudget"]}
    diff("Ancient Debris analyzer budget", baseline["analyzerBudget"], analyzer_budget, failures)
    return failures


def main() -> int:
    try:
        failures = audit()
    except (OSError, KeyError, ValueError, json.JSONDecodeError) as failure:
        print(f"FUNCTIONAL PARITY AUDIT: FAIL: {failure}", file=sys.stderr)
        return 1
    if failures:
        print("FUNCTIONAL PARITY AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        print("Update the baseline only for an intentional, reviewed product-contract change.", file=sys.stderr)
        return 1
    baseline = load_json("quality/functional-parity-baseline.json")["baseline"]
    print("FUNCTIONAL PARITY AUDIT: PASS")
    print(f"baseline_version={baseline['version']}")
    print(f"baseline_runtime_jar_bytes={baseline['runtimeJarBytes']}")
    print(f"goal_runtime_jar_bytes={baseline['goalBytes']}")
    print("functional_contract_change=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
