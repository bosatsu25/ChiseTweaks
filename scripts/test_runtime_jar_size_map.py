#!/usr/bin/env python3
from __future__ import annotations

import tempfile
import unittest
import zipfile
from pathlib import Path

import runtime_jar_size_map as size_map


class RuntimeJarSizeMapTest(unittest.TestCase):
    def test_report_counts_classes_packages_overhead_and_duplicates(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            jar = Path(temporary) / "chise-test.jar"
            with zipfile.ZipFile(jar, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                archive.writestr("dev/chise/A.class", b"same-class-payload")
                archive.writestr("dev/chise/A$Inner.class", b"inner")
                archive.writestr("assets/chise/a.txt", b"same-class-payload")
                archive.writestr("fabric.mod.json", b"{}")

            report = size_map.build_report(jar, top=20)
            self.assertEqual("chise-test.jar", report["artifact_name"])
            self.assertEqual(4, report["entry_count"])
            self.assertEqual(2, report["class_count"])
            self.assertEqual(1, report["synthetic_or_inner_class_count"])
            self.assertGreater(report["container_overhead_bytes"], 0)
            self.assertEqual(1, report["duplicate_group_count"])
            self.assertGreater(report["potential_compressed_duplicate_bytes"], 0)
            self.assertEqual("dev.chise", report["top_class_packages"][0]["package"])
            duplicate = report["duplicate_payload_groups"][0]
            self.assertEqual(
                ["assets/chise/a.txt", "dev/chise/A.class"],
                duplicate["entries"],
            )
            self.assertGreater(duplicate["potential_compressed_duplicate_bytes"], 0)

    def test_markdown_never_exposes_input_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            jar = Path(temporary) / "runtime.jar"
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("fabric.mod.json", b"{}")
            rendered = size_map.markdown(size_map.build_report(jar, top=5))
            self.assertIn("`runtime.jar`", rendered)
            self.assertNotIn(str(Path(temporary)), rendered)

    def test_package_for_handles_default_and_nested_packages(self) -> None:
        self.assertEqual("<default>", size_map.package_for("Main.class"))
        self.assertEqual("dev.chise", size_map.package_for("dev/chise/Main.class"))


if __name__ == "__main__":
    unittest.main()
