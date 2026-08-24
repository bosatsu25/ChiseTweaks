package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsInfoLayoutArchitectureContractTest {
    @Test
    void settingsScreenUsesWrappedVariableHeightInfoRowsInsideTheScrollViewport() throws IOException {
        String screen = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(screen.contains("ChiseTweaksInfoTextLayout.create("));
        assertTrue(screen.contains("font.split(Component.literal(text), availableWidth)"));
        assertTrue(screen.contains("row.infoTextLayout.rowHeight()"));
        assertTrue(screen.contains("extractor.enableScissor("));
        assertTrue(screen.contains("renderInfoText(extractor, row)"));
        assertFalse(screen.contains("extractor.text(font, row.definition.description()"),
                "INFO descriptions must not return to unbounded single-line rendering");
    }
}
