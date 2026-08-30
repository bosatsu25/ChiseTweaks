#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github/workflows/ci.yml"


def require(text: str, marker: str, label: str) -> None:
    if marker not in text:
        raise AssertionError(f"{label}: missing {marker!r}")


def main() -> int:
    text = WORKFLOW.read_text(encoding="utf-8")

    require(text, "name: verify / Java 25 quality gate", "required status-check name")
    require(text, "python3 scripts/ci_scope.py --file-list", "scope classifier")
    require(text, "--diff-filter=ACDMRTUXB", "deletion-aware diff")
    require(text, "if [[ \"$EVENT_NAME\" != 'pull_request' ]]", "main/workflow_dispatch full gate")
    require(text, "python scripts/test_ci_scope.py", "scope self-test")
    require(text, "python scripts/test_ci_workflow_contract.py", "workflow self-test")
    require(text, "success() && steps.scope.outputs.heavy == 'true'", "artifact upload heavy gate")

    heavy_condition = "if: ${{ steps.scope.outputs.heavy == 'true' }}"
    if text.count(heavy_condition) < 6:
        raise AssertionError("heavy runtime steps are no longer consistently scope-gated")

    audit_index = text.index("- name: Audit repository contracts")
    java_index = text.index("- name: Set up Java 25")
    if audit_index >= java_index:
        raise AssertionError("static contract audits must fail fast before Java/Gradle setup")

    print("CI WORKFLOW CONTRACT TESTS: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
