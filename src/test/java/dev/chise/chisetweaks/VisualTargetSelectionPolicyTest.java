package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VanillaOreVisualCatalog;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualTargetSelectionPolicyTest {
    @Test
    void retainedTargetBitsKeepHistoricalPositionsAndAppendTechnicalBits() {
        assertEquals(19, Target.values().length);
        assertEquals(1 << 11, Target.MATERIAL_OBSIDIAN.bitMask());
        assertEquals(1 << 20, Target.MATERIAL_REDSTONE_ORE.bitMask());
        assertEquals(1 << 21, Target.HIDDEN_BLUE_ICE.bitMask());
        assertEquals(1 << 24, Target.HIDDEN_SCULK_CATALYST.bitMask());
        assertEquals(1 << 25, Target.MATERIAL_CRYING_OBSIDIAN.bitMask());
        assertEquals(1 << 27, Target.MATERIAL_NETHER_QUARTZ_ORE.bitMask());
        assertEquals(1 << 28, Target.TECHNICAL_TRIPWIRE.bitMask());
        assertEquals(1 << 29, Target.TECHNICAL_TRIPWIRE_HOOK.bitMask());
        assertEquals(0, VisualTargetSelectionPolicy.ALL_TARGETS_MASK & ((1 << 11) - 1));
    }

    @Test
    void masksAreCompleteDisjointAndSanitized() {
        assertEquals(0, VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK
                & VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK);
        assertEquals(0, VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK
                & VisualTargetSelectionPolicy.TECHNICAL_TRACE_TARGETS_MASK);
        assertEquals(0, VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK
                & VisualTargetSelectionPolicy.TECHNICAL_TRACE_TARGETS_MASK);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK
                        | VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK
                        | VisualTargetSelectionPolicy.TECHNICAL_TRACE_TARGETS_MASK);
        assertEquals((1 << 25) | (1 << 26) | (1 << 27),
                VisualTargetSelectionPolicy.NEW_NETHER_TARGETS_MASK);
        assertEquals((1 << 28) | (1 << 29),
                VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                VisualTargetSelectionPolicy.sanitizeMask(-1));
        assertEquals(0, VisualTargetSelectionPolicy.sanitizeMask((1 << 0) | (1 << 5) | (1 << 10)));
    }

    @Test
    void schemaMigrationAddsOnlyTargetsIntroducedAfterTheStoredSchema() {
        int diamondOnly = Target.MATERIAL_DIAMOND_ORE.bitMask();
        int legacyExpected = diamondOnly
                | VisualTargetSelectionPolicy.NEW_NETHER_TARGETS_MASK
                | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK;
        assertEquals(legacyExpected,
                VisualTargetSelectionPolicy.migrateMask(
                        diamondOnly, VisualTargetSelectionPolicy.LEGACY_SCHEMA_VERSION));
        assertEquals(legacyExpected,
                VisualTargetSelectionPolicy.migrateMask(diamondOnly, 0));
        assertEquals(diamondOnly | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK,
                VisualTargetSelectionPolicy.migrateMask(
                        diamondOnly, VisualTargetSelectionPolicy.NETHER_TARGET_SCHEMA_VERSION));
        assertEquals(diamondOnly,
                VisualTargetSelectionPolicy.migrateMask(
                        diamondOnly, VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION));
        assertEquals(diamondOnly,
                VisualTargetSelectionPolicy.migrateMask(
                        diamondOnly, VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION + 1));
        assertEquals(VisualTargetSelectionPolicy.NEW_NETHER_TARGETS_MASK
                        | VisualTargetSelectionPolicy.NEW_TECHNICAL_TARGETS_MASK,
                VisualTargetSelectionPolicy.migrateMask(
                        1, VisualTargetSelectionPolicy.LEGACY_SCHEMA_VERSION));
    }

    @Test
    void individualTargetOperationsAreNullSafeAndPreserveOtherGroups() {
        int hiddenAndTechnical = VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK
                | VisualTargetSelectionPolicy.TECHNICAL_TRACE_TARGETS_MASK;
        assertFalse(VisualTargetSelectionPolicy.isEnabled(
                hiddenAndTechnical, Target.MATERIAL_DIAMOND_ORE));
        assertFalse(VisualTargetSelectionPolicy.isEnabled(hiddenAndTechnical, null));

        int withDiamond = VisualTargetSelectionPolicy.withEnabled(
                hiddenAndTechnical, Target.MATERIAL_DIAMOND_ORE, true);
        assertTrue(VisualTargetSelectionPolicy.isEnabled(withDiamond, Target.MATERIAL_DIAMOND_ORE));
        assertEquals(hiddenAndTechnical, withDiamond & hiddenAndTechnical);

        int withoutDiamond = VisualTargetSelectionPolicy.withEnabled(
                withDiamond, Target.MATERIAL_DIAMOND_ORE, false);
        assertFalse(VisualTargetSelectionPolicy.isEnabled(withoutDiamond, Target.MATERIAL_DIAMOND_ORE));
        assertEquals(hiddenAndTechnical, withoutDiamond);
        assertEquals(hiddenAndTechnical,
                VisualTargetSelectionPolicy.withEnabled(hiddenAndTechnical | 1, null, true));
    }

    @Test
    void oreBulkOperationsNeverAlterTechnicalOrHiddenSelections() {
        int nonOre = VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK
                | VisualTargetSelectionPolicy.TECHNICAL_TRACE_TARGETS_MASK;
        int enabled = VisualTargetSelectionPolicy.withAllOreHighlightTargets(nonOre, true);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, enabled);
        assertEquals(nonOre, VisualTargetSelectionPolicy.withAllOreHighlightTargets(enabled, false));

        int diamondOnly = VisualTargetSelectionPolicy.withOnlyOreHighlightTarget(
                enabled, Target.MATERIAL_DIAMOND_ORE);
        assertEquals(nonOre | Target.MATERIAL_DIAMOND_ORE.bitMask(), diamondOnly);
        assertEquals(enabled, VisualTargetSelectionPolicy.withOnlyOreHighlightTarget(
                enabled, Target.HIDDEN_BLUE_ICE));
        assertEquals(enabled, VisualTargetSelectionPolicy.withOnlyOreHighlightTarget(
                enabled, Target.TECHNICAL_TRIPWIRE));
        assertEquals(enabled, VisualTargetSelectionPolicy.withOnlyOreHighlightTarget(enabled, null));

        assertTrue(VisualTargetSelectionPolicy.isOreHighlightTarget(Target.MATERIAL_OBSIDIAN));
        assertTrue(VisualTargetSelectionPolicy.isOreHighlightTarget(Target.MATERIAL_NETHER_QUARTZ_ORE));
        assertFalse(VisualTargetSelectionPolicy.isOreHighlightTarget(Target.HIDDEN_BLUE_ICE));
        assertFalse(VisualTargetSelectionPolicy.isOreHighlightTarget(Target.TECHNICAL_TRIPWIRE));
        assertFalse(VisualTargetSelectionPolicy.isOreHighlightTarget(null));
    }

    @Test
    void enabledMatchingUsesFineGrainedMasksForTechnicalMaterialAndHiddenCategories() {
        int all = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:diamond_ore", null));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:diamond_ore", BlockInspectionCategory.NONE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                all, null, BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                all, "   ", BlockInspectionCategory.MATERIAL_HIGHLIGHT));

        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:tripwire", BlockInspectionCategory.TECHNICAL_TRACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:tripwire_hook", BlockInspectionCategory.TECHNICAL_TRACE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.withEnabled(all, Target.TECHNICAL_TRIPWIRE, false),
                "minecraft:tripwire", BlockInspectionCategory.TECHNICAL_TRACE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.withEnabled(all, Target.TECHNICAL_TRIPWIRE_HOOK, false),
                "minecraft:tripwire_hook", BlockInspectionCategory.TECHNICAL_TRACE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:string", BlockInspectionCategory.TECHNICAL_TRACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:netherrack", BlockInspectionCategory.NETHER_PALETTE));

        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:obsidian", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:crying_obsidian", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "  MINECRAFT:DIAMOND_ORE  ", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.withEnabled(all, Target.MATERIAL_DIAMOND_ORE, false),
                "minecraft:diamond_ore", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:stone", BlockInspectionCategory.MATERIAL_HIGHLIGHT));

        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:blue_ice", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:powder_snow", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:sculk_catalyst", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:dead_brain_coral_block", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:dead_brain_coral", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:dead_brain_coral_fan", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:dead_brain_coral_wall_fan", BlockInspectionCategory.HIDDEN_SURFACE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:brain_coral_block", BlockInspectionCategory.HIDDEN_SURFACE));
    }

    @Test
    void vanillaOreCatalogDefinesExactlyElevenFamiliesAndNineteenVariants() {
        Set<String> expected = Set.of(
                "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
                "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
                "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
                "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
                "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
                "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
                "minecraft:ancient_debris");
        assertEquals(11, VanillaOreVisualCatalog.familyCount());
        assertEquals(19, VanillaOreVisualCatalog.blockVariantCount());
        assertEquals(expected, VanillaOreVisualCatalog.blockIds());
        assertEquals(11, VanillaOreVisualCatalog.families().stream().map(f -> f.target()).distinct().count());
        assertEquals(11, VanillaOreVisualCatalog.families().stream().map(f -> f.highlightKey()).distinct().count());
    }

    @Test
    void vanillaOreCatalogNormalizesIdsAndMapsFamiliesToExpectedTargets() {
        assertEquals(Target.MATERIAL_COAL_ORE,
                VanillaOreVisualCatalog.targetForBlockId("minecraft:deepslate_coal_ore"));
        assertEquals(Target.MATERIAL_DIAMOND_ORE,
                VanillaOreVisualCatalog.targetForBlockId(" MINECRAFT:DIAMOND_ORE "));
        assertEquals(Target.MATERIAL_NETHER_GOLD_ORE,
                VanillaOreVisualCatalog.targetForBlockId("minecraft:nether_gold_ore"));
        assertEquals(Target.MATERIAL_NETHER_QUARTZ_ORE,
                VanillaOreVisualCatalog.targetForBlockId("minecraft:nether_quartz_ore"));
        assertEquals(Target.MATERIAL_ANCIENT_DEBRIS,
                VanillaOreVisualCatalog.targetForBlockId("minecraft:ancient_debris"));
        assertNull(VanillaOreVisualCatalog.targetForBlockId(null));
        assertNull(VanillaOreVisualCatalog.targetForBlockId("minecraft:stone"));
        assertTrue(VanillaOreVisualCatalog.isVanillaOreBlock("minecraft:gold_ore"));
        assertFalse(VanillaOreVisualCatalog.isVanillaOreBlock("minecraft:raw_gold_block"));
        assertEquals("diamond", VanillaOreVisualCatalog.highlightKeyForBlockId("minecraft:diamond_ore"));
        assertEquals("ancient_debris", VanillaOreVisualCatalog.highlightKeyForBlockId("minecraft:ancient_debris"));
        assertEquals("", VanillaOreVisualCatalog.highlightKeyForBlockId("minecraft:stone"));
    }

    @Test
    void vanillaOreTargetMaskComposesWithTheTwoSpecialMaterials() {
        int specials = Target.MATERIAL_OBSIDIAN.bitMask() | Target.MATERIAL_CRYING_OBSIDIAN.bitMask();
        assertEquals(VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK,
                VanillaOreVisualCatalog.TARGET_MASK | specials);
        assertEquals(0, VanillaOreVisualCatalog.TARGET_MASK
                & VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK);
        assertEquals(0, VanillaOreVisualCatalog.TARGET_MASK
                & VisualTargetSelectionPolicy.TECHNICAL_TRACE_TARGETS_MASK);
    }

    @Test
    void catalogFamilyRejectsInvalidDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> new VanillaOreVisualCatalog.Family(
                null, "x", java.util.List.of("minecraft:stone")));
        assertThrows(IllegalArgumentException.class, () -> new VanillaOreVisualCatalog.Family(
                Target.MATERIAL_COAL_ORE, " ", java.util.List.of("minecraft:coal_ore")));
        assertThrows(IllegalArgumentException.class, () -> new VanillaOreVisualCatalog.Family(
                Target.MATERIAL_COAL_ORE, "coal", java.util.List.of()));
        assertThrows(IllegalArgumentException.class, () -> new VanillaOreVisualCatalog.Family(
                Target.MATERIAL_COAL_ORE, "coal", java.util.List.of("example:coal_ore")));
        assertThrows(NullPointerException.class, () -> new VanillaOreVisualCatalog.Family(
                Target.MATERIAL_COAL_ORE, "coal", null));
        assertThrows(NullPointerException.class, () -> new VanillaOreVisualCatalog.Family(
                Target.MATERIAL_COAL_ORE, "coal", Arrays.asList((String) null)));
    }
}
