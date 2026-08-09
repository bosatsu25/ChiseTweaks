package dev.chise.chisetweaks;

import dev.chise.chisetweaks.gui.ChiseTweaksSettingsLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsLayoutTest {
    private static final int[][] VIEWPORTS = {
            {360, 260},
            {480, 320},
            {640, 360},
            {854, 480},
            {1280, 720},
            {1920, 1080},
            {2560, 1440}
    };

    @Test
    void categoryLayoutNeverOverlapsSearchBulkPanelOrFooter() {
        for (int[] viewport : VIEWPORTS) {
            var layout = ChiseTweaksSettingsLayout.calculate(viewport[0], viewport[1], true);
            assertFalse(layout.search().overlaps(layout.bulk()), viewportLabel(viewport));
            assertFalse(layout.navigation().overlaps(layout.search()), viewportLabel(viewport));
            assertFalse(layout.search().overlaps(layout.panel()), viewportLabel(viewport));
            assertFalse(layout.bulk().overlaps(layout.panel()), viewportLabel(viewport));
            assertFalse(layout.panel().overlaps(layout.footer()), viewportLabel(viewport));
            assertTrue(layout.content().contains(layout.search()), viewportLabel(viewport));
            assertTrue(layout.content().contains(layout.bulk()), viewportLabel(viewport));
            assertTrue(layout.content().contains(layout.panel()), viewportLabel(viewport));
            assertTrue(layout.content().contains(layout.footer()), viewportLabel(viewport));
        }
    }

    @Test
    void approved854x480StandaloneGeometryIsPinnedAgainstUiRegression() {
        var layout = ChiseTweaksSettingsLayout.calculate(854, 480, true);
        assertEquals(new ChiseTweaksSettingsLayout.Rect(12, 0, 830, 480), layout.content());
        assertTrue(layout.selector().isEmpty());
        assertEquals(new ChiseTweaksSettingsLayout.Rect(12, 40, 463, 20), layout.navigation());
        assertEquals(new ChiseTweaksSettingsLayout.Rect(12, 68, 688, 20), layout.search());
        assertEquals(new ChiseTweaksSettingsLayout.Rect(708, 68, 134, 20), layout.bulk());
        assertEquals(new ChiseTweaksSettingsLayout.Rect(12, 98, 830, 342), layout.panel());
        assertEquals(new ChiseTweaksSettingsLayout.Rect(12, 450, 830, 20), layout.footer());
        assertEquals(112, layout.navButtonWidth());
        assertEquals(118, layout.controlWidth());
        assertEquals(118, layout.controlSlotWidth());
        assertEquals(28, layout.nameX());
        assertEquals(227, layout.nameWidth());
        assertEquals(269, layout.descriptionX());
        assertEquals(427, layout.descriptionWidth());
        assertEquals(708, layout.controlX());
        assertEquals(38, layout.rowHeight());
        assertEquals(24, layout.headerHeight());
        assertFalse(layout.stackedText());
    }

    @Test
    void compactScreensStackTextAndReserveFullIntegerControlSlot() {
        var layout = ChiseTweaksSettingsLayout.calculate(640, 360, true);

        assertTrue(layout.stackedText());
        assertEquals(46, layout.rowHeight());
        assertEquals(layout.nameX(), layout.descriptionX());
        assertEquals(layout.nameWidth(), layout.descriptionWidth());
        assertEquals(110, layout.controlSlotWidth());
        assertTrue(layout.descriptionX() + layout.descriptionWidth() <= layout.controlX());
        assertTrue(layout.booleanControlX() >= layout.controlX());
        assertTrue(layout.booleanControlX() + layout.controlWidth()
                <= layout.controlX() + layout.controlSlotWidth());
    }

    @Test
    void wideScreensCapAndCenterTheContentInsteadOfStretchingControls() {
        var layout = ChiseTweaksSettingsLayout.calculate(2560, 1440, true);
        assertTrue(layout.content().width() <= 1180);
        assertTrue(layout.content().x() > 12);
        assertTrue(layout.controlWidth() <= 132);
        assertTrue(layout.search().width() < 1180);
        assertFalse(layout.stackedText());
    }

    @Test
    void textRegionsStayBeforeTheControlSlotAtEverySupportedViewport() {
        for (int[] viewport : VIEWPORTS) {
            var layout = ChiseTweaksSettingsLayout.calculate(viewport[0], viewport[1], true);
            assertTrue(layout.nameX() < layout.controlX(), viewportLabel(viewport));
            assertTrue(layout.descriptionX() < layout.controlX(), viewportLabel(viewport));
            assertTrue(layout.descriptionX() + layout.descriptionWidth() <= layout.controlX(), viewportLabel(viewport));
            assertTrue(layout.controlX() + layout.controlSlotWidth() <= layout.panel().right(), viewportLabel(viewport));
        }
    }

    @Test
    void nonCategoryPagesGiveTheWholeSearchRowToSearch() {
        var layout = ChiseTweaksSettingsLayout.calculate(854, 480, false);
        assertTrue(layout.bulk().isEmpty());
        assertTrue(layout.search().width() == layout.content().width());
    }

    @Test
    void fourNavigationButtonsFitInsideTheContentAtEverySupportedViewport() {
        for (int[] viewport : VIEWPORTS) {
            var layout = ChiseTweaksSettingsLayout.calculate(viewport[0], viewport[1], true);
            assertTrue(layout.content().contains(layout.navigation()), viewportLabel(viewport));
            assertTrue(layout.navButtonWidth() >= 64, viewportLabel(viewport));
        }
    }

    private static String viewportLabel(int[] viewport) {
        return viewport[0] + "x" + viewport[1];
    }
}
