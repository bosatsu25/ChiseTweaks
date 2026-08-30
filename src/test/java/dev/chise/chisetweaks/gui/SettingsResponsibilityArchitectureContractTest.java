package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsResponsibilityArchitectureContractTest {
    private static final Path GUI = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void controllerOwnsMutationPersistenceButNotPresentation() throws Exception {
        String controller = Files.readString(GUI.resolve("ChiseTweaksSettingsController.java"));

        assertTrue(controller.contains("SettingPersistenceCoordinator.production()"));
        assertTrue(controller.contains("EnumSet<SettingPersistence> reset("));
        assertTrue(controller.contains("SaveResult saveConfig("));
        assertTrue(controller.contains("return ChiseTweaksSettingsRows.rows("));
        assertTrue(controller.contains("return InspectorSettingsRows.rows("));

        for (String presentation : new String[]{
                "addHighlightRows(",
                "addFilterRows(",
                "addAnalyzerRows(",
                "addVisibilityRows(",
                "addIntegrationRows(",
                "addCompatibilityRows(",
                "addPlacementRows(",
                "addPatternConsistencyRows(",
                "headerLiteral(",
                "Component.translatable("}) {
            assertFalse(controller.contains(presentation), presentation);
        }
    }

    @Test
    void staticAndDynamicPresentationStaySeparated() throws Exception {
        String rows = Files.readString(GUI.resolve("ChiseTweaksSettingsRows.java"));
        String inspector = Files.readString(GUI.resolve("InspectorSettingsRows.java"));

        assertTrue(rows.contains("addHighlightRows("));
        assertTrue(rows.contains("addFilterRows("));
        assertTrue(rows.contains("addAnalyzerRows("));
        assertTrue(rows.contains("addVisibilityRows("));
        assertTrue(rows.contains("addIntegrationRows("));
        assertTrue(rows.contains("addCompatibilityRows("));
        assertTrue(rows.contains("InspectorSettingsRows.addRows("));

        assertTrue(inspector.contains("addPlacementRows("));
        assertTrue(inspector.contains("addSchematicPlacementRows("));
        assertTrue(inspector.contains("addPatternConsistencyRows("));
        assertTrue(inspector.contains("addInteractionHistoryRows("));
        assertTrue(inspector.contains("addCommonHelpRows("));

        assertFalse(rows.contains("SettingPersistenceCoordinator"));
        assertFalse(rows.contains("resetHighlightFeatures("));
        assertFalse(inspector.contains("SettingPersistenceCoordinator"));
        assertFalse(inspector.contains("FeatureSwitches."));
    }
}
