package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsResponsibilityArchitectureContractTest {
    private static final Path GUI = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void controllerCoordinatesSettingsWithoutOwningMinecraftWidgetsOrPresentationBuilders() throws Exception {
        String controller = Files.readString(GUI.resolve("ChiseTweaksSettingsController.java"));

        assertTrue(controller.contains("TweaksProductSettingsRows"));
        assertTrue(controller.contains("TweaksBuilderAssistRows"));
        assertTrue(controller.contains("SettingPersistenceCoordinator"));

        for (String presentation : new String[]{
                "net.minecraft.client.gui.components.Button",
                "net.minecraft.client.gui.screens.Screen",
                "GuiGraphicsExtractor",
                "Tooltip",
                "Component.translatable(",
                "headerLiteral(",
                "addHighlightRows(",
                "addAnalyzerRows(",
                "addPlacementRows("}) {
            assertFalse(controller.contains(presentation),
                    () -> "controller owns presentation detail: " + presentation);
        }
    }

    @Test
    void rowModelsRemainPresentationOnly() throws Exception {
        String rows = Files.readString(GUI.resolve("TweaksProductSettingsRows.java"));
        String assist = Files.readString(GUI.resolve("TweaksBuilderAssistRows.java"));
        String inspector = Files.readString(GUI.resolve("InspectorSettingsRows.java"));

        assertTrue(rows.contains("ChiseTweaksSettingRowDefinition"));
        assertTrue(assist.contains("ChiseTweaksSettingRowDefinition"));
        assertTrue(inspector.contains("ChiseTweaksSettingRowDefinition"));

        for (String persistence : new String[]{
                "SettingPersistenceCoordinator",
                "savePendingConfig(",
                "pendingDomainsForDiagnostics(",
                "SecureConfigStorage",
                ".save()"}) {
            assertFalse(rows.contains(persistence),
                    () -> "static settings rows own persistence: " + persistence);
            assertFalse(assist.contains(persistence),
                    () -> "builder assist rows own persistence: " + persistence);
            assertFalse(inspector.contains(persistence),
                    () -> "inspector rows own persistence: " + persistence);
        }
    }

    @Test
    void screenOwnsWidgetLifecycleButNotConfigDomainPersistence() throws Exception {
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));

        assertTrue(screen.contains("extends Screen"));
        assertTrue(screen.contains("ChiseTweaksSettingsController"));
        assertFalse(screen.contains("SettingPersistenceCoordinator"));
        assertFalse(screen.contains("LocalFeatureConfig.getInstance()"));
        assertFalse(screen.contains("MasaIntegrationConfig.getInstance()"));
        assertFalse(screen.contains("CompatibilityIntegrationConfig.getInstance()"));
        assertFalse(screen.contains("SecureConfigStorage"));
    }
}
