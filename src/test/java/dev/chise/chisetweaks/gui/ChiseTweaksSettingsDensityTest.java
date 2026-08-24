package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsDensityTest {
    @Test
    void desktopRowsAndControlsStayCompact() {
        var geometry = ChiseTweaksSettingsLayout.calculate(854, 480);
        assertEquals(30, geometry.rowHeight());
        assertEquals(42, geometry.infoRowHeight());
        assertEquals(24, geometry.headerHeight());
        assertTrue(geometry.toggleWidth() <= 84);
        assertTrue(geometry.actionWidth() <= 96);
    }

    @Test
    void narrowLayoutAddsTouchHeightOnlyWhereHelpNeedsTwoLines() {
        var geometry = ChiseTweaksSettingsLayout.calculate(320, 240);
        assertEquals(34, geometry.rowHeight());
        assertEquals(48, geometry.infoRowHeight());
        assertEquals(24, geometry.headerHeight());
        assertTrue(geometry.nameWidth() > 0);
        assertTrue(geometry.toggleWidth() <= 84);
        assertTrue(geometry.actionWidth() <= 96);
    }
}
