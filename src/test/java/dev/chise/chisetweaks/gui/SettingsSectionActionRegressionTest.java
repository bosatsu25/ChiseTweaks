package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 一括操作UIと行別設定ボタンがmain画面へ再混入しないためのsource contract。 */
final class SettingsSectionActionRegressionTest {
    private static final Path GUI_ROOT = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void mainSettingsScreenContainsNoHighlightBulkImplementation() throws Exception {
        String screen = Files.readString(GUI_ROOT.resolve("ChiseTweaksConfigScreen.java"));
        String controller = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsController.java"));
        String layout = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsLayout.java"));

        for (String removed : new String[]{
                "bulkButton", "toggleHighlightBulk", "shouldTurnHighlightBulkOn",
                "bulkMessage", "geometry.bulk()"}) {
            assertFalse(screen.contains(removed), removed);
            assertFalse(controller.contains(removed), removed);
            assertFalse(layout.contains(removed), removed);
        }
    }

    @Test
    void visualFilterUsesOneHeaderSettingsActionAndPlainMainToggles() throws Exception {
        String catalog = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingsCatalog.java"));
        String definition = Files.readString(GUI_ROOT.resolve("ChiseTweaksSettingRowDefinition.java"));

        assertTrue(catalog.contains("OPEN_VISUAL_FILTER_DETAILS"));
        assertTrue(catalog.contains("compactFeature(rows, \"focusBlocks\""));
        assertTrue(catalog.contains("compactFeature(rows, \"focusEntities\""));
        assertFalse(catalog.contains("compactFeatureAction"));
        assertFalse(definition.contains("BOOLEAN_ACTION"));
        assertFalse(definition.contains("boolAction("));
    }
}
