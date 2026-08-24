package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimplePlacementPreviewTest {
    @Test
    void disabledPreviewDoesNotReadRuntimeContext() {
        var disabled = CrosshairInspector.predictPlacementState(
                null, null, null, null, null, false);

        assertNull(disabled);
    }

    @Test
    void descriptorKeepsPredictionSeparateFromActualPlacement() {
        List<ChiseTweaksSettingRowDefinition> rows = new ChiseTweaksSettingsCatalog().inspectorRows(
                CrosshairInspector.Snapshot.noTarget(), false);

        assertTrue(rows.stream().anyMatch(row -> row.id().equals("placement.none")));
        assertFalse(rows.stream().anyMatch(row -> row.id().equals("placement.predicted")));
    }

    @Test
    void supportedFamiliesUseTypesTagsAndPropertiesInsteadOfBlockIdLists() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/gui/CrosshairInspector.java"));

        assertTrue(source.contains("instanceof TrapDoorBlock"));
        assertTrue(source.contains("instanceof SlabBlock"));
        assertTrue(source.contains("state.is(BlockTags.LOGS)"));
        assertTrue(source.contains("endsWith(\"_froglight\")"));
        for (String property : new String[]{
                "AXIS", "SLAB_TYPE", "HORIZONTAL_FACING", "HALF", "OPEN", "POWERED", "WATERLOGGED"}) {
            assertTrue(source.contains("BlockStateProperties." + property), property);
        }
        assertFalse(source.contains("oak_trapdoor\""));
        assertFalse(source.contains("oak_log\""));
        assertFalse(source.contains("ochre_froglight\""));
    }

    @Test
    void productionPreviewUsesVanillaPlacementWithoutMutationOrInputInjection() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/gui/CrosshairInspector.java"));

        assertTrue(source.contains("getStateForPlacement(context)"));
        assertTrue(source.contains("BlockPlaceContext"));
        assertTrue(source.contains("BlockState predictedPlacement"));
        assertFalse(source.contains("List<BlockState>"));
        assertTrue(source.contains("snapshot.predictedPlacement() == predictedPlacement"));
        assertTrue(source.contains("snapshot.clickedFace() == clickedFace"));
        assertTrue(source.contains("snapshot.upperClick() == upperClick"));
        String catalog = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java"));
        assertTrue(catalog.contains("CrosshairInspector.placementStateProperties(state)"));
        for (String forbidden : new String[]{
                "setBlock(", "setBlockAndUpdate(", ".place(", "sendPacket", "send(",
                "clickMouse", "pressMouse", "keyPress", ".clip(", "raycast(", "LOGGER"}) {
            assertFalse(source.contains(forbidden), forbidden);
        }
    }

    @Test
    void clientGameTestCoversTheRequiredVanillaOracleMatrix() throws Exception {
        String source = Files.readString(Path.of(
                "src/gametest/java/dev/chise/chisetweaks/gui/TrapdoorPlacementClientGameTest.java"));

        for (String direction : new String[]{"NORTH", "EAST", "SOUTH", "WEST", "UP", "DOWN"}) {
            assertTrue(source.contains("Direction." + direction), direction);
        }
        for (String target : new String[]{"SOLID", "REPLACEABLE", "WATER", "BLOCKED"}) {
            assertTrue(source.contains("TargetKind." + target), target);
        }
        for (String block : new String[]{
                "OAK_TRAPDOOR", "IRON_TRAPDOOR", "OAK_LOG", "OAK_WOOD", "CRIMSON_STEM",
                "CRIMSON_HYPHAE", "OCHRE_FROGLIGHT", "VERDANT_FROGLIGHT",
                "PEARLESCENT_FROGLIGHT", "OAK_SLAB", "STONE", "QUARTZ_PILLAR"}) {
            assertTrue(source.contains("Blocks." + block), block);
        }
        assertTrue(source.contains("new ItemStack(Items.STICK)"));
        assertTrue(source.contains("setShiftKeyDown(sneaking)"));
        assertTrue(source.contains("predictPlacementState"));
        assertTrue(source.contains(".place(actualContext)"));
        assertTrue(source.contains("TrapDoorBlock.FACING"));
        assertTrue(source.contains("BlockStateProperties.AXIS"));
        assertTrue(source.contains("BlockStateProperties.SLAB_TYPE"));
        assertTrue(source.contains("BlockStateProperties.WATERLOGGED"));
    }
}
