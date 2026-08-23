#!/usr/bin/env python3
"""Reject hard renderer-mod coupling that would break optional compatibility."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OPTIONAL_RENDERER_MODS = {"sodium", "iris", "immediatelyfast", "entityculling"}
FORBIDDEN_IMPLEMENTATION_TOKENS = (
    "me.jellysquid.mods.sodium",
    "net.caffeinemc.mods.sodium",
    "net.irisshaders",
    "immediatelyfast",
    "entityculling",
)
EXPECTED_MIXIN_PLUGIN = "dev.chise.chisetweaks.mixin.FeatureAvailabilityMixinConfigPlugin"


def main() -> int:
    failures: list[str] = []
    fabric = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text(encoding="utf-8"))
    hard_depends = set(fabric.get("depends", {}))
    hard_renderer_dependencies = sorted(hard_depends & OPTIONAL_RENDERER_MODS)
    if hard_renderer_dependencies:
        failures.append(f"optional renderer mods became hard dependencies: {hard_renderer_dependencies}")

    recommends = fabric.get("recommends", {})
    if "sodium" not in recommends:
        failures.append("Sodium should remain an optional recommendation")

    mixin_path = ROOT / "src/main/resources/chisetweaks.features.mixins.json"
    mixin = json.loads(mixin_path.read_text(encoding="utf-8"))
    if mixin.get("required") is not False:
        failures.append("feature mixin config must remain fail-soft with required=false")
    if mixin.get("plugin") != EXPECTED_MIXIN_PLUGIN:
        failures.append(f"feature mixin plugin must be {EXPECTED_MIXIN_PLUGIN}")

    production_root = ROOT / "src/main/java"
    for path in production_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8").lower()
        relative = path.relative_to(ROOT)
        for token in FORBIDDEN_IMPLEMENTATION_TOKENS:
            if token.lower() in text:
                failures.append(f"{relative}: hard renderer implementation reference detected: {token}")

    if failures:
        print("COMPATIBILITY CONTRACT AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1

    print("COMPATIBILITY CONTRACT AUDIT: PASS")
    print("optional_renderer_hard_dependencies=false")
    print("mixin_fail_soft=true")
    print("canonical_mixin_plugin=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
