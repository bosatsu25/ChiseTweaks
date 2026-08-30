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
    void listEditorsShareOneRuntimeScreenWithoutCollapsingPersistenceDomains() throws IOException {
        Path gui = ROOT.resolve("src/main/java/dev/chise/chisetweaks/gui");
        String editor = Files.readString(gui.resolve("ChiseListEditorScreen.java"));
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
            assertTrue(editor.contains(target));
            assertTrue(configScreen.contains("ChiseListEditorScreen.Target." + target));
        }

        assertTrue(editor.contains("SettingPersistenceCoordinator.production()"));
        assertTrue(editor.contains("MasaIntegrationConfig.getInstance()"));
        assertTrue(editor.contains("OreHighlightCompatibilityConfig.put"));
        assertTrue(editor.contains("OreHighlightModelReload.request()"));
        assertTrue(editor.contains("ChiseOreCompatibilityLayout.calculate"));
        assertTrue(editor.contains("private void movePage(int delta)"));
        assertTrue(editor.contains("private void clampPage()"));
        assertTrue(editor.contains("private void createRemoveButtons()"));
        assertTrue(editor.contains("private void createFooter()"));
    }
}
