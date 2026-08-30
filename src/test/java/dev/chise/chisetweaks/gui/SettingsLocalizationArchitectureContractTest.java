package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsLocalizationArchitectureContractTest {
    private static final Path GUI = Path.of("src/main/java/dev/chise/chisetweaks/gui");
    private static final Pattern JAPANESE_STRING_LITERAL = Pattern.compile(
            "\\\"[^\\\"\\n]*[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}][^\\\"\\n]*\\\"");

    @Test
    void settingsBuilderAssistAndMasaOperationalCopyLivesInLanguageResources() throws Exception {
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));
        String rows = Files.readString(GUI.resolve("ChiseTweaksSettingsRows.java"));
        String assist = Files.readString(GUI.resolve("BuilderAssistRows.java"));
        String workflow = Files.readString(GUI.resolve("WorkflowRows.java"));
        String masa = Files.readString(GUI.resolve("MasaListBackend.java"));

        assertTrue(screen.contains("screen.chisetweaks.settings.apply_changes"));
        assertTrue(rows.contains("screen.chisetweaks.settings.copy.villager_links.description"));
        assertTrue(assist.contains("screen.chisetweaks.inspector.schematic.none.description"));
        assertTrue(masa.contains("screen.chisetweaks.masa_editor.feedback.save_failed"));

        assertFalse(JAPANESE_STRING_LITERAL.matcher(screen).find());
        assertFalse(JAPANESE_STRING_LITERAL.matcher(rows).find());
        assertFalse(JAPANESE_STRING_LITERAL.matcher(assist).find());
        assertFalse(JAPANESE_STRING_LITERAL.matcher(workflow).find());
        assertFalse(JAPANESE_STRING_LITERAL.matcher(masa).find());
    }
}
