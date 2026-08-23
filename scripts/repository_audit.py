#!/usr/bin/env python3
"""Fail-fast repository audit for the retained eleven-feature ChiseTweaks scope."""
from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

RETAINED_ENGLISH_NAMES = (
    "Scene Filter: Blocks",
    "Scene Filter: Entities",
    "Fine Thread Trace",
    "Hidden Surface Trace",
    "Glass Highlight",
    "Ore Highlights",
    "Nether Palette",
    "Kelp Highlight",
    "Ancient Debris Analyzer",
    "Fire Visibility",
    "Lava Source Highlight",
)

FORBIDDEN_JAVA_TOKENS = (
    "PUMPKIN_SCAFFOLD",
    "PLACEMENT_GUIDE",
    "PumpkinScaffold",
    "PlacementGuide",
    "SODIUM_LAVA_HIGHLIGHT",
    "LavaHighlightRendererMixin",
    "LavaFluidRenderHandler",
    "ExternalHookCircuitBreaker",
    "BuilderEntityVisibilityPolicy",
    "ModVersionPolicy",
)

FORBIDDEN_PATHS = (
    "src/main/resources/chisetweaks.sodium.mixins.json",
    "src/main/java/dev/chise/chisetweaks/mixin/sodium/LavaHighlightRendererMixin.java",
    "src/main/java/dev/chise/chisetweaks/mixin/sodium/SodiumMixinPlugin.java",
    "src/main/java/dev/chise/chisetweaks/feature/building/PumpkinScaffoldFeature.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/PlacementGuideLineGeometry.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaFluidRenderHandler.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightConfig.java",
    "src/main/java/dev/chise/chisetweaks/runtime/ExternalHookCircuitBreaker.java",
    "src/main/java/dev/chise/chisetweaks/core/policy/BuilderEntityVisibilityPolicy.java",
    "src/main/java/dev/chise/chisetweaks/core/policy/ModVersionPolicy.java",
    "src/main/resources/assets/chisetweaks/models/block/visual/diamond_ore.json",
    "src/main/resources/assets/chisetweaks/models/block/visual/deepslate_diamond_ore.json",
    "src/main/resources/assets/chisetweaks/textures/block/visual/diamond_ore_chise.png.mcmeta",
    "src/main/resources/assets/chisetweaks/textures/block/visual/deepslate_diamond_ore_chise.png.mcmeta",
)

REQUIRED_PATHS = (
    "build.gradle",
    "gradle.properties",
    "gradle/wrapper/gradle-wrapper.properties",
    "src/main/resources/fabric.mod.json",
    "src/main/resources/chisetweaks.features.mixins.json",
    "src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/NearestPositionBuffer.java",
    ".github/workflows/ci.yml",
    ".github/workflows/verify-build.yml",
    ".github/workflows/release.yml",
    "scripts/ci_toolchain_audit.py",
    "scripts/quality_summary.py",
    "scripts/artifact_audit.py",
    "scripts/release_residue_audit.py",
)

FORBIDDEN_TRACKED_DIRECTORY_NAMES = {
    ".gradle", "build", "out", "run", "runs", ".fabric", ".mixin.out", "__pycache__",
}
FORBIDDEN_RESIDUE_SUFFIXES = {".log", ".tmp", ".temp", ".bak", ".class", ".swp"}
AUDITED_TEXT_SUFFIXES = {
    ".java", ".json", ".md", ".txt", ".gradle", ".properties", ".py", ".yml", ".yaml", ".toml", ".xml",
}
AUDITED_TEXT_NAMES = {".gitignore", ".gitattributes", ".editorconfig", "gradlew", "gradlew.bat", "LICENSE", "NOTICE"}

LOCAL_PATH_PATTERNS = (
    re.compile(r"[A-Za-z]:[\\/]+Users[\\/]+", re.IGNORECASE),
    re.compile(r"/home/[^/\\s]+/"),
    re.compile(r"AppData[\\/]", re.IGNORECASE),
    re.compile(r"(?:^|[\\/])PrismLauncher(?:[\\/]|$)", re.IGNORECASE),
    re.compile(r"(?:^|[\\/])\.minecraft(?:[\\/]|$)", re.IGNORECASE),
)
IPV4_PATTERN = re.compile(r"(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?(?![\d.])")
ALLOWED_IP_LITERALS = {"127.0.0.1", "0.0.0.0"}
SELF_PATH = Path("scripts/repository_audit.py")


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def tracked_paths() -> list[Path]:
    try:
        completed = subprocess.run(
            ["git", "ls-files", "-z"],
            cwd=ROOT,
            check=True,
            capture_output=True,
        )
        return [Path(value.decode("utf-8")) for value in completed.stdout.split(b"\0") if value]
    except (OSError, subprocess.CalledProcessError, UnicodeDecodeError):
        return [path.relative_to(ROOT) for path in ROOT.rglob("*") if path.is_file() and ".git" not in path.parts]


def contains_sensitive_ipv4(text: str) -> bool:
    for match in IPV4_PATTERN.finditer(text):
        host = match.group(0).split(":", 1)[0]
        octets = host.split(".")
        if len(octets) != 4 or any(int(value) > 255 for value in octets):
            continue
        if host not in ALLOWED_IP_LITERALS:
            return True
    return False


def audit_repository_hygiene(failures: list[str]) -> None:
    for relative in tracked_paths():
        path = ROOT / relative
        if not path.is_file():
            continue

        if any(part in FORBIDDEN_TRACKED_DIRECTORY_NAMES for part in relative.parts):
            fail(f"{relative}: generated/runtime directory must not be tracked", failures)
        if path.suffix.lower() in FORBIDDEN_RESIDUE_SUFFIXES:
            fail(f"{relative}: generated/log/temp artifact must not be tracked", failures)

        if relative == SELF_PATH:
            continue
        if path.suffix.lower() not in AUDITED_TEXT_SUFFIXES and path.name not in AUDITED_TEXT_NAMES:
            continue
        try:
            text = read_text(path)
        except UnicodeDecodeError:
            continue
        for pattern in LOCAL_PATH_PATTERNS:
            if pattern.search(text):
                fail(f"{relative}: local machine path detected", failures)
        if contains_sensitive_ipv4(text):
            fail(f"{relative}: non-loopback IPv4/server address detected", failures)


def audit_ci_toolchain(failures: list[str]) -> None:
    try:
        completed = subprocess.run(
            [sys.executable, str(ROOT / "scripts/ci_toolchain_audit.py")],
            cwd=ROOT,
            check=False,
            capture_output=True,
            text=True,
        )
    except OSError as error:
        fail(f"CI toolchain audit could not start: {error}", failures)
        return
    if completed.returncode != 0:
        detail = (completed.stderr or completed.stdout).strip()
        fail(f"CI toolchain audit failed: {detail}", failures)


def audit() -> list[str]:
    failures: list[str] = []

    for relative in REQUIRED_PATHS:
        if not (ROOT / relative).is_file():
            fail(f"required file missing: {relative}", failures)

    for relative in FORBIDDEN_PATHS:
        if (ROOT / relative).exists():
            fail(f"removed path returned: {relative}", failures)

    java_root = ROOT / "src/main/java"
    if java_root.is_dir():
        for path in java_root.rglob("*.java"):
            text = read_text(path)
            relative = path.relative_to(ROOT)
            for token in FORBIDDEN_JAVA_TOKENS:
                if token in text:
                    fail(f"{relative}: removed token still present: {token}", failures)

    feature_path = ROOT / "src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java"
    if feature_path.is_file():
        feature_source = read_text(feature_path)
        for name in RETAINED_ENGLISH_NAMES:
            if f'"{name}"' not in feature_source:
                fail(f"FeatureDefinition is missing retained name: {name}", failures)
        if feature_source.count("FeatureArea.RENDERING") != 11:
            fail("FeatureDefinition must contain exactly eleven retained rendering definitions", failures)

    fabric_path = ROOT / "src/main/resources/fabric.mod.json"
    if fabric_path.is_file():
        try:
            metadata = json.loads(read_text(fabric_path))
        except (OSError, json.JSONDecodeError) as error:
            fail(f"fabric.mod.json is invalid JSON: {error}", failures)
        else:
            if metadata.get("id") != "chisetweaks":
                fail("fabric.mod.json id must be chisetweaks", failures)
            if metadata.get("environment") != "client":
                fail("fabric.mod.json environment must be client", failures)
            entrypoints = metadata.get("entrypoints", {})
            if set(entrypoints) != {"client", "modmenu"}:
                fail(f"unexpected entrypoint set: {sorted(entrypoints)}", failures)
            if metadata.get("mixins") != ["chisetweaks.features.mixins.json"]:
                fail("unexpected mixin configuration set", failures)
            custom = metadata.get("custom", {}).get("chisetweaks", {})
            required_false = (
                "serverInstallationRequired",
                "customPlayProtocol",
                "remoteModDetection",
                "backgroundThreads",
                "automaticModDownload",
                "automaticJarReplacement",
                "modMenuRequired",
            )
            if custom.get("side") != "client-only":
                fail("custom side metadata must remain client-only", failures)
            for key in required_false:
                if custom.get(key) is not False:
                    fail(f"custom metadata {key} must be false", failures)

    build_path = ROOT / "build.gradle"
    if build_path.is_file():
        build = read_text(build_path)
        for marker in (
            "id 'jacoco'",
            "id 'info.solidsoft.pitest'",
            "tasks.register('qualityGate')",
            "mutationThreshold",
            "testStrengthThreshold",
            "WorksiteScanThrottlePolicy",
            "WorksiteHighlightProfilePolicy",
            "ChiseTweaksSettingsLayout",
            "PreReleaseUiPolicy",
            "FeatureManager$TickSlot",
        ):
            if marker not in build:
                fail(f"verification marker missing from build.gradle: {marker}", failures)

    audit_ci_toolchain(failures)
    audit_repository_hygiene(failures)
    return failures


def main() -> int:
    failures = audit()
    if failures:
        print("REPOSITORY AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1
    print("REPOSITORY AUDIT: PASS")
    print("scope=11 retained rendering features")
    print("client_only=true")
    print("ci_toolchain_policy=true")
    print("removed_feature_residue=false")
    print("local_machine_paths=false")
    print("non_loopback_ipv4_literals=false")
    print("tracked_runtime_residue=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
