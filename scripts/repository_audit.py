#!/usr/bin/env python3
"""Fail-fast repository audit for the rebuilt eight-feature ChiseTweaks scope."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

RETAINED_ENGLISH_NAMES = (
    "Scene Filter: Blocks",
    "Scene Filter: Entities",
    "Fine Thread Trace",
    "Hidden Surface Trace",
    "Glass Inspection",
    "Ore Highlights",
    "Nether Palette",
    "Lava Analyzer",
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
)

FORBIDDEN_PATHS = (
    "src/main/resources/chisetweaks.sodium.mixins.json",
    "src/main/java/dev/chise/chisetweaks/mixin/sodium/LavaHighlightRendererMixin.java",
    "src/main/java/dev/chise/chisetweaks/mixin/sodium/SodiumMixinPlugin.java",
    "src/main/java/dev/chise/chisetweaks/feature/building/PumpkinScaffoldFeature.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/PlacementGuideLineGeometry.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaFluidRenderHandler.java",
    "src/main/java/dev/chise/chisetweaks/runtime/ExternalHookCircuitBreaker.java",
)

REQUIRED_PATHS = (
    "build.gradle",
    "gradle.properties",
    "src/main/resources/fabric.mod.json",
    "src/main/resources/chisetweaks.features.mixins.json",
    "src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java",
    "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java",
    ".github/workflows/ci.yml",
    ".github/workflows/verify-build.yml",
    ".github/workflows/release.yml",
    "scripts/quality_summary.py",
    "scripts/artifact_audit.py",
)

LOCAL_PATH_PATTERNS = (
    re.compile(r"[A-Za-z]:\\\\Users\\\\", re.IGNORECASE),
    re.compile(r"/home/[^/\\s]+/"),
    re.compile(r"AppData[/\\\\]", re.IGNORECASE),
)


def fail(message: str, failures: list[str]) -> None:
    failures.append(message)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


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
            for pattern in LOCAL_PATH_PATTERNS:
                if pattern.search(text):
                    fail(f"{relative}: local machine path detected", failures)

    feature_path = ROOT / "src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java"
    if feature_path.is_file():
        feature_source = read_text(feature_path)
        for name in RETAINED_ENGLISH_NAMES:
            if f'"{name}"' not in feature_source:
                fail(f"FeatureDefinition is missing retained name: {name}", failures)
        if feature_source.count("FeatureArea.RENDERING") != 8:
            fail("FeatureDefinition must contain exactly eight retained rendering definitions", failures)

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
        ):
            if marker not in build:
                fail(f"rebuilt verification marker missing from build.gradle: {marker}", failures)

    resources_root = ROOT / "src/main/resources"
    if resources_root.is_dir():
        for path in resources_root.rglob("*"):
            if not path.is_file() or path.suffix.lower() in {".png", ".jpg", ".jpeg", ".webp"}:
                continue
            try:
                text = read_text(path)
            except UnicodeDecodeError:
                continue
            relative = path.relative_to(ROOT)
            for pattern in LOCAL_PATH_PATTERNS:
                if pattern.search(text):
                    fail(f"{relative}: local machine path detected", failures)

    return failures


def main() -> int:
    failures = audit()
    if failures:
        print("REPOSITORY AUDIT: FAIL", file=sys.stderr)
        for failure in failures:
            print(f"- {failure}", file=sys.stderr)
        return 1
    print("REPOSITORY AUDIT: PASS")
    print("scope=8 retained rendering features")
    print("client_only=true")
    print("removed_feature_residue=false")
    print("local_machine_paths=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
