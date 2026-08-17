package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VanillaOreVisualCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockInspectionPolicyContractTest {
    @Test
    void classificationCoversTheFourRetainedScanCategories() {
        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE,
                BlockInspectionPolicy.classify("minecraft:tripwire"));
        assertEquals(BlockInspectionCategory.HIDDEN_SURFACE,
                BlockInspectionPolicy.classify("minecraft:powder_snow"));
        assertEquals(BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                BlockInspectionPolicy.classify("minecraft:diamond_ore"));
        assertEquals(BlockInspectionCategory.NETHER_PALETTE,
                BlockInspectionPolicy.classify("minecraft:netherrack"));
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.classify("minecraft:white_stained_glass_pane"));
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.classify("minecraft:tinted_glass"));
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.classify("minecraft:kelp"));
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.classify("minecraft:kelp_plant"));
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.classify("minecraft:stone"));
    }

    @Test
    void overlappingNetherResourcesRetainBothRelevantCategories() {
        assertEquals(Set.of(
                        BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                        BlockInspectionCategory.NETHER_PALETTE),
                BlockInspectionPolicy.categories("minecraft:crying_obsidian"));
        assertEquals(Set.of(
                        BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                        BlockInspectionCategory.NETHER_PALETTE),
                BlockInspectionPolicy.categories("minecraft:nether_gold_ore"));
        assertTrue(BlockInspectionPolicy.categories(null).isEmpty());
        assertTrue(BlockInspectionPolicy.categories("bad id").isEmpty());
    }

    @Test
    void retainedMatchRulesCoverTechnicalHiddenMaterialAndNetherFamilies() {
        assertTrue(BlockInspectionPolicy.matches("minecraft:tripwire_hook", BlockInspectionCategory.TECHNICAL_TRACE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:blue_ice", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:dead_fire_coral_wall_fan", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:deepslate_emerald_ore", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertTrue(BlockInspectionPolicy.matches("minecraft:polished_blackstone_bricks", BlockInspectionCategory.NETHER_PALETTE));
        assertFalse(BlockInspectionPolicy.matches("minecraft:glass", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(BlockInspectionPolicy.matches("minecraft:stone", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(BlockInspectionPolicy.matches("minecraft:tripwire", BlockInspectionCategory.NONE));
        assertFalse(BlockInspectionPolicy.matches("minecraft:tripwire", null));
    }

    @Test
    void technicalInspectionKeepsOnlyBoundedSafeRelevantState() {
        Map<String, String> properties = Map.of(
                "powered", "true",
                "facing", "north",
                "attached", "false",
                "unrelated", "ignored");
        var presentation = BlockInspectionPolicy.inspect(" MINECRAFT:TRIPWIRE_HOOK ", properties);
        assertEquals("minecraft:tripwire_hook", presentation.blockId());
        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE, presentation.category());
        assertEquals(List.of("attached=false", "facing=north", "powered=true"), presentation.details());
        assertEquals("attached=false, facing=north, powered=true", presentation.compactDetails());
        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE.argb(), presentation.argb());
    }

    @Test
    void categorySpecificInspectionRejectsMismatchedOrUnsafeInput() {
        var mismatch = BlockInspectionPolicy.inspect(
                "minecraft:stone", Map.of("powered", "true"), BlockInspectionCategory.TECHNICAL_TRACE);
        assertEquals(BlockInspectionCategory.NONE, mismatch.category());
        assertTrue(mismatch.details().isEmpty());
        assertEquals("", mismatch.compactDetails());

        var unsafeProperty = BlockInspectionPolicy.inspect(
                "minecraft:tripwire_hook",
                Map.of("powered", "true\nfalse", "facing", "north"),
                BlockInspectionCategory.TECHNICAL_TRACE);
        assertEquals(List.of("facing=north"), unsafeProperty.details());

        var invalidId = BlockInspectionPolicy.inspect("minecraft:bad id", Map.of());
        assertEquals("", invalidId.blockId());
        assertEquals(BlockInspectionCategory.NONE, invalidId.category());
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.inspect("minecraft:tripwire", Map.of(), null).category());
    }

    @Test
    void materialAndNetherCatalogsMatchTheCurrentRetainedScope() {
        assertEquals(VanillaOreVisualCatalog.blockVariantCount() + 2,
                BlockInspectionPolicy.materialHighlightIds().size());
        assertTrue(BlockInspectionPolicy.materialHighlightIds().containsAll(VanillaOreVisualCatalog.blockIds()));
        assertTrue(BlockInspectionPolicy.materialHighlightIds().contains("minecraft:obsidian"));
        assertTrue(BlockInspectionPolicy.materialHighlightIds().contains("minecraft:crying_obsidian"));
        assertEquals(27, BlockInspectionPolicy.netherPaletteIds().size());
        assertTrue(BlockInspectionPolicy.netherPaletteIds().contains("minecraft:netherrack"));
        assertTrue(BlockInspectionPolicy.netherPaletteIds().contains("minecraft:nether_quartz_ore"));
        assertTrue(BlockInspectionPolicy.netherPaletteIds().contains("minecraft:crying_obsidian"));
    }

    @Test
    void scanCategoryPredicateExcludesNullAndNone() {
        assertFalse(BlockInspectionPolicy.isScanCategory(null));
        assertFalse(BlockInspectionPolicy.isScanCategory(BlockInspectionCategory.NONE));
        assertTrue(BlockInspectionPolicy.isScanCategory(BlockInspectionCategory.TECHNICAL_TRACE));
        assertTrue(BlockInspectionPolicy.isScanCategory(BlockInspectionCategory.MATERIAL_HIGHLIGHT));
    }

    @Test
    void inspectionPresentationDefensivelyCopiesAndEnforcesDetailBudget() {
        var presentation = new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:tripwire",
                BlockInspectionCategory.TECHNICAL_TRACE,
                List.of("north=true"),
                BlockInspectionCategory.TECHNICAL_TRACE.argb());
        assertEquals(List.of("north=true"), presentation.details());

        assertThrows(NullPointerException.class, () -> new BlockInspectionPolicy.InspectionPresentation(
                null, BlockInspectionCategory.TECHNICAL_TRACE, List.of(), 0));
        assertThrows(NullPointerException.class, () -> new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:tripwire", null, List.of(), 0));
        assertThrows(NullPointerException.class, () -> new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:tripwire", BlockInspectionCategory.TECHNICAL_TRACE, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:tripwire",
                BlockInspectionCategory.TECHNICAL_TRACE,
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), 0));
    }
}
