#!/usr/bin/env python3
"""Audit built ChiseTweaks JARs and emit release-safe metadata."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def props(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def fail(message: str) -> None:
    print(f"ARTIFACT AUDIT: FAIL - {message}", file=sys.stderr)
    sys.exit(1)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--libs", type=Path, default=ROOT / "build/libs")
    parser.add_argument("--report-dir", type=Path, default=ROOT / "build/ci")
    args = parser.parse_args()

    p = props(ROOT / "gradle.properties")
    version = p["mod_version"]
    base = p["archives_base_name"]
    runtime = args.libs / f"{base}-{version}.jar"
    sources = args.libs / f"{base}-{version}-sources.jar"
    if not runtime.is_file():
        fail(f"expected runtime JAR not found: {runtime}")
    if not sources.is_file():
        fail(f"expected sources JAR not found: {sources}")

    runtime_candidates = [x for x in args.libs.glob("*.jar") if not x.name.endswith("-sources.jar")]
    if runtime_candidates != [runtime]:
        fail(f"expected exactly one runtime JAR; found {[x.name for x in runtime_candidates]}")

    required_runtime = {
        "fabric.mod.json",
        "chisetweaks.features.mixins.json",
        "chisetweaks.sodium.mixins.json",
        "NOTICE",
        f"LICENSE_{base}",
        f"LICENSE_MIT_{base}",
        f"LICENSE_APACHE-2.0_{base}",
    }
    forbidden_fragments = (".git/", ".gradle/", ".idea/", "src/test/", "META-INF/gradle-plugins/")

    with zipfile.ZipFile(runtime) as jar:
        names = set(jar.namelist())
        missing = sorted(required_runtime - names)
        if missing:
            fail(f"runtime JAR is missing required entries: {missing}")
        bad = sorted(
            name for name in names
            if name.endswith("Test.class") or any(fragment in name for fragment in forbidden_fragments)
        )
        if bad:
            fail(f"runtime JAR contains forbidden entries: {bad[:20]}")
        try:
            metadata = json.loads(jar.read("fabric.mod.json").decode("utf-8"))
        except Exception as exc:
            fail(f"cannot parse packaged fabric.mod.json: {exc}")

    required_sources = {
        "NOTICE",
        f"LICENSE_{base}",
        f"LICENSE_MIT_{base}",
        f"LICENSE_APACHE-2.0_{base}",
    }
    with zipfile.ZipFile(sources) as source_jar:
        source_names = set(source_jar.namelist())
        missing_sources = sorted(required_sources - source_names)
        if missing_sources:
            fail(f"sources JAR is missing required notices: {missing_sources}")

    if metadata.get("version") != version:
        fail(f"packaged version {metadata.get('version')!r} != {version!r}")
    if metadata.get("environment") != "client":
        fail("packaged mod environment is not client")
    depends = metadata.get("depends", {})
    expected_depends = {
        "minecraft": p["minecraft_version"],
        "fabricloader": f">={p['loader_version']}",
        "fabric-api": f">={p['fabric_api_version']}",
        "java": ">=25",
        "malilib": f">={p['malilib_version']}",
    }
    for key, expected in expected_depends.items():
        if depends.get(key) != expected:
            fail(f"packaged dependency {key}={depends.get(key)!r}, expected {expected!r}")
    recommends = metadata.get("recommends", {})
    if recommends.get("modmenu") != f">={p['modmenu_version']}":
        fail("packaged Mod Menu recommendation does not match gradle.properties")
    if recommends.get("sodium") != f">={p['sodium_compat_version']}":
        fail("packaged Sodium recommendation does not match gradle.properties")

    args.report_dir.mkdir(parents=True, exist_ok=True)
    runtime_hash = sha256(runtime)
    sources_hash = sha256(sources)
    report = {
        "version": version,
        "runtime_jar": runtime.name,
        "runtime_sha256": runtime_hash,
        "runtime_size": runtime.stat().st_size,
        "sources_jar": sources.name,
        "sources_sha256": sources_hash,
        "sources_size": sources.stat().st_size,
    }
    (args.report_dir / "artifact-audit.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    (args.report_dir / "SHA256SUMS.txt").write_text(
        f"{runtime_hash}  {runtime.name}\n{sources_hash}  {sources.name}\n", encoding="utf-8"
    )
    summary = (
        "## ChiseTweaks verified artifacts\n\n"
        f"- Version: `{version}`\n"
        f"- Runtime: `{runtime.name}` ({runtime.stat().st_size} bytes)\n"
        f"- Runtime SHA-256: `{runtime_hash}`\n"
        f"- Sources: `{sources.name}` ({sources.stat().st_size} bytes)\n"
        f"- Sources SHA-256: `{sources_hash}`\n"
    )
    (args.report_dir / "artifact-summary.md").write_text(summary, encoding="utf-8")

    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as output:
            output.write(f"version={version}\n")
            output.write(f"runtime_jar={runtime.name}\n")
            output.write(f"sources_jar={sources.name}\n")
            output.write(f"runtime_sha256={runtime_hash}\n")

    print(f"ARTIFACT AUDIT: PASS ({runtime.name}, sha256={runtime_hash})")


if __name__ == "__main__":
    main()
