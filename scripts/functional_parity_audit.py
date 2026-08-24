#!/usr/bin/env python3
"""Fail CI when size/UI work silently changes the frozen 0.9.4 runtime contract."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
BASELINE_PATH = ROOT / "quality" / "functional-parity-baseline.json"


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
    if token == "true":
        return True
    if token == "false":
        return False
    if re.fullmatch(r"-?\d+", token):
        return int(token)
    name = token.rsplit(".", 1)[-1]
    if name in constants:
        return constants[name]
    return token


def diff(label: str, expected: Any, actual: Any, failures: list[str]) -> None:
    if expected != actual:
        failures.append(f"{label}: expected={expected!r}, actual={actual!r}")


def audit() -> list[str]:
    baseline = load_json("quality/functional-parity-baseline.json")
    failures: list[str] = []

    # baseline.version/commit are provenance only. A release-version bump must not be interpreted
    # as a feature-parity change; the actual product contract below remains frozen independently.
    if not baseline.get("baseline", {}).get("version") or not baseline.get("baseline", {}).get("commit"):
        failures.append("functional parity baseline provenance is incomplete")

    definition_text = read("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java")
    definition_map = enum_string_map(definition_text)
    feature_ids = list(definition_map.values())
    diff("feature ids", baseline["featureIds"], feature_ids, failures)

    feature_switches = read("src/main/java/dev/chise/chisetweaks/config/FeatureSwitches.java")
    feature_switch = read("src/main/java/dev/chise/chisetweaks/config/FeatureSwitch.java")
    global_constants = re.findall(r"FeatureDefinition\.([A-Z][A-Z0-9_]*)", feature_switches)
    global_constants = list(dict.fromkeys(global_constants))
    global_settings = {
        camel_case(definition_map[name]): False
        for name in global_constants
        if name in definition_map
    }
    if "DEFAULT_ENABLED = false" not in feature_switch:
        failures.append("global FeatureSwitch default is no longer false")
    diff("global feature switches", baseline["settings"]["globalFeatureSwitches"], global_settings, failures)

    local_switches_text = read("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitches.java")
    local_switch_text = read("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitch.java")
    local_keys = re.findall(
        r"new\s+LocalFeatureSwitch\(\s*FeatureDefinition\.[A-Z0-9_]+\s*,\s*\"([^\"]+)\"",
        local_switches_text,
        re.DOTALL,
    )
    local_switches = {name: False for name in local_keys}
    if "DEFAULT_ENABLED = false" not in local_switch_text:
        failures.append("local FeatureSwitch default is no longer false")
    diff("local feature switches", baseline["settings"]["localFeatureSwitches"], local_switches, failures)

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
        r"entry\(Target\.[A-Z0-9_]+\s*,\s*\"([^\"]+)\"",
        visual_targets_text,
        re.DOTALL,
    )
    visual_targets = {name: True for name in visual_target_keys}
    if not re.search(r"new\s+SimpleBooleanSetting\(\s*configName\s*,\s*true\s*,", visual_targets_text):
        failures.append("visual target default is no longer true")
    diff("visual target defaults", baseline["settings"]["visualTargets"], visual_targets, failures)

    builder_text = read("src/main/java/dev/chise/chisetweaks/config/BuilderFocusConfig.java")
    builder: dict[str, Any] = {}
    for name, default in re.findall(
        r"new\s+SimpleBooleanSetting\(\s*\"([^\"]+)\"\s*,\s*(true|false)",
        builder_text,
        re.DOTALL,
    ):
        builder[name] = default == "true"
    for name, mode in re.findall(
        r"new\s+ChiseRuleModeSetting\(\s*\"([^\"]+)\"\s*,\s*ChiseRuleMode\.([A-Z_]+)",
        builder_text,
        re.DOTALL,
    ):
        builder[name] = mode
    # Only the first two constructor arguments define the frozen default. Additional metadata
    # such as persistence domain must not make the parity audit depend on constructor shape.
    for name in re.findall(
        r"new\s+ChiseStringListSetting\(\s*\"([^\"]+)\"\s*,\s*List\.of\(\)",
        builder_text,
        re.DOTALL,
    ):
        builder[name] = []
    diff("Builder Focus defaults", baseline["settings"]["builderFocus"], builder, failures)

    visibility_settings: dict[str, bool] = {}
    for path in (
        "src/main/java/dev/chise/chisetweaks/config/ChestVisibilitySetting.java",
        "src/main/java/dev/chise/chisetweaks/config/WhiteConcreteVisibilitySetting.java",
    ):
        text = read(path)
        match = re.search(r"super\(\s*\"([^\"]+)\"\s*,\s*(true|false)", text, re.DOTALL)
        if match:
            visibility_settings[match.group(1)] = match.group(2) == "true"
    diff("visibility setting defaults", baseline["settings"]["visibilityPacks"], visibility_settings, failures)

    visibility_pack_text = read("src/main/java/dev/chise/chisetweaks/feature/resource/VisibilityPack.java")
    pack_paths = re.findall(r"^\s*[A-Z_]+\(\"([^\"]+)\"\s*,", visibility_pack_text, re.MULTILINE)
    pack_ids = [f"chisetweaks:{path}" for path in pack_paths]
    diff("built-in resource pack ids", baseline["resourcePackIds"], pack_ids, failures)

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
    diagnostic_events = list(enum_string_map(diagnostic_text).values())
    diff("diagnostic events", baseline["diagnosticEvents"], diagnostic_events, failures)

    missing_fixtures = [
        path for path in baseline["migrationFixtures"]
        if not (ROOT / path).is_file()
    ]
    if missing_fixtures:
        failures.append(f"migration fixtures missing: {missing_fixtures}")

    analyzer_constants = int_constants(debris_policy)
    analyzer_budget = {
        name: analyzer_constants.get(name)
        for name in baseline["analyzerBudget"]
    }
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
