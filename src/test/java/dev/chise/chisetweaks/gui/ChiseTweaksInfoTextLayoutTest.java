package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksInfoTextLayoutTest {
    private static final String ENGLISH_DESCRIPTION =
            "Analyzes Lava Sources and Ancient Debris only in already-loaded chunks and never forces unloaded chunks to load.";
    private static final String JAPANESE_DESCRIPTION =
            "Lava SourceとAncient Debrisを読み込み済みチャンクだけから解析します。未ロードチャンクを強制ロードしません。";

    @Test
    void englishAndJapaneseHelpTextStayInsideSupportedPanelWidths() {
        for (String description : List.of(ENGLISH_DESCRIPTION, JAPANESE_DESCRIPTION)) {
            for (int screenWidth : List.of(320, 480, 960)) {
                var geometry = ChiseTweaksSettingsLayout.calculate(screenWidth, 480);
                var layout = create(geometry, description);

                assertTrue(geometry.infoTextRight() < geometry.panel().right());
                assertEquals(geometry.infoTextWidth(),
                        geometry.infoTextRight() - geometry.nameX());
                assertTrue(layout.rowHeight() >= geometry.infoRowHeight());
                assertTrue(layout.textBottom() <= layout.rowHeight());
                for (String line : layout.nameLines()) {
                    assertTrue(pixelWidth(line) <= geometry.infoTextWidth(), line);
                }
                for (String line : layout.descriptionLines()) {
                    assertTrue(pixelWidth(line) <= geometry.infoTextWidth(), line);
                }
            }
        }
    }

    @Test
    void narrowerPanelsProduceAtLeastAsManyLinesAndGrowTheInfoRow() {
        var narrow = create(ChiseTweaksSettingsLayout.calculate(320, 240), ENGLISH_DESCRIPTION);
        var medium = create(ChiseTweaksSettingsLayout.calculate(480, 320), ENGLISH_DESCRIPTION);
        var wide = create(ChiseTweaksSettingsLayout.calculate(960, 540), ENGLISH_DESCRIPTION);

        assertTrue(narrow.descriptionLines().size() >= medium.descriptionLines().size());
        assertTrue(medium.descriptionLines().size() >= wide.descriptionLines().size());
        assertTrue(narrow.rowHeight() >= medium.rowHeight());
        assertTrue(narrow.rowHeight() > ChiseTweaksSettingsLayout.calculate(320, 240).infoRowHeight());
    }

    @Test
    void emptyDescriptionKeepsTheMinimumHeightWithoutInventingASecondLine() {
        var geometry = ChiseTweaksSettingsLayout.calculate(854, 480);
        var layout = ChiseTweaksInfoTextLayout.create(
                "Help", "", geometry.infoTextWidth(), 9, geometry.infoRowHeight(),
                ChiseTweaksInfoTextLayoutTest::wrap);

        assertTrue(layout.descriptionLines().isEmpty());
        assertEquals(geometry.infoRowHeight(), layout.rowHeight());
        assertTrue(layout.textBottom() <= layout.rowHeight());
    }

    private static ChiseTweaksInfoTextLayout.Layout<String> create(
            ChiseTweaksSettingsLayout.Geometry geometry,
            String description) {
        return ChiseTweaksInfoTextLayout.create(
                "Analyzer", description, geometry.infoTextWidth(), 9, geometry.infoRowHeight(),
                ChiseTweaksInfoTextLayoutTest::wrap);
    }

    private static List<String> wrap(String text, int availableWidth) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int width = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            int characterWidth = pixelWidth(codePoint);
            if (!line.isEmpty() && width + characterWidth > availableWidth) {
                lines.add(line.toString());
                line.setLength(0);
                width = 0;
            }
            line.appendCodePoint(codePoint);
            width += characterWidth;
            offset += Character.charCount(codePoint);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return List.copyOf(lines);
    }

    private static int pixelWidth(String text) {
        return text.codePoints().map(ChiseTweaksInfoTextLayoutTest::pixelWidth).sum();
    }

    private static int pixelWidth(int codePoint) {
        return codePoint <= 0x7F ? 6 : 9;
    }
}
