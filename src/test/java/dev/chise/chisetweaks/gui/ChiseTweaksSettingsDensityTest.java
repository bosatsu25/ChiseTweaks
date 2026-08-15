package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsDensityTest {
    @Test
    void desktopRowsAndControlsStayCompact() {
        var geometry = ChiseTweaksSettingsLayout.calculate(854, 480, true, 3);
        assertEquals(34, geometry.rowHeight());
        assertEquals(22, geometry.headerHeight());
        assertTrue(geometry.controlWidth() <= 116);
    }

    @Test
    void narrowLayoutKeepsReadableButReducedVerticalDensity() {
        var geometry = ChiseTweaksSettingsLayout.calculate(320, 240, true, 3);
        assertTrue(geometry.stackedText());
        assertEquals(42, geometry.rowHeight());
        assertEquals(22, geometry.headerHeight());
        assertTrue(geometry.controlWidth() <= 116);
    }
}
