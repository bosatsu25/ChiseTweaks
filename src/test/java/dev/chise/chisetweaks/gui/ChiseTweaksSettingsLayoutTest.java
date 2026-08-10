package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsLayoutTest {
    @Test
    void compactMinecraftViewportKeepsAllPrimaryRegionsInsideTheScreen() {
        var geometry = ChiseTweaksSettingsLayout.calculate(320, 240, true, 3);

        assertTrue(geometry.content().x() >= 0);
        assertTrue(geometry.content().right() <= 320);
        assertTrue(geometry.navigation().right() <= geometry.content().right());
        assertTrue(geometry.search().right() <= geometry.content().right());
        assertTrue(geometry.bulk().right() <= geometry.content().right());
        assertTrue(geometry.panel().right() <= geometry.content().right());
        assertTrue(geometry.footer().right() <= geometry.content().right());
        assertTrue(geometry.footer().bottom() <= 240);
        assertFalse(geometry.search().overlaps(geometry.bulk()));
        assertTrue(geometry.panel().bottom() <= geometry.footer().y());
        assertFooterIsNonOverlappingAndContained(geometry);
    }

    @Test
    void narrowCategoryToolbarStacksInsteadOfOverlapping() {
        var geometry = ChiseTweaksSettingsLayout.calculate(300, 240, true, 3);

        assertTrue(geometry.toolbarStacked());
        assertEquals(geometry.content().width(), geometry.search().width());
        assertEquals(geometry.content().width(), geometry.bulk().width());
        assertFalse(geometry.search().overlaps(geometry.bulk()));
        assertTrue(geometry.bulk().bottom() <= geometry.panel().y());
        assertFooterIsNonOverlappingAndContained(geometry);
    }

    @Test
    void categoryToolbarUsesInlineLayoutAtBoundaryWhenItFits() {
        // 316px screen - 24px margins = 292px content, exactly the inline toolbar requirement.
        var geometry = ChiseTweaksSettingsLayout.calculate(316, 240, true, 3);

        assertFalse(geometry.toolbarStacked());
        assertEquals(150, geometry.search().width());
        assertEquals(134, geometry.bulk().width());
        assertFalse(geometry.search().overlaps(geometry.bulk()));
    }

    @Test
    void navigationUsesCurrentSectionCountInsteadOfAStaleFixedCount() {
        var threeSections = ChiseTweaksSettingsLayout.calculate(854, 480, true, 3);
        var fiveSections = ChiseTweaksSettingsLayout.calculate(854, 480, true, 5);
        var defensiveFallback = ChiseTweaksSettingsLayout.calculate(854, 480, true, 0);

        assertEquals(3, threeSections.navigationCount());
        assertEquals(5, fiveSections.navigationCount());
        assertEquals(1, defensiveFallback.navigationCount());
        assertTrue(navigationRight(threeSections) <= threeSections.content().right());
        assertTrue(navigationRight(fiveSections) <= fiveSections.content().right());
        assertTrue(navigationRight(defensiveFallback) <= defensiveFallback.content().right());
    }

    @Test
    void textLayoutSwitchesExactlyAtTheResponsiveBreakpoint() {
        var below = ChiseTweaksSettingsLayout.calculate(743, 480, true, 3);
        var at = ChiseTweaksSettingsLayout.calculate(744, 480, true, 3);

        assertEquals(719, below.content().width());
        assertEquals(720, at.content().width());
        assertTrue(below.stackedText());
        assertFalse(at.stackedText());
        assertTrue(below.descriptionWidth() > 0);
        assertTrue(at.descriptionWidth() > 0);
    }

    @Test
    void wideViewportCentersAndCapsContentWithoutStretchingControlsIndefinitely() {
        var geometry = ChiseTweaksSettingsLayout.calculate(1920, 1080, true, 3);

        assertEquals(1180, geometry.content().width());
        assertEquals((1920 - 1180) / 2, geometry.content().x());
        assertTrue(geometry.controlWidth() <= 132);
        assertTrue(geometry.navButtonWidth() <= 112);
        assertFooterIsNonOverlappingAndContained(geometry);
    }

    @Test
    void nonCategoryPageHasNoBulkControlAndSearchUsesTheFullWidth() {
        var geometry = ChiseTweaksSettingsLayout.calculate(854, 480, false, 3);

        assertFalse(geometry.categoryPage());
        assertTrue(geometry.bulk().isEmpty());
        assertEquals(geometry.content().x(), geometry.search().x());
        assertEquals(geometry.content().width(), geometry.search().width());
    }

    @Test
    void footerCompressionPreventsTheFormerApplyDoneOverlapAtMinimumSupportedScale() {
        var geometry = ChiseTweaksSettingsLayout.calculate(320, 240, true, 3);

        assertFalse(geometry.resetButton().overlaps(geometry.applyButton()));
        assertFalse(geometry.applyButton().overlaps(geometry.doneButton()));
        assertFalse(geometry.resetButton().overlaps(geometry.doneButton()));
        assertTrue(geometry.footer().contains(geometry.resetButton()));
        assertTrue(geometry.footer().contains(geometry.applyButton()));
        assertTrue(geometry.footer().contains(geometry.doneButton()));
    }

    @Test
    void rectangleOverlapTreatsTouchingEdgesAsNonOverlapping() {
        var left = new ChiseTweaksSettingsLayout.Rect(0, 0, 10, 10);
        var touching = new ChiseTweaksSettingsLayout.Rect(10, 0, 10, 10);
        var overlapping = new ChiseTweaksSettingsLayout.Rect(9, 0, 10, 10);

        assertFalse(left.overlaps(touching));
        assertTrue(left.overlaps(overlapping));
        assertFalse(left.overlaps(null));
    }

    private static int navigationRight(ChiseTweaksSettingsLayout.Geometry geometry) {
        return geometry.navigation().x()
                + geometry.navButtonWidth() * geometry.navigationCount()
                + geometry.navGap() * (geometry.navigationCount() - 1);
    }

    private static void assertFooterIsNonOverlappingAndContained(
            ChiseTweaksSettingsLayout.Geometry geometry) {
        assertTrue(geometry.footer().contains(geometry.resetButton()));
        assertTrue(geometry.footer().contains(geometry.applyButton()));
        assertTrue(geometry.footer().contains(geometry.doneButton()));
        assertFalse(geometry.resetButton().overlaps(geometry.applyButton()));
        assertFalse(geometry.applyButton().overlaps(geometry.doneButton()));
        assertFalse(geometry.resetButton().overlaps(geometry.doneButton()));
    }
}
