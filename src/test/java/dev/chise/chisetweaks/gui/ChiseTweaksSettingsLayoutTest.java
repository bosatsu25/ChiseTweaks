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
        assertTrue(geometry.panel().right() <= geometry.content().right());
        assertTrue(geometry.footer().right() <= geometry.content().right());
        assertTrue(geometry.footer().bottom() <= 240);
        assertTrue(geometry.panel().contains(geometry.bulk()));
        assertTrue(geometry.panel().contains(geometry.headerAction()));
        assertFalse(geometry.headerAction().overlaps(geometry.bulk()));
        assertTrue(geometry.panel().bottom() <= geometry.footer().y());
        assertFooterIsNonOverlappingAndContained(geometry);
    }

    @Test
    void compactRowsReserveOneNameColumnAndTwoNonOverlappingControls() {
        var compact = ChiseTweaksSettingsLayout.calculate(320, 240);
        var desktop = ChiseTweaksSettingsLayout.calculate(854, 480);

        assertTrue(compact.nameWidth() > 0);
        assertTrue(desktop.nameWidth() > compact.nameWidth());
        assertTrue(compact.actionX() + compact.actionWidth() <= compact.toggleX());
        assertTrue(desktop.actionX() + desktop.actionWidth() <= desktop.toggleX());
        assertTrue(compact.toggleX() + compact.toggleWidth() <= compact.panel().right());
        assertTrue(desktop.toggleX() + desktop.toggleWidth() <= desktop.panel().right());
    }

    @Test
    void highlightHeaderSettingsAndBulkButtonsShareOneHeaderWithoutOverlap() {
        var geometry = ChiseTweaksSettingsLayout.calculate(854, 480);

        assertEquals(18, geometry.bulk().height());
        assertEquals(18, geometry.headerAction().height());
        assertFalse(geometry.headerAction().overlaps(geometry.bulk()));
        assertTrue(geometry.bulk().bottom() <= geometry.panelContentTop() + geometry.headerHeight());
        assertTrue(geometry.headerAction().bottom() <= geometry.panelContentTop() + geometry.headerHeight());
    }

    @Test
    void wideViewportCentersAndCapsContentWithoutStretchingControlsIndefinitely() {
        var geometry = ChiseTweaksSettingsLayout.calculate(1920, 1080);

        assertEquals(960, geometry.content().width());
        assertEquals((1920 - 960) / 2, geometry.content().x());
        assertTrue(geometry.toggleWidth() <= 84);
        assertTrue(geometry.actionWidth() <= 96);
        assertFooterIsNonOverlappingAndContained(geometry);
    }

    @Test
    void footerCompressionKeepsGuideResetApplyAndDoneSeparateAtMinimumSupportedScale() {
        var geometry = ChiseTweaksSettingsLayout.calculate(320, 240);

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
