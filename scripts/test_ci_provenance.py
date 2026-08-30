#!/usr/bin/env python3
import json
from pathlib import Path
from tempfile import TemporaryDirectory

from ci_provenance import promotion_decision, write_provenance


BASE = {
    "schema": 2,
    "repository": "owner/repo",
    "source_run_id": 42,
    "event": "pull_request",
    "scope": "full",
    "tested_commit_sha": "a" * 40,
    "tested_tree_sha": "b" * 40,
    "version": "0.15.0+mc26.1.2",
    "verification": {
        "repository_contracts": True,
        "quality_gate": True,
        "client_gametest": True,
        "distribution_audit": True,
    },
    "runtime_verified": True,
    "runtime_jar": "chise-tweaks-0.15.0+mc26.1.2.jar",
    "runtime_sha256": "c" * 64,
}


def decide(payload, *, tree="b" * 40, run_id=42, has_runtime=True):
    return promotion_decision(
        payload,
        repository="owner/repo",
        expected_run_id=run_id,
        current_tree_sha=tree,
        has_runtime_artifact=has_runtime,
    )


def main() -> int:
    result = decide(dict(BASE))
    assert result == {
        "scope": "full", "heavy": False, "promote": True, "reason": "tree-identical-full"}

    assert decide(dict(BASE), tree="d" * 40)["reason"] == "tree-mismatch"
    assert decide(dict(BASE), tree="NOT-A-SHA")["reason"] == "tree-sha-format"
    assert decide(dict(BASE), has_runtime=False)["reason"] == "runtime-artifact-missing"
    assert decide(dict(BASE), run_id=99)["reason"] == "run-id"

    malformed_commit = dict(BASE)
    malformed_commit["tested_commit_sha"] = "NOT-A-COMMIT"
    assert decide(malformed_commit)["reason"] == "commit-sha-format"

    for gate in ("quality_gate", "client_gametest", "distribution_audit"):
        incomplete = json.loads(json.dumps(BASE))
        incomplete["verification"][gate] = False
        assert decide(incomplete)["reason"] == "verification-incomplete"

    no_contracts = json.loads(json.dumps(BASE))
    no_contracts["verification"]["repository_contracts"] = False
    assert decide(no_contracts)["reason"] == "repository-contracts-unverified"

    docs = json.loads(json.dumps(BASE))
    docs.update(scope="docs-only", runtime_verified=False, runtime_jar="", runtime_sha256="")
    docs["verification"] = {
        "repository_contracts": True,
        "quality_gate": False,
        "client_gametest": False,
        "distribution_audit": False,
    }
    assert decide(docs) == {
        "scope": "docs-only", "heavy": False, "promote": False,
        "reason": "tree-identical-non-runtime"}

    tooling = json.loads(json.dumps(docs))
    tooling["scope"] = "tooling-only"
    assert decide(tooling)["heavy"] is False

    forged_docs = json.loads(json.dumps(docs))
    forged_docs["runtime_jar"] = "chise-tweaks-forged.jar"
    assert decide(forged_docs)["reason"] == "non-runtime-claimed-artifact"

    forged_heavy = json.loads(json.dumps(docs))
    forged_heavy["verification"]["quality_gate"] = True
    assert decide(forged_heavy)["reason"] == "non-full-claimed-heavy-verification"

    malformed = dict(BASE)
    malformed["repository"] = "other/repo"
    assert decide(malformed)["reason"] == "repository"

    malformed_hash = dict(BASE)
    malformed_hash["runtime_sha256"] = "Z" * 64
    assert decide(malformed_hash)["reason"] == "runtime-sha256"

    with TemporaryDirectory() as directory:
        output = Path(directory) / "ci-provenance.json"
        write_provenance(
            output,
            repository="owner/repo",
            run_id=42,
            event="pull_request",
            scope="full",
            tested_commit_sha="a" * 40,
            tested_tree_sha="b" * 40,
            version="0.15.0+mc26.1.2",
            runtime_jar="chise-tweaks-0.15.0+mc26.1.2.jar",
            runtime_sha256="c" * 64,
            runtime_verified=True,
            repository_contracts_verified=True,
            quality_gate_verified=True,
            client_gametest_verified=True,
            distribution_verified=True,
        )
        written = json.loads(output.read_text(encoding="utf-8"))
        assert written["schema"] == 2
        assert written["tested_tree_sha"] == "b" * 40
        assert written["runtime_verified"] is True
        assert all(written["verification"].values())

    try:
        with TemporaryDirectory() as directory:
            write_provenance(
                Path(directory) / "invalid.json",
                repository="owner/repo",
                run_id=42,
                event="pull_request",
                scope="full",
                tested_commit_sha="a" * 40,
                tested_tree_sha="b" * 40,
                version="0.15.0+mc26.1.2",
                runtime_jar="chise-tweaks-0.15.0+mc26.1.2.jar",
                runtime_sha256="c" * 64,
                runtime_verified=True,
                repository_contracts_verified=True,
                quality_gate_verified=True,
                client_gametest_verified=False,
                distribution_verified=True,
            )
        raise AssertionError("incomplete full verification must be rejected")
    except ValueError:
        pass

    print("CI PROVENANCE TESTS: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
