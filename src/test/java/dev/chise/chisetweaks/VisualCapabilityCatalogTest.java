package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VanillaOreVisualCatalog;
import dev.chise.chisetweaks.core.vision.VisualCapabilityCatalog;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualCapabilityCatalogTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void catalogOwnsAllFourP0VisualDomainsAndPreservesOverlap() {
        assertEquals(Set.of(BlockInspectionCategory.TECHNICAL_TRACE),
                VisualCapabilityCatalog.categories("minecraft:tripwire"));
        assertEquals(Set.of(BlockInspectionCategory.HIDDEN_SURFACE),
                VisualCapabilityCatalog.categories("minecraft:powder_snow"));
        assertEquals(Set.of(BlockInspectionCategory.MATERIAL_HIGHLIGHT),
                VisualCapabilityCatalog.categories("minecraft:diamond_ore"));
        assertEquals(Set.of(BlockInspectionCategory.NETHER_PALETTE),
                VisualCapabilityCatalog.categories("minecraft:netherrack"));
        assertEquals(Set.of(
                        BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                        BlockInspectionCategory.NETHER_PALETTE),
                VisualCapabilityCatalog.categories("minecraft:crying_obsidian"));
        assertEquals(Set.of(
                        BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                        BlockInspectionCategory.NETHER_PALETTE),
                VisualCapabilityCatalog.categories("minecraft:nether_quartz_ore"));
        assertTrue(VisualCapabilityCatalog.categories(null).isEmpty());
        assertTrue(VisualCapabilityCatalog.categories("  ").isEmpty());
        assertTrue(VisualCapabilityCatalog.categories("minecraft:stone").isEmpty());
    }

    @Test
    void selectableTargetsCentralizeMaterialHiddenAndTechnicalOwnership() {
        assertEquals(Target.TECHNICAL_TRIPWIRE,
                VisualCapabilityCatalog.selectableTarget(
                        " MINECRAFT:TRIPWIRE ", BlockInspectionCategory.TECHNICAL_TRACE));
        assertEquals(Target.TECHNICAL_TRIPWIRE_HOOK,
                VisualCapabilityCatalog.selectableTarget(
                        "minecraft:tripwire_hook", BlockInspectionCategory.TECHNICAL_TRACE));
        assertEquals(Target.MATERIAL_OBSIDIAN,
                VisualCapabilityCatalog.selectableTarget(
                        "minecraft:obsidian", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertEquals(Target.MATERIAL_CRYING_OBSIDIAN,
                VisualCapabilityCatalog.selectableTarget(
                        "minecraft:crying_obsidian", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertEquals(Target.MATERIAL_DIAMOND_ORE,
                VisualCapabilityCatalog.selectableTarget(
                        "minecraft:deepslate_diamond_ore", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertEquals(Target.HIDDEN_BLUE_ICE,
                VisualCapabilityCatalog.selectableTarget(
                        "minecraft:blue_ice", BlockInspectionCategory.HIDDEN_SURFACE));
        assertEquals(Target.HIDDEN_POWDER_SNOW,
                VisualCapabilityCatalog.selectableTarget(
                        "minecraft:powder_snow", BlockInspectionCategory.HIDDEN_SURFACE));
        assertEquals(Target.HIDDEN_SCULK_CATALYST,
                VisualCapabilityCatalog.selectableTarget(
                        "minecraft:sculk_catalyst", BlockInspectionCategory.HIDDEN_SURFACE));
        assertEquals(Target.HIDDEN_DEAD_CORAL,
                VisualCapabilityCatalog.selectableTarget(
                        "example:dead_brain_coral_block", BlockInspectionCategory.HIDDEN_SURFACE));

        assertNull(VisualCapabilityCatalog.selectableTarget(
                "minecraft:netherrack", BlockInspectionCategory.NETHER_PALETTE));
        assertNull(VisualCapabilityCatalog.selectableTarget(
                "minecraft:stone", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertNull(VisualCapabilityCatalog.selectableTarget(
                null, BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertNull(VisualCapabilityCatalog.selectableTarget(
                "minecraft:diamond_ore", null));
        assertNull(VisualCapabilityCatalog.selectableTarget(
                "minecraft:diamond_ore", BlockInspectionCategory.NONE));
    }

    @Test
    void hiddenDeadCoralCompatibilityKeepsAllRetainedSuffixesWithoutMatchingLiveCoral() {
        assertEquals(Target.HIDDEN_DEAD_CORAL,
                hidden("minecraft:dead_fire_coral_block"));
        assertEquals(Target.HIDDEN_DEAD_CORAL,
                hidden("minecraft:dead_fire_coral"));
        assertEquals(Target.HIDDEN_DEAD_CORAL,
                hidden("minecraft:dead_fire_coral_fan"));
        assertEquals(Target.HIDDEN_DEAD_CORAL,
                hidden("minecraft:dead_fire_coral_wall_fan"));
        assertNull(hidden("minecraft:fire_coral_block"));
        assertNull(hidden("minecraft:dead_fire_coral_wall"));
        assertNull(hidden("minecraft:stone"));
    }

    @Test
    void retainedCatalogSizesAndMatchingRemainStable() {
        assertEquals(VanillaOreVisualCatalog.blockVariantCount() + 2,
                VisualCapabilityCatalog.materialHighlightIds().size());
        assertTrue(VisualCapabilityCatalog.materialHighlightIds()
                .containsAll(VanillaOreVisualCatalog.blockIds()));
        assertTrue(VisualCapabilityCatalog.materialHighlightIds().contains("minecraft:obsidian"));
        assertTrue(VisualCapabilityCatalog.materialHighlightIds().contains("minecraft:crying_obsidian"));

        assertEquals(27, VisualCapabilityCatalog.netherPaletteIds().size());
        assertTrue(VisualCapabilityCatalog.netherPaletteIds().contains("minecraft:netherrack"));
        assertTrue(VisualCapabilityCatalog.netherPaletteIds().contains("minecraft:polished_basalt"));
        assertTrue(VisualCapabilityCatalog.netherPaletteIds().contains("minecraft:nether_gold_ore"));
        assertTrue(VisualCapabilityCatalog.netherPaletteIds().contains("minecraft:crying_obsidian"));

        assertTrue(VisualCapabilityCatalog.matches(
                "minecraft:tripwire_hook", BlockInspectionCategory.TECHNICAL_TRACE));
        assertTrue(VisualCapabilityCatalog.matches(
                "minecraft:dead_horn_coral_wall_fan", BlockInspectionCategory.HIDDEN_SURFACE));
        assertTrue(VisualCapabilityCatalog.matches(
                "minecraft:ancient_debris", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertTrue(VisualCapabilityCatalog.matches(
                "minecraft:warped_wart_block", BlockInspectionCategory.NETHER_PALETTE));
        assertFalse(VisualCapabilityCatalog.matches(
                "minecraft:stone", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(VisualCapabilityCatalog.matches(
                "minecraft:netherrack", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(VisualCapabilityCatalog.matches(
                "minecraft:netherrack", BlockInspectionCategory.NONE));
        assertFalse(VisualCapabilityCatalog.matches("minecraft:netherrack", null));
        assertFalse(VisualCapabilityCatalog.matches(null, BlockInspectionCategory.NETHER_PALETTE));
    }

    @Test
    void p0PoliciesDelegateToOneCatalogInsteadOfOwningDuplicateTargetLists() throws IOException {
        String inspection = source("src/main/java/dev/chise/chisetweaks/core/vision/BlockInspectionPolicy.java");
        String selection = source("src/main/java/dev/chise/chisetweaks/core/vision/VisualTargetSelectionPolicy.java");
        String catalog = source("src/main/java/dev/chise/chisetweaks/core/vision/VisualCapabilityCatalog.java");

        assertTrue(inspection.contains("VisualCapabilityCatalog.categories(id)"));
        assertTrue(inspection.contains("VisualCapabilityCatalog.matches(id, category)"));
        assertTrue(inspection.contains("VisualCapabilityCatalog.materialHighlightIds()"));
        assertTrue(inspection.contains("VisualCapabilityCatalog.netherPaletteIds()"));
        assertFalse(inspection.contains("NETHER_PALETTE_IDS = Set.of"));
        assertFalse(inspection.contains("createMaterialHighlightIds"));
        assertFalse(inspection.contains("private static boolean isTechnical"));
        assertFalse(inspection.contains("private static boolean isHiddenSurface"));

        assertTrue(selection.contains("VisualCapabilityCatalog.selectableTarget"));
        assertTrue(selection.contains("VisualCapabilityCatalog.matches(rawBlockId, category)"));
        assertFalse(selection.contains("private static boolean technicalEnabled"));
        assertFalse(selection.contains("private static boolean materialEnabled"));
        assertFalse(selection.contains("private static boolean hiddenEnabled"));

        assertTrue(catalog.contains("VanillaOreVisualCatalog.blockIds()"));
        assertTrue(catalog.contains("NETHER_PALETTE_IDS = Set.of"));
        assertTrue(catalog.contains("TECHNICAL_TRACE_IDS = Set.of"));
    }

    private static Target hidden(String blockId) {
        return VisualCapabilityCatalog.selectableTarget(
                blockId, BlockInspectionCategory.HIDDEN_SURFACE);
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
