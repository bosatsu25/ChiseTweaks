package dev.chise.chisetweaks.gui;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrapdoorPlacementPreviewTest {
    @Test
    void disabledPreviewDoesNotReadRuntimeContext() {
        var disabled = CrosshairInspector.predictTrapdoorState(
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
    void trapdoorFamilyIsSelectedByVanillaTypeRatherThanAnIdList() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/gui/CrosshairInspector.java"));

        assertTrue(source.contains("instanceof TrapDoorBlock"));
        assertFalse(source.contains("oak_trapdoor\""));
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
        assertTrue(catalog.contains("CrosshairInspector.stateProperties(state)"));
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
        assertTrue(source.contains("new ItemStack(Items.STICK)"));
        assertTrue(source.contains("new ItemStack(Blocks.OAK_TRAPDOOR)"));
        assertTrue(source.contains("Blocks.IRON_TRAPDOOR"));
        assertTrue(source.contains("setShiftKeyDown(sneaking)"));
        assertTrue(source.contains("predictTrapdoorState"));
        assertTrue(source.contains(".place(actualContext)"));
        assertTrue(source.contains("TrapDoorBlock.FACING"));
        assertTrue(source.contains("TrapDoorBlock.HALF"));
        assertTrue(source.contains("TrapDoorBlock.OPEN"));
        assertTrue(source.contains("TrapDoorBlock.WATERLOGGED"));
    }
}
