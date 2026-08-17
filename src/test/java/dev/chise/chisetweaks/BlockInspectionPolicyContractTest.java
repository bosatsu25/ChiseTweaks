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
    void classificationCoversTheSixRetainedScanCategories() {
        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE,
                BlockInspectionPolicy.classify("minecraft:tripwire"));
        assertEquals(BlockInspectionCategory.HIDDEN_SURFACE,
                BlockInspectionPolicy.classify("minecraft:powder_snow"));
        assertEquals(BlockInspectionCategory.GLASS_INSPECTION,
                BlockInspectionPolicy.classify("minecraft:white_stained_glass_pane"));
        assertEquals(BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                BlockInspectionPolicy.classify("minecraft:diamond_ore"));
        assertEquals(BlockInspectionCategory.NETHER_PALETTE,
                BlockInspectionPolicy.classify("minecraft:netherrack"));
        assertEquals(BlockInspectionCategory.KELP_HIGHLIGHT,
                BlockInspectionPolicy.classify("minecraft:kelp"));
        assertEquals(BlockInspectionCategory.KELP_HIGHLIGHT,
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
    void retainedMatchRulesCoverTechnicalHiddenGlassMaterialNetherAndKelpFamilies() {
        assertTrue(BlockInspectionPolicy.matches("minecraft:tripwire_hook", BlockInspectionCategory.TECHNICAL_TRACE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:blue_ice", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:dead_fire_coral_wall_fan", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:tinted_glass", BlockInspectionCategory.GLASS_INSPECTION));
        assertTrue(BlockInspectionPolicy.matches("minecraft:red_stained_glass", BlockInspectionCategory.GLASS_INSPECTION));
        assertTrue(BlockInspectionPolicy.matches("minecraft:red_stained_glass_pane", BlockInspectionCategory.GLASS_INSPECTION));
        assertTrue(BlockInspectionPolicy.matches("minecraft:deepslate_emerald_ore", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertTrue(BlockInspectionPolicy.matches("minecraft:polished_blackstone_bricks", BlockInspectionCategory.NETHER_PALETTE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:kelp", BlockInspectionCategory.KELP_HIGHLIGHT));
        assertTrue(BlockInspectionPolicy.matches("minecraft:kelp_plant", BlockInspectionCategory.KELP_HIGHLIGHT));
        assertFalse(BlockInspectionPolicy.matches("minecraft:seagrass", BlockInspectionCategory.KELP_HIGHLIGHT));
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
    void kelpInspectionDoesNotRetainUnneededBlockState() {
        var head = BlockInspectionPolicy.inspect(
                "minecraft:kelp",
                Map.of("age", "24", "waterlogged", "true"),
                BlockInspectionCategory.KELP_HIGHLIGHT);
        assertEquals(BlockInspectionCategory.KELP_HIGHLIGHT, head.category());
        assertTrue(head.details().isEmpty());

        var plant = BlockInspectionPolicy.inspect(
                "minecraft:kelp_plant",
                Map.of("age", "24"),
                BlockInspectionCategory.KELP_HIGHLIGHT);
        assertEquals(BlockInspectionCategory.KELP_HIGHLIGHT, plant.category());
        assertTrue(plant.details().isEmpty());
    }

    @Test
    void categorySpecificInspectionRejectsMismatchedOrUnsafeInput() {
        var mismatch = BlockInspectionPolicy.inspect(
                "minecraft:stone", Map.of("powered", "true"), BlockInspectionCategory.TECHNICAL_TRACE);
        assertEquals(BlockInspectionCategory.NONE, mismatch.category());
        assertTrue(mismatch.details().isEmpty());
        assertEquals("", mismatch.compactDetails());

        var unsafeProperty = BlockInspectionPolicy.inspect(
                "minecraft:glass_pane",
                Map.of("north", "true\nfalse", "waterlogged", "false"),
                BlockInspectionCategory.GLASS_INSPECTION);
        assertEquals(List.of("waterlogged=false"), unsafeProperty.details());

        var invalidId = BlockInspectionPolicy.inspect("minecraft:bad id", Map.of());
        assertEquals("", invalidId.blockId());
        assertEquals(BlockInspectionCategory.NONE, invalidId.category());
        assertEquals(BlockInspectionCategory.NONE,
                BlockInspectionPolicy.inspect("minecraft:glass", Map.of(), null).category());
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
        assertTrue(BlockInspectionPolicy.isScanCategory(BlockInspectionCategory.KELP_HIGHLIGHT));
    }

    @Test
    void inspectionPresentationDefensivelyCopiesAndEnforcesDetailBudget() {
        var presentation = new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:glass",
                BlockInspectionCategory.GLASS_INSPECTION,
                List.of("north=true"),
                BlockInspectionCategory.GLASS_INSPECTION.argb());
        assertEquals(List.of("north=true"), presentation.details());

        assertThrows(NullPointerException.class, () -> new BlockInspectionPolicy.InspectionPresentation(
                null, BlockInspectionCategory.GLASS_INSPECTION, List.of(), 0));
        assertThrows(NullPointerException.class, () -> new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:glass", null, List.of(), 0));
        assertThrows(NullPointerException.class, () -> new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:glass", BlockInspectionCategory.GLASS_INSPECTION, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:glass",
                BlockInspectionCategory.GLASS_INSPECTION,
                List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), 0));
    }
}
