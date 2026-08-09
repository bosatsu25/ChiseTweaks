package dev.chise.chisetweaks;

import dev.chise.chisetweaks.gui.ChiseTweaksRowTextLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksRowTextLayoutTest {
    @Test
    void shortDescriptionStaysOnOneCenteredLineCandidate() {
        var lines = ChiseTweaksRowTextLayout.wrap("短い説明", 20, String::length);

        assertEquals("短い説明", lines.first());
        assertEquals("", lines.second());
        assertEquals(1, lines.lineCount());
    }

    @Test
    void japaneseDescriptionWrapsWithoutDroppingCharacters() {
        var lines = ChiseTweaksRowTextLayout.wrap(
                "かぼちゃを使った設置作業を補助する",
                10,
                String::length);

        assertEquals("かぼちゃを使った設", lines.first());
        assertEquals("置作業を補助する", lines.second());
        assertEquals("かぼちゃを使った設置作業を補助する", lines.first() + lines.second());
        assertEquals(2, lines.lineCount());
    }

    @Test
    void englishDescriptionPrefersAWordBoundary() {
        var lines = ChiseTweaksRowTextLayout.wrap(
                "Highlight Nether Quartz Ore clearly",
                22,
                String::length);

        assertEquals("Highlight Nether", lines.first());
        assertEquals("Quartz Ore clearly", lines.second());
    }

    @Test
    void oversizedSecondLineUsesEllipsisInsideTheAvailableWidth() {
        var lines = ChiseTweaksRowTextLayout.wrap(
                "abcdefghijklmnopqrstuvwxyz",
                8,
                String::length);

        assertEquals("abcdefgh", lines.first());
        assertTrue(lines.second().endsWith("…"));
        assertTrue(lines.second().length() <= 8);
    }

    @Test
    void nullAndZeroWidthAreSafe() {
        assertEquals(0, ChiseTweaksRowTextLayout.wrap(null, 10, String::length).lineCount());
        assertEquals(0, ChiseTweaksRowTextLayout.wrap("text", 0, String::length).lineCount());
    }
}
