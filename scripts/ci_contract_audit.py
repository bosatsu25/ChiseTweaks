#!/usr/bin/env python3
"""Fail-fast source/resource/metadata audit for ChiseTweaks CI.

This audit deliberately avoids Gradle and Minecraft class loading. It validates
repository invariants that should fail a pull request before an expensive build.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java"
RESOURCES = ROOT / "src/main/resources"
LANG = RESOURCES / "assets/chisetweaks/lang"

failures: list[str] = []
warnings: list[str] = []


def fail(message: str) -> None:
    failures.append(message)


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        fail(f"cannot parse {path.relative_to(ROOT)}: {exc}")
        return {}


def read_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            fail(f"{path.relative_to(ROOT)}: malformed property line: {raw!r}")
            continue
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()
    return props


# Required legal/provenance files.
for name in ("LICENSE", "LICENSE_MIT", "LICENSE_APACHE-2.0", "NOTICE"):
    path = ROOT / name
    if not path.is_file() or not path.read_text(encoding="utf-8").strip():
        fail(f"required distribution notice is missing or empty: {name}")

properties = read_properties(ROOT / "gradle.properties")
provenance_status = properties.get("provenance_status")
if provenance_status not in {"unresolved", "resolved"}:
    fail("gradle.properties: provenance_status must be 'unresolved' or 'resolved'")

# Language resources must be exact-key mirrors with nonblank values.
en = load_json(LANG / "en_us.json")
ja = load_json(LANG / "ja_jp.json")
if set(en) != set(ja):
    missing_ja = sorted(set(en) - set(ja))
    missing_en = sorted(set(ja) - set(en))
    if missing_ja:
        fail(f"ja_jp.json is missing keys: {missing_ja}")
    if missing_en:
        fail(f"en_us.json is missing keys: {missing_en}")
for locale, values in (("en_us", en), ("ja_jp", ja)):
    for key, value in values.items():
        if not isinstance(value, str) or not value.strip():
            fail(f"{locale}.json has a blank/non-string value for {key}")

# Literal translatable keys in production Java must exist in both languages.
java_files = list(MAIN.rglob("*.java"))
text_by_file = {path: path.read_text(encoding="utf-8") for path in java_files}
for path, text in text_by_file.items():
    for match in re.finditer(r'(?:translatable|translate)\(\s*"([a-z0-9_.-]+)"', text):
        key = match.group(1)
        if key.startswith("chisetweaks.") or key.startswith("config.") or key.startswith("help."):
            if key not in en:
                fail(f"{path.relative_to(ROOT)} references missing translation key: {key}")

fabric_mod_path = RESOURCES / "fabric.mod.json"
fabric = load_json(fabric_mod_path)
props = read_properties(ROOT / "gradle.properties")

# Version/dependency metadata must remain sourced from gradle.properties.
expected_metadata = {
    ("version",): "${version}",
    ("depends", "minecraft"): "${minecraft_version}",
    ("depends", "fabricloader"): ">=${loader_version}",
    ("depends", "fabric-api"): ">=${fabric_api_version}",
    ("depends", "malilib"): ">=${malilib_version}",
    ("recommends", "modmenu"): ">=${modmenu_version}",
    ("recommends", "sodium"): ">=${sodium_compat_version}",
}
for path_tuple, expected in expected_metadata.items():
    value = fabric
    for segment in path_tuple:
        value = value.get(segment, {}) if isinstance(value, dict) else None
    if value != expected:
        fail(f"fabric.mod.json {'.'.join(path_tuple)} must be {expected!r}, got {value!r}")
for key in (
    "mod_version", "minecraft_version", "loader_version", "fabric_api_version",
    "malilib_version", "modmenu_version", "sodium_compat_version", "jacoco_version",
):
    if not props.get(key):
        fail(f"gradle.properties is missing required CI metadata: {key}")
if fabric.get("depends", {}).get("java") != ">=25":
    fail("fabric.mod.json Java dependency must remain >=25")

# Dynamic Mixin audit: every *Mixin.java is owned by the most-specific
# configured package, must be registered exactly once, and every JSON entry
# must resolve to a source file.
mixin_configs = fabric.get("mixins", [])
if not isinstance(mixin_configs, list) or not mixin_configs:
    fail("fabric.mod.json has no mixin configs")
    mixin_configs = []
resource_mixin_files = {p.name for p in RESOURCES.glob("*.mixins.json")}
configured_mixin_files = set(mixin_configs)
if resource_mixin_files != configured_mixin_files:
    extra = sorted(resource_mixin_files - configured_mixin_files)
    missing = sorted(configured_mixin_files - resource_mixin_files)
    if extra:
        fail(f"unreferenced mixin JSON files: {extra}")
    if missing:
        fail(f"fabric.mod.json references missing mixin JSON files: {missing}")

config_data: dict[str, dict] = {}
package_to_config: dict[str, str] = {}
for config_name in mixin_configs:
    config = load_json(RESOURCES / config_name)
    config_data[config_name] = config
    package = config.get("package")
    if not isinstance(package, str) or not package:
        fail(f"{config_name}: missing package")
        continue
    if package in package_to_config:
        fail(f"Mixin package is owned by multiple configs: {package}")
    package_to_config[package] = config_name
    if config.get("compatibilityLevel") != "JAVA_25":
        fail(f"{config_name}: compatibilityLevel must be JAVA_25")
    if config.get("required") is not False:
        fail(f"{config_name}: required must remain false")
    if config.get("injectors", {}).get("defaultRequire") != 0:
        fail(f"{config_name}: injectors.defaultRequire must remain 0")

registered_classes: dict[str, str] = {}
source_classes_by_config: dict[str, set[str]] = {name: set() for name in mixin_configs}
for source in MAIN.rglob("*Mixin.java"):
    relative = source.relative_to(MAIN).with_suffix("")
    fqcn = ".".join(relative.parts)
    matching_packages = [pkg for pkg in package_to_config if fqcn.startswith(pkg + ".")]
    if not matching_packages:
        fail(f"Mixin source is not covered by any configured package: {fqcn}")
        continue
    owner_package = max(matching_packages, key=len)
    owner_config = package_to_config[owner_package]
    entry = fqcn[len(owner_package) + 1:]
    source_classes_by_config[owner_config].add(entry)

for config_name in mixin_configs:
    config = config_data.get(config_name, {})
    package = config.get("package")
    entries: list[str] = []
    for section in ("mixins", "client", "server"):
        values = config.get(section, [])
        if not isinstance(values, list):
            fail(f"{config_name}: {section} must be an array")
            continue
        entries.extend(str(v) for v in values)
    configured_entries = set(entries)
    source_entries = source_classes_by_config.get(config_name, set())
    for missing in sorted(source_entries - configured_entries):
        fail(f"{config_name}: source Mixin is not registered: {missing}")
    for stale in sorted(configured_entries - source_entries):
        fail(f"{config_name}: registered Mixin has no source file: {stale}")
    if len(entries) != len(configured_entries):
        fail(f"{config_name}: duplicate Mixin registration")
    if isinstance(package, str):
        for entry in entries:
            full = f"{package}.{entry}"
            previous = registered_classes.get(full)
            if previous:
                fail(f"Mixin registered by multiple configs: {full} ({previous}, {config_name})")
            registered_classes[full] = config_name

    plugin = config.get("plugin")
    if plugin:
        plugin_path = MAIN / Path(*str(plugin).split(".")).with_suffix(".java")
        if not plugin_path.is_file():
            fail(f"{config_name}: plugin source missing: {plugin}")

# Source identity and lower-level hardening audits are mandatory gates.
identity_audit = ROOT / "scripts/source_identity_audit.py"
if not identity_audit.is_file():
    fail("scripts/source_identity_audit.py is missing")
audit_script = ROOT / "scripts/pre_java25_audit.py"
if not audit_script.is_file():
    fail("scripts/pre_java25_audit.py is missing")

# Wrapper integrity must stay enabled and checksum-pinned.
wrapper = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8")
if "distributionSha256Sum=" not in wrapper:
    fail("Gradle wrapper distributionSha256Sum is missing")
if "validateDistributionUrl=true" not in wrapper:
    fail("Gradle wrapper validateDistributionUrl=true is missing")

# Workflow contracts: exact release tags, least privilege, and no pull_request_target.
workflow_dir = ROOT / ".github/workflows"
required_workflows = {"ci.yml", "verify-build.yml", "release.yml"}
existing_workflows = {p.name for p in workflow_dir.glob("*.yml")} if workflow_dir.is_dir() else set()
missing_workflows = sorted(required_workflows - existing_workflows)
if missing_workflows:
    fail(f"required GitHub workflows are missing: {missing_workflows}")
workflow_text = "\n".join((workflow_dir / name).read_text(encoding="utf-8") for name in sorted(existing_workflows))
if "pull_request_target:" in workflow_text:
    fail("pull_request_target is forbidden for this repository CI")
if re.search(r"uses:\s+[^\s]+@v\d+\s*$", workflow_text, re.M):
    fail("GitHub Actions must use an exact release tag, not a moving major-only tag")
ci_text = (workflow_dir / "ci.yml").read_text(encoding="utf-8") if workflow_dir.is_dir() else ""
verify_text = (workflow_dir / "verify-build.yml").read_text(encoding="utf-8") if workflow_dir.is_dir() else ""
release_text = (workflow_dir / "release.yml").read_text(encoding="utf-8") if workflow_dir.is_dir() else ""
if "contents: read" not in ci_text or "contents: read" not in verify_text:
    fail("normal CI/reusable verification must use read-only contents permission")
if "contents: write" not in release_text:
    fail("release workflow must explicitly scope contents: write")
for required_action in (
    "actions/checkout@v7.0.1",
    "actions/setup-java@v5.7.0",
    "gradle/actions/setup-gradle@v6.3.0",
    "actions/upload-artifact@v7.0.1",
):
    if required_action not in verify_text:
        fail(f"verify-build.yml lost pinned action: {required_action}")
if "actions/download-artifact@v8.0.1" not in release_text:
    fail("release.yml lost pinned download-artifact action")
if "python3 scripts/source_identity_audit.py" not in verify_text:
    fail("verify-build.yml no longer runs the source identity audit")
if "./gradlew --no-daemon --stacktrace clean qualityGate build" not in verify_text:
    fail("verify-build.yml no longer executes the full quality gate before build")
if "sha256sum --check SHA256SUMS.txt" not in release_text:
    fail("release workflow no longer verifies downloaded artifact checksums")

# Reproducible runtime JAR settings must not regress.
build_gradle = (ROOT / "build.gradle").read_text(encoding="utf-8")
for required in (
    "preserveFileTimestamps = false",
    "reproducibleFileOrder = true",
    "jacocoTestCoverageVerification",
    "minimum = 0.96",
    "mutationThreshold = 96",
    "testStrengthThreshold = 96",
):
    if required not in build_gradle:
        fail(f"build.gradle lost quality/reproducibility contract: {required}")

if failures:
    print("CI CONTRACT AUDIT: FAIL")
    for item in failures:
        print(f" - {item}")
    if warnings:
        print("Warnings:")
        for item in warnings:
            print(f" - {item}")
    sys.exit(1)

print(
    "CI CONTRACT AUDIT: PASS "
    f"({len(java_files)} production Java files, {len(en)} translation keys, "
    f"{len(registered_classes)} registered Mixins)"
)
