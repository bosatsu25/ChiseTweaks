#!/usr/bin/env python3
"""Validate a PR-verified runtime JAR promoted into the main CI run."""
from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path

import artifact_audit
import artifact_audit_core as core
import release_residue_audit

ROOT = Path(__file__).resolve().parents[1]
PROVENANCE = ROOT / "build" / "prior" / "ci-provenance.json"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    try:
        payload = json.loads(PROVENANCE.read_text(encoding="utf-8"))
        if payload.get("schema") != 1 or payload.get("scope") != "full":
            raise RuntimeError("promotion provenance is not a full runtime verification")
        if payload.get("runtime_verified") is not True:
            raise RuntimeError("promotion provenance does not mark the runtime as verified")

        current_tree = subprocess.run(
            ["git", "rev-parse", "HEAD^{tree}"],
            cwd=ROOT,
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()
        if payload.get("tested_tree_sha") != current_tree:
            raise RuntimeError("promoted artifact tree no longer matches current main tree")

        properties = core.read_properties(ROOT / "gradle.properties")
        version = properties["mod_version"]
        runtime_name = payload.get("runtime_jar")
        if runtime_name != f"{properties['archives_base_name']}-{version}.jar":
            raise RuntimeError("promoted runtime filename/version contract changed")

        runtime = ROOT / "build" / "libs" / runtime_name
        if not runtime.is_file():
            raise RuntimeError(f"promoted runtime JAR is missing: {runtime_name}")
        actual_hash = sha256(runtime)
        if actual_hash != payload.get("runtime_sha256"):
            raise RuntimeError("promoted runtime SHA-256 differs from PR provenance")

        core.audit_runtime(runtime, version, properties)
        artifact_audit.audit_bright_resource_pack_residue()
        artifact_audit.audit_runtime_size()
        release_residue_audit.audit_jar(runtime)

        print("PROMOTED ARTIFACT AUDIT: PASS")
        print(f"runtime={runtime_name}")
        print(f"sha256={actual_hash}")
        print(f"tree={current_tree}")
        return 0
    except (
            OSError, KeyError, ValueError, RuntimeError, subprocess.CalledProcessError,
            json.JSONDecodeError, zipfile.BadZipFile) as error:
        print(f"PROMOTED ARTIFACT AUDIT: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
