#!/usr/bin/env python3
from ci_scope import classify, is_docs_only_path, is_tooling_only_path


def expect(actual, expected, label):
    if actual != expected:
        raise AssertionError(f"{label}: expected {expected!r}, got {actual!r}")


def main() -> int:
    expect(classify([]), "full", "empty change set fails closed")
    expect(classify(["README.md"]), "docs-only", "README only")
    expect(classify(["DEVELOPMENT.md", "docs/ci.md"]), "docs-only", "document set")

    expect(classify([".github/workflows/ci.yml"]), "tooling-only", "workflow only")
    expect(classify(["scripts/repository_audit.py"]), "tooling-only", "audit tooling only")
    expect(classify(["quality/functional-parity-baseline.json"]), "tooling-only", "quality baseline only")
    expect(classify(["quality/risk-register.json"]), "full", "risk traceability changes require executable evidence")
    expect(classify(["README.md", ".github/workflows/ci.yml"]), "tooling-only", "docs plus tooling")

    expect(classify(["src/main/java/X.java"]), "full", "production source")
    expect(classify(["src/test/java/XTest.java"]), "full", "test source")
    expect(classify(["src/main/resources/fabric.mod.json"]), "full", "runtime resource")
    expect(classify(["build.gradle"]), "full", "Gradle build logic")
    expect(classify(["gradle.properties"]), "full", "runtime/build configuration")
    expect(classify(["README.md", "build.gradle"]), "full", "mixed docs/runtime")
    expect(classify(["scripts/repository_audit.py", "src/main/java/X.java"]), "full", "mixed tooling/runtime")
    expect(classify(["docs/../src/main/java/X.java"]), "full", "path traversal shape")

    expect(is_docs_only_path("docs/ci.md"), True, "docs prefix")
    expect(is_docs_only_path("../README.md"), False, "parent segment")
    expect(is_tooling_only_path(".github/workflows/ci.yml"), True, "workflow tooling")
    expect(is_tooling_only_path("quality/functional-parity-baseline.json"), True, "quality tooling")
    expect(is_tooling_only_path("quality/risk-register.json"), False, "risk register is quality-gated")
    expect(is_tooling_only_path("build.gradle"), False, "build logic remains full")

    print("CI SCOPE TESTS: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
