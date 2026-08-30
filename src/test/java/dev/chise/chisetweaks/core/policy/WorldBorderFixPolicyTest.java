package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldBorderFixPolicyTest {
    @Test
    void borderGuardTriggersInsideConfiguredDistance() {
        assertTrue(WorldBorderFixPolicy.shouldSuppress(
                127.0, 0.0, false, true, 128, false, 100000));
        assertFalse(WorldBorderFixPolicy.shouldSuppress(
                128.0, 0.0, false, true, 128, false, 100000));
    }

    @Test
    void hysteresisKeepsSuppressionUntilOutsideExitMargin() {
        assertTrue(WorldBorderFixPolicy.shouldSuppress(
                150.0, 0.0, true, true, 128, false, 100000));
        assertFalse(WorldBorderFixPolicy.shouldSuppress(
                160.0, 0.0, true, true, 128, false, 100000));
    }

    @Test
    void farCoordinateGuardUsesExitMarginWhenSuppressed() {
        assertTrue(WorldBorderFixPolicy.shouldSuppress(
                Double.POSITIVE_INFINITY, 100000.0, false, false, 128, true, 100000));
        assertTrue(WorldBorderFixPolicy.shouldSuppress(
                Double.POSITIVE_INFINITY, 99800.0, true, false, 128, true, 100000));
        assertFalse(WorldBorderFixPolicy.shouldSuppress(
                Double.POSITIVE_INFINITY, 99743.0, true, false, 128, true, 100000));
    }

    @Test
    void squareBorderDistanceIsNegativeOutsideBorder() {
        assertEquals(50.0, WorldBorderFixPolicy.distanceInsideSquareBorder(
                0.0, 0.0, 0.0, 0.0, 100.0));
        assertEquals(0.0, WorldBorderFixPolicy.distanceInsideSquareBorder(
                50.0, 0.0, 0.0, 0.0, 100.0));
        assertEquals(-10.0, WorldBorderFixPolicy.distanceInsideSquareBorder(
                60.0, 0.0, 0.0, 0.0, 100.0));
    }
}
