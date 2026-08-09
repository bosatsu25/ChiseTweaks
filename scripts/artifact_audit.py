#!/usr/bin/env python3
"""Audit built ChiseTweaks artifacts and emit release-safe metadata."""
from __future__ import annotations

import hashlib
import json
import os
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build"
LIBS = BUILD / "libs"
CI_DIR = BUILD / "ci"

FORBIDDEN_ENTRY_TOKENS = (
    "PumpkinScaffold",
    "PlacementGuide",
    "mixin/sodium/",
    "LavaHighlightRendererMixin",
    "LavaFluidRenderHandler",
    "ExternalHookCircuitBreaker",
)

REQUIRED_LICENSE_ENTRIES = {
    "LICENSE_chise-tweaks",
    "LICENSE_MIT_chise-tweaks",
    "LICENSE_APACHE-2.0_chise-tweaks",
    "NOTICE",
}


def read_properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def duplicate_entries(names: list[str]) -> list[str]:
    seen: set[str] = set()
    duplicates: list[str] = []
    for name in names:
        if name in seen and name not in duplicates:
            duplicates.append(name)
        seen.add(name)
    return duplicates


def audit_runtime(path: Path, expected_version: str, properties: dict[str, str]) -> dict[str, object]:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        duplicates = duplicate_entries(names)
        if duplicates:
            raise RuntimeError(f"runtime JAR contains duplicate entries: {duplicates[:5]}")
        for token in FORBIDDEN_ENTRY_TOKENS:
            if any(token in name for name in names):
                raise RuntimeError(f"runtime JAR contains removed production entry token: {token}")
        if any(name.endswith("Test.class") or "/test/" in name.lower() for name in names):
            raise RuntimeError("runtime JAR contains test classes")
        missing_licenses = sorted(REQUIRED_LICENSE_ENTRIES.difference(names))
        if missing_licenses:
            raise RuntimeError(f"runtime JAR missing license/notice entries: {missing_licenses}")
        if "fabric.mod.json" not in names:
            raise RuntimeError("runtime JAR is missing fabric.mod.json")

        metadata = json.loads(archive.read("fabric.mod.json").decode("utf-8"))
        if metadata.get("id") != "chisetweaks":
            raise RuntimeError("fabric.mod.json id is not chisetweaks")
        if metadata.get("version") != expected_version:
            raise RuntimeError(
                f"fabric.mod.json version {metadata.get('version')!r} != {expected_version!r}")
        if metadata.get("environment") != "client":
            raise RuntimeError("fabric.mod.json environment is not client")
        if set(metadata.get("entrypoints", {})) != {"client", "modmenu"}:
            raise RuntimeError("unexpected runtime entrypoint set")
        if metadata.get("mixins") != ["chisetweaks.features.mixins.json"]:
            raise RuntimeError("unexpected runtime mixin configuration set")

        depends = metadata.get("depends", {})
        if depends.get("minecraft") != properties["minecraft_version"]:
            raise RuntimeError("runtime Minecraft dependency does not match gradle.properties")
        if depends.get("java") != ">=25":
            raise RuntimeError("runtime Java dependency must be >=25")

        custom = metadata.get("custom", {}).get("chisetweaks", {})
        if custom.get("side") != "client-only":
            raise RuntimeError("runtime custom side metadata is not client-only")
        for key in (
            "serverInstallationRequired",
            "customPlayProtocol",
            "remoteModDetection",
            "backgroundThreads",
            "automaticModDownload",
            "automaticJarReplacement",
            "modMenuRequired",
        ):
            if custom.get(key) is not False:
                raise RuntimeError(f"runtime custom metadata {key} must be false")

        if "chisetweaks.sodium.mixins.json" in names:
            raise RuntimeError("obsolete Sodium mixin config returned to runtime JAR")

        return {
            "entries": len(names),
            "environment": metadata.get("environment"),
            "entrypoints": sorted(metadata.get("entrypoints", {}).keys()),
            "mixins": metadata.get("mixins", []),
        }


def audit_sources(path: Path) -> dict[str, object]:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        duplicates = duplicate_entries(names)
        if duplicates:
            raise RuntimeError(f"sources JAR contains duplicate entries: {duplicates[:5]}")
        for token in FORBIDDEN_ENTRY_TOKENS:
            if any(token in name for name in names):
                raise RuntimeError(f"sources JAR contains removed production entry token: {token}")
        missing_licenses = sorted(REQUIRED_LICENSE_ENTRIES.difference(names))
        if missing_licenses:
            raise RuntimeError(f"sources JAR missing license/notice entries: {missing_licenses}")
        return {"entries": len(names)}


def github_output(key: str, value: str) -> None:
    output = os.environ.get("GITHUB_OUTPUT")
    if not output:
        return
    with Path(output).open("a", encoding="utf-8") as handle:
        handle.write(f"{key}={value}\n")


def main() -> int:
    try:
        properties = read_properties(ROOT / "gradle.properties")
        version = properties["mod_version"]
        base_name = properties["archives_base_name"]
        runtime_name = f"{base_name}-{version}.jar"
        sources_name = f"{base_name}-{version}-sources.jar"
        runtime = LIBS / runtime_name
        sources = LIBS / sources_name
        if not runtime.is_file():
            raise RuntimeError(f"runtime JAR missing: {runtime}")
        if not sources.is_file():
            raise RuntimeError(f"sources JAR missing: {sources}")

        runtime_details = audit_runtime(runtime, version, properties)
        source_details = audit_sources(sources)
        runtime_hash = sha256(runtime)
        sources_hash = sha256(sources)

        CI_DIR.mkdir(parents=True, exist_ok=True)
        audit = {
            "version": version,
            "minecraft_version": properties["minecraft_version"],
            "java": 25,
            "runtime_jar": runtime_name,
            "runtime_sha256": runtime_hash,
            "sources_jar": sources_name,
            "sources_sha256": sources_hash,
            "client_only": True,
            "server_installation_required": False,
            "runtime": runtime_details,
            "sources": source_details,
        }
        (CI_DIR / "artifact-audit.json").write_text(
            json.dumps(audit, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        (CI_DIR / "SHA256SUMS.txt").write_text(
            f"{runtime_hash}  {runtime_name}\n{sources_hash}  {sources_name}\n",
            encoding="utf-8")
        (CI_DIR / "artifact-summary.md").write_text(
            "\n".join((
                "## Verified ChiseTweaks artifacts",
                "",
                f"- Version: `{version}`",
                f"- Minecraft: `{properties['minecraft_version']}`",
                "- Java: `25`",
                "- Side: **client-only**",
                f"- Runtime: `{runtime_name}`",
                f"- Runtime SHA-256: `{runtime_hash}`",
                f"- Sources: `{sources_name}`",
                f"- Sources SHA-256: `{sources_hash}`",
                "- Removed Pumpkin/Placement/Sodium-lava hook residue: **not present**",
                "",
            )),
            encoding="utf-8")

        github_output("version", version)
        github_output("runtime_jar", runtime_name)
        github_output("sources_jar", sources_name)
        github_output("runtime_sha256", runtime_hash)
        print("ARTIFACT AUDIT: PASS")
        print(f"runtime={runtime_name}")
        print(f"sha256={runtime_hash}")
        return 0
    except (OSError, KeyError, ValueError, json.JSONDecodeError, zipfile.BadZipFile, RuntimeError) as error:
        print(f"ARTIFACT AUDIT: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
