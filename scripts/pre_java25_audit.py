#!/usr/bin/env python3
"""Source-only release audit that does not require Gradle or a Java 25 runtime."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java"
RESOURCES = ROOT / "src/main/resources"

failures: list[str] = []
warnings: list[str] = []

java_files = list(MAIN.rglob("*.java"))
text_by_file = {path: path.read_text(encoding="utf-8") for path in java_files}
all_text = "\n".join(text_by_file.values())

for path, text in text_by_file.items():
    relative = path.relative_to(ROOT)
    for marker in ("TODO", "FIXME", "HACK"):
        if re.search(rf"\\b{marker}\\b", text):
            failures.append(f"{relative}: contains {marker}")
    if re.search(r"catch\s*\(\s*(?:Exception|Throwable)\b", text):
        failures.append(f"{relative}: catches Exception/Throwable directly")
    if re.search(r"catch\s*\([^)]*\)\s*\{\s*\}", text, re.S):
        failures.append(f"{relative}: contains an empty catch block")

if "WorksiteVisibilityFeature" in all_text:
    failures.append("legacy WorksiteVisibilityFeature reference remains")
if "getWorksiteVisibilityFeature" in all_text:
    failures.append("legacy getWorksiteVisibilityFeature reference remains")

mixin_path = RESOURCES / "chisetweaks.features.mixins.json"
try:
    mixin = json.loads(mixin_path.read_text(encoding="utf-8"))
except Exception as exc:  # audit script, not production code
    failures.append(f"cannot parse {mixin_path.relative_to(ROOT)}: {exc}")
else:
    clients = set(mixin.get("client", []))
    required = {
        "building.PumpkinScaffoldInputMixin",
        "rendering.BuilderFocusBlockMixin",
        "rendering.BuilderFocusEntityMixin",
    }
    missing = sorted(required - clients)
    if missing:
        failures.append(f"missing client mixins: {missing}")
    if mixin.get("compatibilityLevel") != "JAVA_25":
        failures.append("feature mixin compatibilityLevel is not JAVA_25")
    if mixin.get("required") is not False:
        failures.append("feature mixin config is no longer fail-soft (required=false)")
    if mixin.get("injectors", {}).get("defaultRequire") != 0:
        failures.append("feature mixin config no longer uses defaultRequire=0")

pumpkin_mixin = text_by_file.get(
    MAIN / "dev/chise/chisetweaks/mixin/building/PumpkinScaffoldInputMixin.java", ""
)
for method_name in ('method = "startUseItem"', 'method = "tick"'):
    if method_name not in pumpkin_mixin:
        failures.append(f"Pumpkin Scaffold mixin lost injection target {method_name}")

# Java 25 contracts must agree across Gradle, Fabric metadata and Mixin configs.
build_gradle = (ROOT / "build.gradle").read_text(encoding="utf-8")
if "options.release = 25" not in build_gradle:
    failures.append("build.gradle does not compile with --release 25")
fabric_mod = json.loads((RESOURCES / "fabric.mod.json").read_text(encoding="utf-8"))
if fabric_mod.get("depends", {}).get("java") != ">=25":
    failures.append("fabric.mod.json Java dependency is not >=25")


# Config persistence must have exactly one security/storage implementation.
legacy_storage = (
    MAIN / "dev/chise/chisetweaks/config/ChiseConfigStorage.java",
    MAIN / "dev/chise/chisetweaks/util/SafeFileStorage.java",
)
for legacy in legacy_storage:
    if legacy.exists():
        failures.append(f"legacy config storage implementation remains: {legacy.relative_to(ROOT)}")
if "ChiseConfigStorage" in all_text or "SafeFileStorage" in all_text:
    failures.append("production code still references a legacy config storage implementation")
secure_storage = MAIN / "dev/chise/chisetweaks/core/security/SecureConfigStorage.java"
if not secure_storage.exists():
    failures.append("SecureConfigStorage is missing")
for config_source in (
    MAIN / "dev/chise/chisetweaks/config/FeatureConfig.java",
    MAIN / "dev/chise/chisetweaks/config/LocalFeatureConfig.java",
):
    config_text = text_by_file.get(config_source, "")
    if "SecureConfigStorage" not in config_text:
        failures.append(f"{config_source.relative_to(ROOT)} bypasses SecureConfigStorage")
    if "FabricLoader.getInstance().getConfigDir()" not in config_text:
        failures.append(f"{config_source.relative_to(ROOT)} does not use the shared Fabric config root")
if "dev.chise.chisetweaks.core.security.SecureConfigStorage" not in build_gradle:
    failures.append("PIT configuration does not include SecureConfigStorage")

# Runtime isolation must guard both isActive() and tick().
feature_manager = text_by_file.get(
    MAIN / "dev/chise/chisetweaks/runtime/FeatureManager.java", ""
)
active_call = feature_manager.find("boolean active = component.isActive();")
if active_call < 0 or feature_manager.rfind("try {", max(0, active_call - 160), active_call) < 0:
    failures.append("FeatureManager isActive() is no longer inside the failure-isolation try block")

# Worksite caches may be block-type keyed, never world-position keyed.
worksite_text = "\n".join(
    text for path, text in text_by_file.items()
    if "/feature/rendering/worksite/" in path.as_posix()
)
if re.search(r"Map\s*<\s*(?:BlockPos|Vec3)", worksite_text):
    failures.append("worksite cache is keyed by world position/vector and may grow with exploration")
for forbidden in ("String.format(", ".stream()"):
    if forbidden in worksite_text:
        warnings.append(f"worksite hot path contains {forbidden}")

# Event-driven Pumpkin Scaffold must not become a ticking feature.
pumpkin = text_by_file.get(
    MAIN / "dev/chise/chisetweaks/feature/building/PumpkinScaffoldFeature.java", ""
)
if "implements TickingFeature" in pumpkin or "void tick(" in pumpkin:
    failures.append("Pumpkin Scaffold unexpectedly owns a tick loop")

# Worksite scans must remain bounded and loaded-chunk only.
scanner = text_by_file.get(
    MAIN / "dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java", ""
)
for required_snippet in (
    "MutableBlockPos",
    "PriorityQueue<ScanCandidate>",
    "MAX_SCAN_CANDIDATES",
    "hasChunkAt(position)",
):
    if required_snippet not in scanner:
        failures.append(f"WorksiteScanner lost required bound: {required_snippet}")
if "origin.offset(" in scanner:
    failures.append("WorksiteScanner reintroduced per-position origin.offset allocations")

# User-facing feature IDs must remain centralized in FeatureDefinition.
definition_path = MAIN / "dev/chise/chisetweaks/core/definition/FeatureDefinition.java"
definition_text = definition_path.read_text(encoding="utf-8")
ids = re.findall(r'^\s*"([a-z][a-z0-9_]+)",\s*$', definition_text, re.M)
for feature_id in ids:
    count = sum(text.count(f'"{feature_id}"') for text in text_by_file.values())
    if count != 1:
        failures.append(f"feature id {feature_id!r} appears {count} times in production Java")

if failures:
    print("PRE-JAVA25 AUDIT: FAIL")
    for item in failures:
        print(f" - {item}")
    if warnings:
        print("Warnings:")
        for item in warnings:
            print(f" - {item}")
    sys.exit(1)

print(f"PRE-JAVA25 AUDIT: PASS ({len(java_files)} production Java files)")
