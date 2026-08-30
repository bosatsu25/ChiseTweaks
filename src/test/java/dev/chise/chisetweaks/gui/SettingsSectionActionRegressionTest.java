package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 旧section遷移と旧footer操作が設定画面へ再混入しないためのsource contract。 */
final class SettingsSectionActionRegressionTest {
    private static final Path GUI_ROOT = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void settingsScreenContainsNoLegacySectionNavigationOrBulkImplementation() throws Exception {
        String screen = Files.readString(GUI_ROOT.resolve("ChiseTweaksConfigScreen.java"));
        String controller = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsController.java"));
        String layout = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsLayout.java"));
        String definition = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingRowDefinition.java"));

        for (String removed : new String[]{
                "OPEN_HIGHLIGHT_DETAILS", "OPEN_VISUAL_FILTER_DETAILS", "OPEN_LAVA_DETAILS",
                "HIGHLIGHT_DETAILS", "VISUAL_FILTER_DETAILS", "LAVA_DETAILS",
                "bulkButton", "toggleHighlightBulk", "shouldTurnHighlightBulkOn",
                "geometry.helpButton()", "geometry.resetButton()", "geometry.applyButton()"}) {
            assertFalse(screen.contains(removed), removed);
            assertFalse(controller.contains(removed), removed);
            assertFalse(layout.contains(removed), removed);
            assertFalse(definition.contains(removed), removed);
        }
    }

    @Test
    void screenUsesSixTabsAndOneContextualSettingsButton() throws Exception {
        String screen = Files.readString(GUI_ROOT.resolve("ChiseTweaksConfigScreen.java"));
        String controller = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsController.java"));
        String layout = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsLayout.java"));

        assertTrue(controller.contains("HIGHLIGHT"));
        assertTrue(controller.contains("FILTER"));
        assertTrue(controller.contains("INSPECTOR"));
        assertTrue(controller.contains("ANALYZER"));
        assertTrue(controller.contains("VISIBILITY"));
        assertFalse(controller.contains("VISUAL_FILTER"));
        assertFalse(controller.contains("HELP"));
        assertTrue(layout.contains("TAB_COUNT = 6"));
        assertTrue(screen.contains("screen.chisetweaks.settings.apply_changes"));
        assertTrue(screen.contains("screen.chisetweaks.settings.reset_all"));
        assertTrue(screen.contains("SettingChangeDispatcher.revision()"));
        assertTrue(screen.contains("ensureSurfaceRows("));
        assertTrue(screen.contains("reusableInspectorRow("));
        assertFalse(screen.contains("createAllRows()"));
        assertTrue(screen.contains("ChiseTweaksMetadata.MOD_NAME"));
        assertTrue(screen.contains("CrosshairInspector"));
        assertFalse(screen.contains("ChiseTweaksMetadata.MOD_VERSION"));
    }
}
