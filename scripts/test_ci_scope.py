#!/usr/bin/env python3
from ci_scope import classify, is_docs_only_path


def expect(actual, expected, label):
    if actual != expected:
        raise AssertionError(f"{label}: expected {expected!r}, got {actual!r}")


def main() -> int:
    expect(classify([]), "full", "empty change set fails closed")
    expect(classify(["README.md"]), "docs-only", "README only")
    expect(classify(["DEVELOPMENT.md", "docs/ci.md"]), "docs-only", "document set")
    expect(classify(["src/main/java/X.java"]), "full", "production source")
    expect(classify(["src/test/java/XTest.java"]), "full", "test source")
    expect(classify([".github/workflows/ci.yml"]), "full", "workflow")
    expect(classify(["scripts/repository_audit.py"]), "full", "audit tooling")
    expect(classify(["quality/functional-parity-baseline.json"]), "full", "quality baseline")
    expect(classify(["README.md", "build.gradle"]), "full", "mixed docs/runtime")
    expect(classify(["docs/../src/main/java/X.java"]), "full", "path traversal shape")
    expect(is_docs_only_path("docs/ci.md"), True, "docs prefix")
    expect(is_docs_only_path("../README.md"), False, "parent segment")
    print("CI SCOPE TESTS: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
