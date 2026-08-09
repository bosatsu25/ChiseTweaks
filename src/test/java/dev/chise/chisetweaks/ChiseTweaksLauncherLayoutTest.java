package dev.chise.chisetweaks;

import dev.chise.chisetweaks.gui.ChiseTweaksLauncherLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksLauncherLayoutTest {
    @Test
    void emptyOptionsScreenUsesCompactTopRightLauncher() {
        var placement = ChiseTweaksLauncherLayout.place(854, 480, List.of());

        assertEquals(736, placement.x());
        assertEquals(6, placement.y());
        assertEquals(112, placement.width());
        assertEquals(20, placement.height());
    }

    @Test
    void launcherMovesBelowExistingTopRightControls() {
        var occupied = List.of(new ChiseTweaksLauncherLayout.Bounds(730, 4, 120, 22));
        var placement = ChiseTweaksLauncherLayout.place(854, 480, occupied);
        var launcherBounds = new ChiseTweaksLauncherLayout.Bounds(
                placement.x(), placement.y(), placement.width(), placement.height());

        assertTrue(placement.y() >= 30);
        assertFalse(launcherBounds.overlaps(occupied.getFirst()));
    }

    @Test
    void launcherFallsBackToLeftWhenRightColumnIsExhausted() {
        var occupied = List.of(new ChiseTweaksLauncherLayout.Bounds(736, 0, 112, 480));
        var placement = ChiseTweaksLauncherLayout.place(854, 480, occupied);

        assertEquals(6, placement.x());
        assertEquals(6, placement.y());
    }

    @Test
    void launcherRemainsInsideSupportedViewportMatrix() {
        for (int[] viewport : List.of(
                new int[] {320, 240},
                new int[] {640, 360},
                new int[] {854, 480},
                new int[] {1280, 720},
                new int[] {1920, 1080},
                new int[] {2560, 1440})) {
            var placement = ChiseTweaksLauncherLayout.place(viewport[0], viewport[1], List.of());
            assertTrue(placement.x() >= 0);
            assertTrue(placement.y() >= 0);
            assertTrue(placement.x() + placement.width() <= viewport[0]);
            assertTrue(placement.y() + placement.height() <= viewport[1]);
        }
    }
}
