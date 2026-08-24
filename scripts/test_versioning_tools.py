#!/usr/bin/env python3
from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from bump_version import compute_next_version, update_version_file
from versioning_core import classify_one_step, parse_version, replace_property


class VersioningToolsTest(unittest.TestCase):
    def test_patch_bump(self) -> None:
        self.assertEqual(
            compute_next_version("0.9.4+mc26.1.2", "26.1.2", "patch"),
            "0.9.5+mc26.1.2",
        )

    def test_minor_bump(self) -> None:
        self.assertEqual(
            compute_next_version("0.9.4+mc26.1.2", "26.1.2", "minor"),
            "0.10.0+mc26.1.2",
        )

    def test_major_bump(self) -> None:
        self.assertEqual(
            compute_next_version("0.9.4+mc26.1.2", "26.1.2", "major"),
            "1.0.0+mc26.1.2",
        )

    def test_malformed_version_rejected(self) -> None:
        with self.assertRaises(ValueError):
            parse_version("0.10+mc26.1.2")

    def test_minecraft_metadata_mismatch_rejected(self) -> None:
        with self.assertRaises(ValueError):
            compute_next_version("0.9.4+mc26.1.1", "26.1.2", "minor")

    def test_one_step_classification(self) -> None:
        self.assertEqual(classify_one_step((0, 9, 4), (0, 9, 5)), "patch")
        self.assertEqual(classify_one_step((0, 9, 4), (0, 10, 0)), "minor")
        self.assertEqual(classify_one_step((0, 9, 4), (1, 0, 0)), "major")
        self.assertIsNone(classify_one_step((0, 9, 4), (0, 11, 0)))
        self.assertIsNone(classify_one_step((0, 9, 4), (0, 9, 4)))

    def test_replace_property_requires_single_authoritative_value(self) -> None:
        with self.assertRaises(ValueError):
            replace_property("mod_version=a\nmod_version=b\n", "mod_version", "c")

    def test_failed_update_does_not_modify_file(self) -> None:
        original = "minecraft_version=26.1.2\nmod_version=0.9.4+mc26.1.1\n"
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "gradle.properties"
            path.write_text(original, encoding="utf-8")
            with self.assertRaises(ValueError):
                update_version_file(path, "minor")
            self.assertEqual(path.read_text(encoding="utf-8"), original)

    def test_dry_run_does_not_modify_file(self) -> None:
        original = "minecraft_version=26.1.2\nmod_version=0.9.4+mc26.1.2\n"
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "gradle.properties"
            path.write_text(original, encoding="utf-8")
            previous, current = update_version_file(path, "minor", dry_run=True)
            self.assertEqual(previous, "0.9.4+mc26.1.2")
            self.assertEqual(current, "0.10.0+mc26.1.2")
            self.assertEqual(path.read_text(encoding="utf-8"), original)


if __name__ == "__main__":
    unittest.main()
