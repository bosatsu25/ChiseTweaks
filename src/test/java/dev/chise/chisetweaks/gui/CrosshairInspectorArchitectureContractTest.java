package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrosshairInspectorArchitectureContractTest {
    private static final Path GUI = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void inspectorReusesTheExistingHitAndCachesUnchangedState() throws Exception {
        String inspector = Files.readString(GUI.resolve("CrosshairInspector.java"));
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));

        assertTrue(inspector.contains("client.hitResult"));
        assertTrue(inspector.contains("cachedBlockState == state"));
        assertTrue(inspector.contains("cachedFilterRevision == filterRevision"));
        assertTrue(screen.contains("inspector.refresh(minecraft)"));
        assertTrue(screen.contains("rebuildInspectorRows()"));
        assertTrue(screen.contains("row.removeWidgets(this::removeWidget)"));
        assertFalse(inspector.contains(".clip("));
        assertFalse(inspector.contains("raycast("));
        assertFalse(inspector.contains("pick("));
    }

    @Test
    void inspectorIsReadOnlyAndDoesNotAcquirePrivateBlockEntityOrPlayerData() throws Exception {
        String inspector = Files.readString(GUI.resolve("CrosshairInspector.java"));
        for (String forbidden : new String[]{
                "setBlock(", "setValue(", "toggleBooleanValue(", "saveToFile(",
                "send(", "sendPacket", "getBlockEntity(", "getUpdateTag(", "saveWith",
                "getNbt", "getComponents(", "getUUID(", "getInventory", "getContainer",
                "getMessage", "getText", "chat"}) {
            assertFalse(inspector.contains(forbidden), forbidden);
        }
        assertFalse(inspector.contains("LOGGER"));
    }

    @Test
    void helpSurfaceMigratesToInspectorWithoutRestoringLegacyUtilityChrome() throws Exception {
        String controller = Files.readString(GUI.resolve("ChiseTweaksSettingsController.java"));
        String inspectorRows = Files.readString(GUI.resolve("InspectorSettingsRows.java"));
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));

        assertTrue(controller.contains("INSPECTOR"));
        assertFalse(controller.contains("HELP"));
        assertFalse(controller.contains("VISUAL_FILTER"));
        assertTrue(inspectorRows.contains("addCommonHelpRows"));
        assertTrue(screen.contains("inspectorHelpVisible"));
        assertFalse(screen.contains("ChiseTweaksMetadata.MOD_VERSION"));
        assertFalse(screen.contains("Diagnostics"));
        assertFalse(screen.contains("Resource reload"));
    }
}
