package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsResponsibilityArchitectureContractTest {
    private static final Path GUI = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void controllerCoordinatesSettingsWithoutOwningPresentationBuilders() throws Exception {
        String controller = Files.readString(GUI.resolve("ChiseTweaksSettingsController.java"));
        assertTrue(controller.contains("ChiseTweaksSettingsRows"));
        assertTrue(controller.contains("BuilderAssistRows"));
        assertTrue(controller.contains("WorkflowRows"));
        assertTrue(controller.contains("SettingPersistenceCoordinator"));

        for (String presentation : new String[]{
                "net.minecraft.client.gui.components.Button",
                "GuiGraphicsExtractor", "Tooltip", "Component.translatable("}) {
            assertFalse(controller.contains(presentation), presentation);
        }
    }

    @Test
    void rowModelsRemainPresentationOnly() throws Exception {
        String combined = Files.readString(GUI.resolve("ChiseTweaksSettingsRows.java"))
                + Files.readString(GUI.resolve("BuilderAssistRows.java"))
                + Files.readString(GUI.resolve("WorkflowRows.java"));
        for (String persistence : new String[]{
                "SettingPersistenceCoordinator", "savePendingConfig(", "SecureConfigStorage", ".save()"}) {
            assertFalse(combined.contains(persistence), persistence);
        }
    }

    @Test
    void screenOwnsWidgetLifecycleButNotConfigPersistence() throws Exception {
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));
        assertTrue(screen.contains("extends Screen"));
        assertTrue(screen.contains("ChiseTweaksSettingsController"));
        assertFalse(screen.contains("SettingPersistenceCoordinator"));
        assertFalse(screen.contains("LocalFeatureConfig.getInstance()"));
        assertFalse(screen.contains("SecureConfigStorage"));
    }
}
