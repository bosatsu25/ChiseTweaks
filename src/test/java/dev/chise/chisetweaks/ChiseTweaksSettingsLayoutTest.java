package dev.chise.chisetweaks;

import dev.chise.chisetweaks.gui.ChiseTweaksSettingsLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsLayoutTest {
    private static final int[][] VIEWPORTS = {
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
    void wideScreensCapAndCenterTheContentInsteadOfStretchingControls() {
        var layout = ChiseTweaksSettingsLayout.calculate(2560, 1440, true);

        assertTrue(layout.content().width() <= 1180);
        assertTrue(layout.content().x() > 12);
        assertTrue(layout.controlWidth() <= 132);
        assertTrue(layout.search().width() < 1180);
    }

    @Test
    void compactControlsStayToTheRightOfDescriptions() {
        for (int[] viewport : VIEWPORTS) {
            var layout = ChiseTweaksSettingsLayout.calculate(viewport[0], viewport[1], true);
            assertTrue(layout.nameX() < layout.descriptionX(), viewportLabel(viewport));
            assertTrue(layout.descriptionX() < layout.controlX(), viewportLabel(viewport));
            assertTrue(layout.controlX() + layout.controlWidth() <= layout.panel().right(), viewportLabel(viewport));
        }
    }

    @Test
    void nonCategoryPagesGiveTheWholeSearchRowToSearch() {
        var layout = ChiseTweaksSettingsLayout.calculate(854, 480, false);

        assertTrue(layout.bulk().isEmpty());
        assertTrue(layout.search().width() == layout.content().width());
    }

    @Test
    void fiveNavigationButtonsFitInsideTheContentAtEverySupportedViewport() {
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
