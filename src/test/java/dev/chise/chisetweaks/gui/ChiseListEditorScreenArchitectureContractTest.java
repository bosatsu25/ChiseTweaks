package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseListEditorScreenArchitectureContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void sharedScreenOwnsWidgetsWhileDomainAdaptersOwnBusinessRules() throws IOException {
        Path gui = ROOT.resolve("src/main/java/dev/chise/chisetweaks/gui");
        String screen = Files.readString(gui.resolve("ChiseListEditorScreen.java"));
        String backend = Files.readString(gui.resolve("ChiseListEditorBackend.java"));
        String scene = Files.readString(gui.resolve("SceneFilterBackend.java"));
        String masa = Files.readString(gui.resolve("MasaListBackend.java"));
        String ore = Files.readString(gui.resolve("OreCompatibilityBackend.java"));
        String configScreen = Files.readString(gui.resolve("ChiseTweaksConfigScreen.java"));

        assertFalse(Files.exists(gui.resolve("ChiseMasaIntegrationEditorScreen.java")));
        assertFalse(Files.exists(gui.resolve("ChiseSceneFilterEditorScreen.java")));
        assertFalse(Files.exists(gui.resolve("ChiseOreCompatibilityScreen.java")));

        for (String target : new String[]{
                "BLOCK_FILTER",
                "ENTITY_FILTER",
                "ORE_COMPATIBILITY",
                "LITEMATICA_PICK_REDIRECT",
                "TWEAKERMORE_AUTO_PICK_GUARD",
                "TWEAKEROO_TOOL_SWITCH_GUARD"}) {
            assertTrue(screen.contains(target));
            assertTrue(backend.contains("case " + target));
            assertTrue(configScreen.contains("ChiseListEditorScreen.Target." + target));
        }

        assertTrue(screen.contains("private void movePage(int delta)"));
        assertTrue(screen.contains("private void clampPage()"));
        assertTrue(screen.contains("private void createRemoveButtons()"));
        assertTrue(screen.contains("private void createFooter()"));
        assertTrue(screen.contains("private void applyMutation("));
        assertTrue(screen.contains("ChiseOreCompatibilityLayout.calculate"));

        assertFalse(screen.contains("BuilderFocusConfig"));
        assertFalse(screen.contains("MasaIntegrationConfig"));
        assertFalse(screen.contains("OreHighlightCompatibilityConfig"));
        assertFalse(screen.contains("SettingPersistenceCoordinator"));
        assertFalse(screen.contains("BuiltInRegistries"));
        assertFalse(screen.contains("isSceneFilter()"));
        assertFalse(screen.contains("isMasaGuard()"));
        assertFalse(screen.contains("isPickRedirect()"));

        assertTrue(Files.exists(gui.resolve("SceneFilterBackend.java")));
        assertTrue(Files.exists(gui.resolve("MasaListBackend.java")));
        assertTrue(Files.exists(gui.resolve("OreCompatibilityBackend.java")));
        assertTrue(scene.contains("SettingPersistenceCoordinator.production()"));
        assertTrue(masa.contains("MasaIntegrationConfig.getInstance()"));
        assertTrue(ore.contains("OreHighlightCompatibilityConfig.put"));
        assertTrue(ore.contains("OreHighlightModelReload.request()"));
        assertTrue(backend.contains("BuiltInRegistries.BLOCK"));
    }
}
