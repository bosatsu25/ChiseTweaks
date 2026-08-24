package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsLayoutTest {
    @Test
    void compactMinecraftViewportKeepsTabsPanelAndFooterInsideTheScreen() {
        var geometry = ChiseTweaksSettingsLayout.calculate(320, 240);

        assertTrue(geometry.content().x() >= 0);
        assertTrue(geometry.content().right() <= 320);
        assertEquals(5, geometry.tabs().size());
        for (var tab : geometry.tabs()) {
            assertTrue(geometry.content().contains(tab));
        }
        assertTrue(geometry.panel().right() <= geometry.content().right());
        assertTrue(geometry.footer().right() <= geometry.content().right());
        assertTrue(geometry.footer().bottom() <= 240);
        assertTrue(geometry.panel().bottom() <= geometry.footer().y());
        assertFooterIsNonOverlappingAndContained(geometry);
    }

    @Test
    void fiveTabsStayOrderedAndNonOverlapping() {
        var geometry = ChiseTweaksSettingsLayout.calculate(854, 480);
        assertEquals(5, geometry.tabs().size());
        for (int index = 0; index < geometry.tabs().size() - 1; index++) {
            var current = geometry.tabs().get(index);
            var next = geometry.tabs().get(index + 1);
            assertFalse(current.overlaps(next));
            assertTrue(current.right() <= next.x());
        }
    }

    @Test
    void compactRowsReserveOneNameColumnAndStableRightAlignedControls() {
        var compact = ChiseTweaksSettingsLayout.calculate(320, 240);
        var desktop = ChiseTweaksSettingsLayout.calculate(854, 480);

        assertTrue(compact.nameWidth() > 0);
        assertTrue(desktop.nameWidth() > compact.nameWidth());
        assertTrue(compact.infoTextWidth() > compact.nameWidth());
        assertTrue(desktop.infoTextWidth() > desktop.nameWidth());
        assertTrue(compact.infoTextRight() < compact.panel().right());
        assertTrue(desktop.infoTextRight() < desktop.panel().right());
        assertTrue(compact.actionX() + compact.actionWidth() <= compact.toggleX());
        assertTrue(desktop.actionX() + desktop.actionWidth() <= desktop.toggleX());
        assertTrue(compact.toggleX() + compact.toggleWidth() <= compact.panel().right());
        assertTrue(desktop.toggleX() + desktop.toggleWidth() <= desktop.panel().right());
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
    void footerUsesOnlyContextActionAndDone() {
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
        assertTrue(geometry.footer().contains(geometry.contextButton()));
        assertTrue(geometry.footer().contains(geometry.doneButton()));
        assertFalse(geometry.contextButton().overlaps(geometry.doneButton()));
    }
}
