from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

EXPECTED = {
    "checkout": "actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1",
    "setup_java": "actions/setup-java@b6effb05e454b25005698d916606bdc6ffcbf961 # v5.7.0",
    "setup_python": "actions/setup-python@5fda3b95a4ea91299a34e894583c3862153e4b97 # v7.0.0",
    "setup_gradle": "gradle/actions/setup-gradle@9c971963bec38e04b3d30dcc455b5382be2fdbfb # v6.3.0",
    "python": "python-version: '3.14'",
    "java": "java-version: '25'",
}
EXPECTED_GRADLE_URL = "gradle-9.7.1-bin.zip"
EXPECTED_GRADLE_SHA256 = "acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a"


def require(text: str, needle: str, source: str) -> None:
    if needle not in text:
        raise SystemExit(f"CI TOOLCHAIN AUDIT: FAIL: {source} missing {needle!r}")


def main() -> None:
    verify = (ROOT / ".github/workflows/verify-build.yml").read_text(encoding="utf-8")
    release = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
    wrapper = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8")

    for source, text in (("verify-build.yml", verify), ("release.yml", release)):
        for needle in EXPECTED.values():
            require(text, needle, source)
        require(text, "runs-on: ubuntu-24.04", source)

    require(wrapper, EXPECTED_GRADLE_URL, "gradle-wrapper.properties")
    require(wrapper, f"distributionSha256Sum={EXPECTED_GRADLE_SHA256}", "gradle-wrapper.properties")

    print("CI TOOLCHAIN AUDIT: PASS")
    print("runner=ubuntu-24.04")
    print("java=25")
    print("python=3.14")
    print("gradle=9.7.1")
    print("actions=pinned_full_sha")


if __name__ == "__main__":
    main()
