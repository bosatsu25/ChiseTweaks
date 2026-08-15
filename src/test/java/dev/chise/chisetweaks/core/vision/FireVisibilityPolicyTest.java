package dev.chise.chisetweaks.core.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FireVisibilityPolicyTest {
    @Test
    void disabledModeIsExactlyVanillaNeutral() {
        assertFalse(FireVisibilityPolicy.shouldLower(false));
        assertEquals(0.0F, FireVisibilityPolicy.verticalOffset(false));
    }

    @Test
    void enabledModeUsesTheBoundedDownwardOffset() {
        assertTrue(FireVisibilityPolicy.shouldLower(true));
        assertEquals(FireVisibilityPolicy.LOWERED_OVERLAY_Y, FireVisibilityPolicy.verticalOffset(true));
        assertTrue(FireVisibilityPolicy.LOWERED_OVERLAY_Y < 0.0F);
        assertTrue(FireVisibilityPolicy.LOWERED_OVERLAY_Y > -0.5F);
    }
}
