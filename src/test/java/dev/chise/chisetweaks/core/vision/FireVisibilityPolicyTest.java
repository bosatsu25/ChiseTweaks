package dev.chise.chisetweaks.core.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FireVisibilityPolicyTest {
    @Test
    void disabledModeIsExactlyVanillaNeutral() {
        assertFalse(FireVisibilityPolicy.shouldLower(false));
        assertEquals(0.0F, FireVisibilityPolicy.verticalOffset(false, FireVisibilityPolicy.SIZE_MEDIUM));
        assertEquals(1.0F, FireVisibilityPolicy.heightScale(false, FireVisibilityPolicy.SIZE_MEDIUM));
    }

    @Test
    void threePresetsStayBoundedAndMonotonic() {
        float large = FireVisibilityPolicy.heightScale(true, FireVisibilityPolicy.SIZE_LARGE);
        float medium = FireVisibilityPolicy.heightScale(true, FireVisibilityPolicy.SIZE_MEDIUM);
        float small = FireVisibilityPolicy.heightScale(true, FireVisibilityPolicy.SIZE_SMALL);

        assertTrue(large > medium);
        assertTrue(medium > small);
        assertTrue(small > 0.0F);
        assertTrue(large < 1.0F);

        assertTrue(FireVisibilityPolicy.verticalOffset(true, FireVisibilityPolicy.SIZE_LARGE) < 0.0F);
        assertTrue(FireVisibilityPolicy.verticalOffset(true, FireVisibilityPolicy.SIZE_MEDIUM)
                < FireVisibilityPolicy.verticalOffset(true, FireVisibilityPolicy.SIZE_LARGE));
        assertTrue(FireVisibilityPolicy.verticalOffset(true, FireVisibilityPolicy.SIZE_SMALL)
                < FireVisibilityPolicy.verticalOffset(true, FireVisibilityPolicy.SIZE_MEDIUM));
    }

    @Test
    void invalidPresetIsClampedAndLabelsRemainStable() {
        assertEquals(FireVisibilityPolicy.SIZE_LARGE, FireVisibilityPolicy.clampSizePreset(-99));
        assertEquals(FireVisibilityPolicy.SIZE_SMALL, FireVisibilityPolicy.clampSizePreset(99));
        assertEquals("Large", FireVisibilityPolicy.sizeLabel(FireVisibilityPolicy.SIZE_LARGE));
        assertEquals("Medium", FireVisibilityPolicy.sizeLabel(FireVisibilityPolicy.SIZE_MEDIUM));
        assertEquals("Small", FireVisibilityPolicy.sizeLabel(FireVisibilityPolicy.SIZE_SMALL));
    }
}
