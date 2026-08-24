#!/usr/bin/env python3
"""Validate ChiseTweaks SemVer and optional release progression intent."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from versioning_core import classify_one_step, expected_bump, parse_version, read_properties

ROOT = Path(__file__).resolve().parents[1]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--previous", help="Previous released version, without a leading v")
    parser.add_argument("--release-type", choices=("patch", "minor", "major"))
    parser.add_argument(
        "--allow-any-one-step",
        action="store_true",
        help="Accept any valid one-step patch/minor/major increment from --previous",
    )
    args = parser.parse_args()

    try:
        properties = read_properties(ROOT / "gradle.properties")
        current = properties["mod_version"]
        minecraft = properties["minecraft_version"]
        current_core, current_minecraft = parse_version(current)
        if current_minecraft != minecraft:
            raise ValueError(
                f"mod_version Minecraft metadata mc{current_minecraft} does not match minecraft_version={minecraft}"
            )

        if args.release_type and args.allow_any_one_step:
            raise ValueError("--release-type and --allow-any-one-step are mutually exclusive")
        if (args.release_type or args.allow_any_one_step) and not args.previous:
            raise ValueError("--previous is required for release progression validation")
        if args.previous and not (args.release_type or args.allow_any_one_step):
            raise ValueError("--previous requires --release-type or --allow-any-one-step")

        detected_release_type: str | None = None
        if args.previous:
            previous_core, _ = parse_version(args.previous)
            if args.release_type:
                expected = expected_bump(previous_core, args.release_type)
                if current_core != expected:
                    expected_text = ".".join(str(value) for value in expected)
                    current_text = ".".join(str(value) for value in current_core)
                    raise ValueError(
                        f"{args.release_type} release requires SemVer core {expected_text}, found {current_text}"
                    )
                detected_release_type = args.release_type
            else:
                detected_release_type = classify_one_step(previous_core, current_core)
                if detected_release_type is None:
                    previous_text = ".".join(str(value) for value in previous_core)
                    current_text = ".".join(str(value) for value in current_core)
                    raise ValueError(
                        f"version {current_text} is not a one-step patch/minor/major increment from {previous_text}"
                    )

        print("VERSION POLICY: PASS")
        print(f"version={current}")
        print(f"semver_core={'.'.join(str(value) for value in current_core)}")
        print(f"minecraft={minecraft}")
        if detected_release_type:
            print(f"release_type={detected_release_type}")
        return 0
    except (OSError, KeyError, ValueError) as error:
        print(f"VERSION POLICY: FAIL: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
