package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsLayoutTest {
    @Test
    void compactMinecraftViewportKeepsAllPrimaryRegionsInsideTheScreen() {
        var geometry = ChiseTweaksSettingsLayout.calculate(320, 240);

        assertTrue(geometry.content().x() >= 0);
        assertTrue(geometry.content().right() <= 320);
        assertTrue(geometry.search().right() <= geometry.content().right());
        assertTrue(geometry.panel().right() <= geometry.content().right());
        assertTrue(geometry.footer().right() <= geometry.content().right());
        assertTrue(geometry.footer().bottom() <= 240);
        assertTrue(geometry.panel().contains(geometry.bulk()));
        assertTrue(geometry.panel().bottom() <= geometry.footer().y());
        assertFooterIsNonOverlappingAndContained(geometry);
    }

    @Test
    void searchAlwaysUsesFullContentWidthWithoutAFormerTopBulkSlot() {
        var compact = ChiseTweaksSettingsLayout.calculate(320, 240);
        var desktop = ChiseTweaksSettingsLayout.calculate(854, 480);

        assertEquals(compact.content().x(), compact.search().x());
        assertEquals(compact.content().width(), compact.search().width());
        assertEquals(desktop.content().x(), desktop.search().x());
        assertEquals(desktop.content().width(), desktop.search().width());
        assertTrue(compact.search().bottom() <= compact.panel().y());
        assertTrue(desktop.search().bottom() <= desktop.panel().y());
    }

    @Test
    void highlightBulkButtonLivesInsideThePanelHeaderArea() {
        var geometry = ChiseTweaksSettingsLayout.calculate(854, 480);

        assertTrue(geometry.panel().contains(geometry.bulk()));
        assertEquals(18, geometry.bulk().height());
        assertTrue(geometry.bulk().y() >= geometry.panelContentTop());
        assertTrue(geometry.bulk().bottom() <= geometry.panelContentTop() + geometry.headerHeight());
    }

    @Test
    void textLayoutSwitchesExactlyAtTheResponsiveBreakpoint() {
        var below = ChiseTweaksSettingsLayout.calculate(743, 480);
        var at = ChiseTweaksSettingsLayout.calculate(744, 480);

        assertEquals(719, below.content().width());
        assertEquals(720, at.content().width());
        assertTrue(below.stackedText());
        assertFalse(at.stackedText());
        assertTrue(below.descriptionWidth() > 0);
        assertTrue(at.descriptionWidth() > 0);
    }

    @Test
    void wideViewportCentersAndCapsContentWithoutStretchingControlsIndefinitely() {
        var geometry = ChiseTweaksSettingsLayout.calculate(1920, 1080);

        assertEquals(1180, geometry.content().width());
        assertEquals((1920 - 1180) / 2, geometry.content().x());
        assertTrue(geometry.controlWidth() <= 116);
        assertEquals(geometry.content().width(), geometry.search().width());
        assertFooterIsNonOverlappingAndContained(geometry);
    }

    @Test
    void footerCompressionKeepsGuideResetApplyAndDoneSeparateAtMinimumSupportedScale() {
        var geometry = ChiseTweaksSettingsLayout.calculate(320, 240);

        assertFalse(geometry.helpButton().overlaps(geometry.resetButton()));
        assertFalse(geometry.resetButton().overlaps(geometry.applyButton()));
        assertFalse(geometry.applyButton().overlaps(geometry.doneButton()));
        assertFalse(geometry.helpButton().overlaps(geometry.doneButton()));
        assertFooterIsNonOverlappingAndContained(geometry);
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

    private static void assertFooterIsNonOverlappingAndContained(
            ChiseTweaksSettingsLayout.Geometry geometry) {
        assertTrue(geometry.footer().contains(geometry.helpButton()));
        assertTrue(geometry.footer().contains(geometry.resetButton()));
        assertTrue(geometry.footer().contains(geometry.applyButton()));
        assertTrue(geometry.footer().contains(geometry.doneButton()));
        assertFalse(geometry.helpButton().overlaps(geometry.resetButton()));
        assertFalse(geometry.helpButton().overlaps(geometry.applyButton()));
        assertFalse(geometry.helpButton().overlaps(geometry.doneButton()));
        assertFalse(geometry.resetButton().overlaps(geometry.applyButton()));
        assertFalse(geometry.resetButton().overlaps(geometry.doneButton()));
        assertFalse(geometry.applyButton().overlaps(geometry.doneButton()));
    }
}
