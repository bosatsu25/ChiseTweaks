#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CI_WORKFLOW = ROOT / ".github/workflows/ci.yml"
RELEASE_WORKFLOW = ROOT / ".github/workflows/release.yml"


def require(text: str, marker: str, label: str) -> None:
    if marker not in text:
        raise AssertionError(f"{label}: missing {marker!r}")


def main() -> int:
    ci = CI_WORKFLOW.read_text(encoding="utf-8")
    release = RELEASE_WORKFLOW.read_text(encoding="utf-8")

    require(ci, "name: verify / Java 25 quality gate", "required status-check name")
    require(ci, "python3 scripts/ci_scope.py --file-list", "scope classifier")
    require(ci, "--diff-filter=ACDMRTUXB", "deletion-aware diff")
    require(ci, "if [[ \"$EVENT_NAME\" != 'pull_request' ]]", "main/workflow_dispatch full gate")
    require(ci, "if [[ \"$scope\" == 'full' ]]", "only full scope enters heavy gate")
    require(ci, "python scripts/test_ci_scope.py", "scope self-test")
    require(ci, "python scripts/test_ci_workflow_contract.py", "workflow self-test")
    require(ci, "success() && steps.scope.outputs.heavy == 'true'", "artifact upload heavy gate")

    require(ci, "- ready_for_review", "draft-to-ready trigger")
    require(ci, "github.event.pull_request.draft == false", "draft PR skip")
    require(ci, "timeout-minutes: 15", "CI runaway budget cap")
    require(ci, "retention-days: 3", "short-lived CI artifact")
    require(ci, "vars.CHISE_CI_RUNS_ON", "runner override")
    require(release, "vars.CHISE_CI_RUNS_ON", "release runner override")
    require(release, "timeout-minutes: 5", "release runaway budget cap")

    heavy_condition = "if: ${{ steps.scope.outputs.heavy == 'true' }}"
    if ci.count(heavy_condition) < 6:
        raise AssertionError("heavy runtime steps are no longer consistently scope-gated")

    audit_index = ci.index("- name: Audit repository contracts")
    java_index = ci.index("- name: Set up Java 25")
    if audit_index >= java_index:
        raise AssertionError("static contract audits must fail fast before Java/Gradle setup")

    print("CI WORKFLOW CONTRACT TESTS: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
