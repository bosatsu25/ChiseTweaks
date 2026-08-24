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
    void screenUsesFiveTabsAndOneContextualSettingsButton() throws Exception {
        String screen = Files.readString(GUI_ROOT.resolve("ChiseTweaksConfigScreen.java"));
        String controller = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsController.java"));
        String layout = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsLayout.java"));

        assertTrue(controller.contains("HIGHLIGHT"));
        assertTrue(controller.contains("VISUAL_FILTER"));
        assertTrue(controller.contains("ANALYZER"));
        assertTrue(controller.contains("VISIBILITY"));
        assertTrue(controller.contains("HELP"));
        assertTrue(layout.contains("TAB_COUNT = 5"));
        assertTrue(screen.contains("設定を適用"));
        assertTrue(screen.contains("設定をリセット"));
        assertTrue(screen.contains("ChiseTweaksMetadata.MOD_NAME"));
        assertFalse(screen.contains("ChiseTweaksMetadata.MOD_VERSION"));
    }
}
