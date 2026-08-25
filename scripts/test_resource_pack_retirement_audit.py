#!/usr/bin/env python3
from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

import resource_pack_retirement_audit as audit


class ResourcePackRetirementAuditTest(unittest.TestCase):
    def test_classification_is_case_and_separator_insensitive(self) -> None:
        self.assertIn("amateras", audit.classify("AMATERAS_Resourcepack_mc26.1.2.zip"))
        self.assertIn("new_glowing_ores", audit.classify("New Glowing Ores.zip"))
        self.assertIn("low_on_fire", audit.classify("LowOnFire v26.2.zip"))
        self.assertIn("legacy_chise", audit.classify("chise_texture_pack_mc26.1.2.zip"))

    def test_inventory_is_read_only_and_reports_rpo_dependency(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            packs = root / "resourcepacks"
            packs.mkdir()
            for name in (
                "AMATERAS Resourcepack.zip", "AMATERAS Resourcepack.rpo", "NewGlowingOres.zip",
                "LowOnFire.zip", "chise_texture_pack.zip", "Small Handhelds.zip",
            ):
                (packs / name).write_bytes(b"unchanged")
            before = {path.name: path.read_bytes() for path in packs.iterdir()}
            report = audit.inventory(root)
            after = {path.name: path.read_bytes() for path in packs.iterdir()}
            self.assertEqual(before, after)
            self.assertFalse(report["mutation_performed"])
            self.assertEqual(4, report["candidate_count_present"])
            self.assertEqual(("AMATERAS Resourcepack.rpo",), report["rpo_files"])
            self.assertEqual("RPO_DEPENDENCIES_REQUIRE_REVIEW", report["respackopts_retirement_preflight"])
            self.assertTrue(report["physical_prism_acceptance_required"])
            self.assertTrue(report["shader_acceptance_required_for_new_glowing_ores"])

    def test_no_rpo_files_is_only_a_preflight_signal(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            packs = root / "resourcepacks"
            packs.mkdir()
            (packs / "Redstone Tweaks.zip").write_bytes(b"retained")
            report = audit.inventory(root)
            self.assertEqual("NO_RPO_FILES_PRESENT", report["respackopts_retirement_preflight"])
            self.assertEqual(0, report["candidate_count_present"])
            self.assertTrue(report["physical_prism_acceptance_required"])

    def test_markdown_never_contains_instance_absolute_path(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            packs = root / "resourcepacks"
            packs.mkdir()
            (packs / "LowOnFire.zip").write_bytes(b"pack")
            rendered = audit.markdown(audit.inventory(root))
            self.assertNotIn(str(root), rendered)
            self.assertIn("LowOnFire.zip", rendered)
            self.assertIn("read-only", rendered)


if __name__ == "__main__":
    unittest.main()
