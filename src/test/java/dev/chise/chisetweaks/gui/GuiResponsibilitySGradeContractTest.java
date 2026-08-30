package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2: presentation surfaces must not reacquire domain persistence/registry responsibilities. */
final class GuiResponsibilitySGradeContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void settingsScreenRemainsPresentationAndNavigationOnly() throws IOException {
        String screen = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");

        assertTrue(screen.contains("ChiseTweaksSettingsController controller"));
        assertTrue(screen.contains("controller.rows(candidate)"));
        assertTrue(screen.contains("controller.inspectorRows("));
        assertTrue(screen.contains("SettingPersistence"));

        for (String forbidden : List.of(
                "LocalFeatureConfig",
                "MasaIntegrationConfig",
                "CompatibilityIntegrationConfig",
                "BuiltInRegistries",
                "FabricLoader.getInstance()",
                "GsonBuilder",
                "Files.write",
                "Files.read")) {
            assertFalse(screen.contains(forbidden), () -> "Settings screen regained domain responsibility: " + forbidden);
        }
    }

    @Test
    void staticAndDynamicRowsRemainOutsideTheScreen() throws IOException {
        String controller = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String staticRows = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsRows.java");
        String inspectorRows = source("src/main/java/dev/chise/chisetweaks/gui/InspectorSettingsRows.java");

        assertTrue(controller.contains("ChiseTweaksSettingsRows"));
        assertTrue(controller.contains("InspectorSettingsRows"));
        assertTrue(staticRows.contains("FeatureSwitches."));
        assertTrue(inspectorRows.contains("CrosshairInspector"));
    }

    @Test
    void listEditorKeepsDomainMutationsInBackends() throws IOException {
        String screen = source("src/main/java/dev/chise/chisetweaks/gui/ChiseListEditorScreen.java");

        assertTrue(screen.contains("SceneFilterBackend"));
        assertTrue(screen.contains("MasaListBackend"));
        assertTrue(screen.contains("OreCompatibilityBackend"));
        for (String forbidden : List.of(
                "LocalFeatureConfig",
                "MasaIntegrationConfig",
                "BuiltInRegistries.BLOCK",
                "ResourceLocation.tryParse")) {
            assertFalse(screen.contains(forbidden), () -> "List editor regained domain responsibility: " + forbidden);
        }
    }

    private static String source(String path) throws IOException {
        return Files.readString(ROOT.resolve(path));
    }
}
