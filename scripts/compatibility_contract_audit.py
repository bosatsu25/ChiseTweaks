#!/usr/bin/env python3
"""Reject optional-renderer coupling and deterministic runtime performance regressions."""
from __future__ import annotations

import json
import sys
from pathlib import Path

import runtime_performance_contract_audit

ROOT = Path(__file__).resolve().parents[1]
OPTIONAL_RENDERER_MODS = {"sodium", "iris", "immediatelyfast", "entityculling"}
FORBIDDEN_IMPLEMENTATION_TOKENS = (
    "me.jellysquid.mods.sodium",
    "net.caffeinemc.mods.sodium",
    "net.irisshaders",
    "immediatelyfast",
    "entityculling",
)
FORBIDDEN_METADATA_RELATIONS = ("depends", "breaks", "conflicts")
EXPECTED_MIXIN_PLUGIN = "dev.chise.chisetweaks.mixin.FeatureAvailabilityMixinConfigPlugin"
EXPECTED_INTEGRATION_MIXIN_PLUGIN = "dev.chise.chisetweaks.mixin.IntegrationMixinConfigPlugin"


def relation_mod_ids(value: object) -> set[str]:
    if isinstance(value, dict):
        return {str(key).lower() for key in value}
    if isinstance(value, list):
        return {str(item).lower() for item in value}
    return set()


def scan_resource_namespaces(failures: list[str]) -> None:
    resources = ROOT / "src/main/resources"
    for path in resources.rglob("*.json"):
        if path.name == "fabric.mod.json":
            continue
        text = path.read_text(encoding="utf-8").lower()
        relative = path.relative_to(ROOT)
        for token in FORBIDDEN_IMPLEMENTATION_TOKENS:
            if token.lower() in text:
                failures.append(
                    f"{relative}: renderer implementation namespace leaked into resource metadata: {token}"
                )


def main() -> int:
    failures: list[str] = []
    fabric = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))

    if fabric.get("environment") != "client":
        failures.append("ChiseTweaks must remain client-only")

    for relation in FORBIDDEN_METADATA_RELATIONS:
        renderer_relations = sorted(relation_mod_ids(fabric.get(relation, {})) & OPTIONAL_RENDERER_MODS)
        if renderer_relations:
            failures.append(
                f"optional renderer mods became {relation} relationships: {renderer_relations}"
            )

    recommends = relation_mod_ids(fabric.get("recommends", {}))
    if "sodium" not in recommends:
        failures.append("Sodium should remain an optional recommendation")

    mixin_path = ROOT / "src/main/resources/chisetweaks.features.mixins.json"
    mixin = json.loads(mixin_path.read_text(encoding="utf-8"))
    if mixin.get("required") is not False:
        failures.append("feature mixin config must remain fail-soft with required=false")
    if mixin.get("plugin") != EXPECTED_MIXIN_PLUGIN:
        failures.append(f"feature mixin plugin must be {EXPECTED_MIXIN_PLUGIN}")

    integration_mixin_path = ROOT / "src/main/resources/chisetweaks.integrations.mixins.json"
    integration_mixin = json.loads(integration_mixin_path.read_text(encoding="utf-8"))
    if integration_mixin.get("required") is not False:
        failures.append("integration mixin config must remain fail-soft with required=false")
    if integration_mixin.get("injectors", {}).get("defaultRequire") != 0:
        failures.append("integration mixin config defaultRequire must remain 0")
    if integration_mixin.get("plugin") != EXPECTED_INTEGRATION_MIXIN_PLUGIN:
        failures.append(
            f"integration mixin plugin must be {EXPECTED_INTEGRATION_MIXIN_PLUGIN}")

    production_root = ROOT / "src/main/java"
    for path in production_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8").lower()
        relative = path.relative_to(ROOT)
        for token in FORBIDDEN_IMPLEMENTATION_TOKENS:
            if token.lower() in text:
                failures.append(f"{relative}: hard renderer implementation reference detected: {token}")

    scan_resource_namespaces(failures)
    failures.extend(
        f"runtime-performance: {failure}"
        for failure in runtime_performance_contract_audit.audit()
    )

    if failures:
        print("COMPATIBILITY / RUNTIME CONTRACT AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print("COMPATIBILITY / RUNTIME CONTRACT AUDIT: PASS")
    print("optional_renderer_hard_dependencies=false")
    print("optional_renderer_conflicts=false")
    print("resource_namespace_coupling=false")
    print("mixin_fail_soft=true")
    print("canonical_mixin_plugin=true")
    print("integration_mixin_fail_soft=true")
    print("blocking_runtime_regressions=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
