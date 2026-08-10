package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksRowTextLayoutTest {
    private static final ToIntFunction<String> MONOSPACE = String::length;

    @Test
    void nullBlankAndNonPositiveWidthsProduceNoRenderedText() {
        assertEquals(0, ChiseTweaksRowTextLayout.wrap(null, 10, MONOSPACE).lineCount());
        assertEquals(0, ChiseTweaksRowTextLayout.wrap("   ", 10, MONOSPACE).lineCount());
        assertEquals(0, ChiseTweaksRowTextLayout.wrap("text", 0, MONOSPACE).lineCount());
        assertEquals(0, ChiseTweaksRowTextLayout.wrap("text", -1, MONOSPACE).lineCount());
    }

    @Test
    void fittingDescriptionRemainsOnOneLine() {
        var lines = ChiseTweaksRowTextLayout.wrap("Nether Palette", 20, MONOSPACE);

        assertEquals("Nether Palette", lines.first());
        assertEquals("", lines.second());
        assertEquals(1, lines.lineCount());
    }

    @Test
    void englishDescriptionPrefersAWhitespaceBreak() {
        var lines = ChiseTweaksRowTextLayout.wrap("show nearby lava sources", 12, MONOSPACE);

        assertEquals("show nearby", lines.first());
        assertTrue(lines.second().startsWith("lava"));
        assertTrue(lines.first().length() <= 12);
        assertTrue(lines.second().length() <= 12);
    }

    @Test
    void japaneseDescriptionWrapsWithoutDependingOnSpaces() {
        var lines = ChiseTweaksRowTextLayout.wrap("近くの溶岩源を壁越しに表示する", 8, MONOSPACE);

        assertEquals(2, lines.lineCount());
        assertTrue(lines.first().length() <= 8);
        assertTrue(lines.second().length() <= 8);
    }

    @Test
    void surrogatePairsAreNeverSplitByWrappingOrEllipsis() {
        ToIntFunction<String> utf16Width = String::length;
        var lines = ChiseTweaksRowTextLayout.wrap("AB🚀CD🚀EF", 5, utf16Width);

        assertWellFormedUtf16(lines.first());
        assertWellFormedUtf16(lines.second());
        assertTrue(lines.first().length() <= 5);
        assertTrue(lines.second().length() <= 5);
    }

    @Test
    void overflowingSecondLineIsEllipsizedWithinTheWidthBudget() {
        var lines = ChiseTweaksRowTextLayout.wrap(
                "one two three four five six seven eight nine",
                10,
                MONOSPACE);

        assertEquals(2, lines.lineCount());
        assertTrue(lines.second().endsWith("…"));
        assertTrue(lines.first().length() <= 10);
        assertTrue(lines.second().length() <= 10);
    }

    @Test
    void widthSmallerThanEllipsisProducesAnEmptySafeResult() {
        ToIntFunction<String> doubleWidth = value -> value.codePointCount(0, value.length()) * 2;
        var lines = ChiseTweaksRowTextLayout.wrap("abc", 1, doubleWidth);

        assertEquals("", lines.first());
        assertEquals("", lines.second());
        assertEquals(0, lines.lineCount());
    }

    @Test
    void leadingAndTrailingWhitespaceIsRemovedBeforeLayout() {
        var lines = ChiseTweaksRowTextLayout.wrap("  block targets  ", 20, MONOSPACE);

        assertEquals("block targets", lines.first());
        assertEquals("", lines.second());
    }

    private static void assertWellFormedUtf16(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                assertTrue(index + 1 < value.length());
                assertTrue(Character.isLowSurrogate(value.charAt(index + 1)));
                index++;
            } else {
                assertTrue(!Character.isLowSurrogate(current));
            }
        }
    }
}
