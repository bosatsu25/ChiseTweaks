package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimplePlacementPreviewTest {
    private static final Path PLACEMENT = Path.of(
            "src/main/java/dev/chise/chisetweaks/gui/PlacementInspector.java");

    @Test
    void disabledPreviewDoesNotReadRuntimeContext() {
        var disabled = PlacementInspector.predictPlacementState(
                null, null, null, null, null, false);

        assertNull(disabled);
    }

    @Test
    void descriptorKeepsPredictionSeparateFromActualPlacement() {
        List<ChiseTweaksSettingRowDefinition> rows = new ChiseTweaksSettingsController().inspectorRows(
                CrosshairInspector.Snapshot.noTarget(), false);

        assertTrue(rows.stream().anyMatch(row -> row.id().equals("placement.none")));
        assertFalse(rows.stream().anyMatch(row -> row.id().equals("placement.predicted")));
    }

    @Test
    void supportedFamiliesUseTypesTagsAndPropertiesInsteadOfBlockIdLists() throws Exception {
        String source = Files.readString(PLACEMENT);

        assertTrue(source.contains("instanceof TrapDoorBlock"));
        assertTrue(source.contains("instanceof SlabBlock"));
        for (String family : new String[]{
                "StairBlock", "GlazedTerracottaBlock", "FenceGateBlock", "GrindstoneBlock",
                "BeehiveBlock", "CampfireBlock"}) {
            assertTrue(source.contains("instanceof " + family), family);
        }
        assertTrue(source.contains("state.is(BlockTags.LOGS)"));
        assertTrue(source.contains("endsWith(\"_froglight\")"));
        for (String property : new String[]{
                "AXIS", "SLAB_TYPE", "HORIZONTAL_FACING", "HALF", "STAIRS_SHAPE", "OPEN",
                "POWERED", "IN_WALL", "ATTACH_FACE", "LEVEL_HONEY", "LIT", "SIGNAL_FIRE",
                "WATERLOGGED"}) {
            assertTrue(source.contains("BlockStateProperties." + property), property);
        }
        assertFalse(source.contains("oak_trapdoor\""));
        assertFalse(source.contains("oak_log\""));
        assertFalse(source.contains("ochre_froglight\""));
    }

    @Test
    void productionPreviewUsesVanillaPlacementWithoutMutationOrInputInjection() throws Exception {
        String placement = Files.readString(PLACEMENT);
        String crosshair = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/gui/CrosshairInspector.java"));

        assertTrue(placement.contains("getStateForPlacement(context)"));
        assertTrue(placement.contains("BlockPlaceContext"));
        assertFalse(crosshair.contains("BlockPlaceContext"));
        assertTrue(crosshair.contains("snapshot.predictedPlacement() == predictedPlacement"));
        assertTrue(crosshair.contains("snapshot.clickedFace() == clickedFace"));
        assertTrue(crosshair.contains("snapshot.upperClick() == upperClick"));

        String catalog = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/gui/InspectorSettingsRows.java"));
        assertTrue(catalog.contains("PlacementInspector.placementStateProperties(state)"));
        assertTrue(catalog.contains("PlacementInspector.actualPlacementStateProperties"));

        for (String forbidden : new String[]{
                "setBlock(", "setBlockAndUpdate(", ".place(", "sendPacket", "send(",
                "clickMouse", "pressMouse", "keyPress", ".clip(", "raycast(", "LOGGER"}) {
            assertFalse(placement.contains(forbidden), forbidden);
            assertFalse(crosshair.contains(forbidden), forbidden);
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
                "PEARLESCENT_FROGLIGHT", "OAK_SLAB", "OAK_STAIRS", "WHITE_GLAZED_TERRACOTTA",
                "OAK_FENCE_GATE", "GRINDSTONE", "BEEHIVE", "BEE_NEST", "CAMPFIRE",
                "STONE", "QUARTZ_PILLAR"}) {
            assertTrue(source.contains("Blocks." + block), block);
        }
        assertTrue(source.contains("new ItemStack(Items.STICK)"));
        assertTrue(source.contains("setShiftKeyDown(sneaking)"));
        assertTrue(source.contains("PlacementInspector.predictPlacementState"));
        assertTrue(source.contains(".place(actualContext)"));
        assertTrue(source.contains("TrapDoorBlock.FACING"));
        assertTrue(source.contains("BlockStateProperties.AXIS"));
        assertTrue(source.contains("BlockStateProperties.SLAB_TYPE"));
        assertTrue(source.contains("BlockStateProperties.WATERLOGGED"));
        assertTrue(source.contains("StairsShape.OUTER_LEFT"));
        assertTrue(source.contains("StairsShape.INNER_LEFT"));
        assertTrue(source.contains("AttachFace.FLOOR"));
        assertTrue(source.contains("AttachFace.WALL"));
        assertTrue(source.contains("AttachFace.CEILING"));
    }
}
