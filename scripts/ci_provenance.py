#!/usr/bin/env python3
"""Create and validate CI provenance for tree-identical artifact promotion."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

VALID_SCOPES = {"docs-only", "tooling-only", "full"}


def is_lower_hex(value: object, length: int) -> bool:
    return (
        isinstance(value, str)
        and len(value) == length
        and all(character in "0123456789abcdef" for character in value)
    )


def write_provenance(
        output: Path,
        *,
        repository: str,
        run_id: int,
        event: str,
        scope: str,
        tested_commit_sha: str,
        tested_tree_sha: str,
        version: str,
        runtime_jar: str,
        runtime_sha256: str,
        runtime_verified: bool) -> None:
    if scope not in VALID_SCOPES:
        raise ValueError(f"invalid scope: {scope}")
    if not repository or run_id <= 0 or not version:
        raise ValueError("repository, positive run id and version are required")
    if not is_lower_hex(tested_commit_sha, 40) or not is_lower_hex(tested_tree_sha, 40):
        raise ValueError("tested commit/tree SHA must be lowercase 40-character hex")
    if event != "pull_request":
        raise ValueError("reusable provenance must originate from pull_request")
    if scope == "full" and not runtime_verified:
        raise ValueError("full provenance must verify a runtime artifact")
    if runtime_verified and (
            not runtime_jar
            or not runtime_jar.startswith("chise-tweaks-")
            or not runtime_jar.endswith(".jar")
            or not is_lower_hex(runtime_sha256, 64)):
        raise ValueError("verified runtime provenance requires Chise runtime filename and SHA-256")
    if not runtime_verified and (runtime_jar or runtime_sha256):
        raise ValueError("non-runtime provenance must not claim an artifact")

    payload = {
        "schema": 1,
        "repository": repository,
        "source_run_id": run_id,
        "event": event,
        "scope": scope,
        "tested_commit_sha": tested_commit_sha,
        "tested_tree_sha": tested_tree_sha,
        "version": version,
        "runtime_verified": runtime_verified,
        "runtime_jar": runtime_jar,
        "runtime_sha256": runtime_sha256,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def promotion_decision(
        payload: dict,
        *,
        repository: str,
        expected_run_id: int,
        current_tree_sha: str,
        has_runtime_artifact: bool) -> dict:
    fallback = {"scope": "full", "heavy": True, "promote": False, "reason": "fallback"}

    if payload.get("schema") != 1:
        return fallback | {"reason": "schema"}
    if payload.get("repository") != repository:
        return fallback | {"reason": "repository"}
    if payload.get("event") != "pull_request":
        return fallback | {"reason": "event"}
    if payload.get("source_run_id") != expected_run_id:
        return fallback | {"reason": "run-id"}
    tested_tree_sha = payload.get("tested_tree_sha")
    if not is_lower_hex(tested_tree_sha, 40) or not is_lower_hex(current_tree_sha, 40):
        return fallback | {"reason": "tree-sha-format"}
    if tested_tree_sha != current_tree_sha:
        return fallback | {"reason": "tree-mismatch"}

    scope = payload.get("scope")
    if scope not in VALID_SCOPES:
        return fallback | {"reason": "scope"}

    if scope in {"docs-only", "tooling-only"}:
        if payload.get("runtime_verified") or payload.get("runtime_jar") or payload.get("runtime_sha256"):
            return fallback | {"reason": "non-runtime-claimed-artifact"}
        return {"scope": scope, "heavy": False, "promote": False, "reason": "tree-identical-non-runtime"}

    if not payload.get("runtime_verified"):
        return fallback | {"reason": "runtime-unverified"}
    runtime_jar = payload.get("runtime_jar")
    runtime_sha256 = payload.get("runtime_sha256")
    if not isinstance(runtime_jar, str) or not runtime_jar.startswith("chise-tweaks-") or not runtime_jar.endswith(".jar"):
        return fallback | {"reason": "runtime-name"}
    if not is_lower_hex(runtime_sha256, 64):
        return fallback | {"reason": "runtime-sha256"}
    if not has_runtime_artifact:
        return fallback | {"reason": "runtime-artifact-missing"}

    return {"scope": "full", "heavy": False, "promote": True, "reason": "tree-identical-full"}


def main() -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)

    write = sub.add_parser("write")
    write.add_argument("--output", type=Path, required=True)
    write.add_argument("--repository", required=True)
    write.add_argument("--run-id", type=int, required=True)
    write.add_argument("--event", required=True)
    write.add_argument("--scope", required=True)
    write.add_argument("--tested-commit-sha", required=True)
    write.add_argument("--tested-tree-sha", required=True)
    write.add_argument("--version", required=True)
    write.add_argument("--runtime-jar", default="")
    write.add_argument("--runtime-sha256", default="")
    write.add_argument("--runtime-verified", action="store_true")

    decide = sub.add_parser("decide")
    decide.add_argument("--input", type=Path, required=True)
    decide.add_argument("--repository", required=True)
    decide.add_argument("--expected-run-id", type=int, required=True)
    decide.add_argument("--current-tree-sha", required=True)
    decide.add_argument("--has-runtime-artifact", choices=("true", "false"), required=True)

    args = parser.parse_args()
    if args.command == "write":
        write_provenance(
            args.output,
            repository=args.repository,
            run_id=args.run_id,
            event=args.event,
            scope=args.scope,
            tested_commit_sha=args.tested_commit_sha,
            tested_tree_sha=args.tested_tree_sha,
            version=args.version,
            runtime_jar=args.runtime_jar,
            runtime_sha256=args.runtime_sha256,
            runtime_verified=args.runtime_verified,
        )
        return 0

    payload = json.loads(args.input.read_text(encoding="utf-8"))
    decision = promotion_decision(
        payload,
        repository=args.repository,
        expected_run_id=args.expected_run_id,
        current_tree_sha=args.current_tree_sha,
        has_runtime_artifact=args.has_runtime_artifact == "true",
    )
    print(json.dumps(decision, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
