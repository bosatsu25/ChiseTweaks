package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VanillaOreVisualCatalog;
import dev.chise.chisetweaks.core.vision.VisualModelSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VanillaOreReplacementContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path GENERATED_MODELS = ROOT.resolve(
            "build/generated/chiseVisualAssets/assets/chisetweaks/models/block/visual/material");

    private static final Set<String> EXPECTED_VANILLA_ORE_BLOCKS = Set.of(
            "minecraft:coal_ore",
            "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore",
            "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore",
            "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore",
            "minecraft:deepslate_gold_ore",
            "minecraft:lapis_ore",
            "minecraft:deepslate_lapis_ore",
            "minecraft:redstone_ore",
            "minecraft:deepslate_redstone_ore",
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:deepslate_emerald_ore",
            "minecraft:nether_gold_ore",
            "minecraft:nether_quartz_ore",
            "minecraft:ancient_debris");

    @Test
    void vanillaOreCatalogCoversEverySupportedVanillaOreVariantExactlyOnce() {
        assertEquals(11, VanillaOreVisualCatalog.familyCount());
        assertEquals(19, VanillaOreVisualCatalog.blockVariantCount());
        assertEquals(EXPECTED_VANILLA_ORE_BLOCKS, VanillaOreVisualCatalog.blockIds());

        LinkedHashSet<String> observed = new LinkedHashSet<>();
        int expectedTargetMask = 0;
        for (var family : VanillaOreVisualCatalog.families()) {
            assertNotNull(family.target());
            assertFalse(family.highlightKey().isBlank());
            expectedTargetMask |= family.target().bitMask();
            for (String blockId : family.blockIds()) {
                assertTrue(observed.add(blockId), "duplicate: " + blockId);
                assertEquals(family.target(), VanillaOreVisualCatalog.targetForBlockId(blockId));
                assertEquals(family.highlightKey(), VanillaOreVisualCatalog.highlightKeyForBlockId(blockId));
            }
        }
        assertEquals(EXPECTED_VANILLA_ORE_BLOCKS, observed);
        assertEquals(expectedTargetMask, VanillaOreVisualCatalog.TARGET_MASK);
        assertEquals(11, Integer.bitCount(VanillaOreVisualCatalog.TARGET_MASK));
    }

    @Test
    void catalogLookupNormalizesInputAndRejectsUnknownBlocksSafely() {
        assertEquals(
                Target.MATERIAL_DIAMOND_ORE,
                VanillaOreVisualCatalog.targetForBlockId("  MINECRAFT:DIAMOND_ORE  "));
        assertEquals("diamond", VanillaOreVisualCatalog.highlightKeyForBlockId("Minecraft:Diamond_Ore"));
        assertTrue(VanillaOreVisualCatalog.isVanillaOreBlock("minecraft:ancient_debris"));
        assertFalse(VanillaOreVisualCatalog.isVanillaOreBlock("minecraft:stone"));
        assertFalse(VanillaOreVisualCatalog.isVanillaOreBlock(null));
        assertNull(VanillaOreVisualCatalog.targetForBlockId(null));
        assertNull(VanillaOreVisualCatalog.targetForBlockId("minecraft:stone"));
        assertEquals("", VanillaOreVisualCatalog.highlightKeyForBlockId(null));
        assertEquals("", VanillaOreVisualCatalog.highlightKeyForBlockId("minecraft:stone"));
    }

    @Test
    void familyDefinitionRejectsInvalidCatalogEntries() {
        assertThrows(IllegalArgumentException.class,
                () -> new VanillaOreVisualCatalog.Family(null, "diamond", List.of("minecraft:diamond_ore")));
        assertThrows(IllegalArgumentException.class,
                () -> new VanillaOreVisualCatalog.Family(Target.MATERIAL_DIAMOND_ORE, " ", List.of("minecraft:diamond_ore")));
        assertThrows(IllegalArgumentException.class,
                () -> new VanillaOreVisualCatalog.Family(Target.MATERIAL_DIAMOND_ORE, "diamond", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new VanillaOreVisualCatalog.Family(Target.MATERIAL_DIAMOND_ORE, "diamond", List.of("example:diamond_ore")));
    }

    @Test
    void everyCatalogOreIsClassifiedAndSelectableByTheRuntimePath() {
        int allTargets = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        for (String blockId : EXPECTED_VANILLA_ORE_BLOCKS) {
            assertTrue(
                    BlockInspectionPolicy.matches(blockId, BlockInspectionCategory.MATERIAL_HIGHLIGHT),
                    blockId);
            assertTrue(
                    VisualTargetSelectionPolicy.matchesEnabled(
                            allTargets,
                            blockId,
                            BlockInspectionCategory.MATERIAL_HIGHLIGHT),
                    blockId);
        }
    }

    @Test
    void vanillaOreModelMaskIsCompleteButDoesNotAbsorbSpecialMaterials() {
        assertEquals(
                VanillaOreVisualCatalog.TARGET_MASK,
                VisualModelSelectionPolicy.VANILLA_ORE_MODEL_TARGET_MASK);
        assertTrue(VisualModelSelectionPolicy.useAllVanillaOres(
                VisualModelSelectionPolicy.VANILLA_ORE_MODEL_TARGET_MASK));
        assertFalse(VisualModelSelectionPolicy.useAllVanillaOres(
                VisualModelSelectionPolicy.VANILLA_ORE_MODEL_TARGET_MASK
                        & ~Target.MATERIAL_COAL_ORE.bitMask()));

        assertEquals(0,
                VisualModelSelectionPolicy.VANILLA_ORE_MODEL_TARGET_MASK
                        & Target.MATERIAL_OBSIDIAN.bitMask());
        assertEquals(0,
                VisualModelSelectionPolicy.VANILLA_ORE_MODEL_TARGET_MASK
                        & Target.MATERIAL_CRYING_OBSIDIAN.bitMask());
    }

    @Test
    void modelPluginRegistersEveryVanillaOreVariant() throws IOException {
        String plugin = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java"));

        for (String blockId : EXPECTED_VANILLA_ORE_BLOCKS) {
            String blockConstant = blockId.substring("minecraft:".length())
                    .toUpperCase(Locale.ROOT);
            assertTrue(plugin.contains("Blocks." + blockConstant), blockId);
        }
    }

    @Test
    void generatedChiseModelsKeepEveryVanillaOreBaseTexture() throws IOException {
        for (String blockId : EXPECTED_VANILLA_ORE_BLOCKS) {
            String modelId = blockId.substring("minecraft:".length());
            Path model = GENERATED_MODELS.resolve(modelId + ".json");
            assertTrue(Files.exists(model), modelId);
            String json = Files.readString(model);
            assertTrue(json.contains("chisetweaks:block/visual/material/"), modelId);
            if (modelId.equals("ancient_debris")) {
                assertTrue(json.contains("minecraft:block/ancient_debris_top"));
                assertTrue(json.contains("minecraft:block/ancient_debris_side"));
            } else {
                assertTrue(json.contains("minecraft:block/" + modelId), modelId);
            }
        }
    }

    @Test
    void nonOreReferenceMaterialsStayOutsideTheVanillaOreContract() {
        for (String blockId : Set.of(
                "minecraft:obsidian",
                "minecraft:crying_obsidian",
                "minecraft:raw_iron_block",
                "minecraft:raw_copper_block",
                "minecraft:raw_gold_block",
                "minecraft:suspicious_sand",
                "minecraft:suspicious_gravel")) {
            assertFalse(VanillaOreVisualCatalog.isVanillaOreBlock(blockId), blockId);
        }
    }
}
