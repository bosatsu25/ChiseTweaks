package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.OreHighlightLightingPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OreHighlightLightingPolicyTest {
    @Test
    void fullBrightLevelMatchesMinecraftMaximum() {
        assertEquals(15, OreHighlightLightingPolicy.FULL_BRIGHT_LIGHT_LEVEL);
    }

    @Test
    void normalBlockCubeAndBakeNoiseRemainNormallyLit() {
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(0.0f, 0.0f, 0.0f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(1.0f, 1.0f, 1.0f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(0.5f, 0.5f, 0.5f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(-0.001f, 0.5f, 0.5f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(1.001f, 0.5f, 0.5f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(0.5f, -0.001f, 0.5f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(0.5f, 1.001f, 0.5f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(0.5f, 0.5f, -0.001f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(0.5f, 0.5f, 1.001f));
    }

    @Test
    void expandedOverlayIsDetectedOnEverySide() {
        assertTrue(OreHighlightLightingPolicy.isOverlayVertex(-0.002f, 0.5f, 0.5f));
        assertTrue(OreHighlightLightingPolicy.isOverlayVertex(1.002f, 0.5f, 0.5f));
        assertTrue(OreHighlightLightingPolicy.isOverlayVertex(0.5f, -0.002f, 0.5f));
        assertTrue(OreHighlightLightingPolicy.isOverlayVertex(0.5f, 1.002f, 0.5f));
        assertTrue(OreHighlightLightingPolicy.isOverlayVertex(0.5f, 0.5f, -0.002f));
        assertTrue(OreHighlightLightingPolicy.isOverlayVertex(0.5f, 0.5f, 1.002f));
    }

    @Test
    void nonFiniteCoordinatesDoNotSilentlyClassifyAsNormalGeometry() {
        assertTrue(OreHighlightLightingPolicy.isOverlayVertex(Float.NEGATIVE_INFINITY, 0.5f, 0.5f));
        assertTrue(OreHighlightLightingPolicy.isOverlayVertex(Float.POSITIVE_INFINITY, 0.5f, 0.5f));
        assertFalse(OreHighlightLightingPolicy.isOverlayVertex(Float.NaN, Float.NaN, Float.NaN));
    }
}
