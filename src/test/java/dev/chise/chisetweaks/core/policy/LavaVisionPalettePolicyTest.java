package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LavaVisionPalettePolicyTest {
    @Test
    void fillPalettePreservesDistanceRgbAndUsesTranslucentFaceAlpha() {
        assertEquals(0x4D075B32, LavaVisionPalettePolicy.fillColorForDistance(0.0));
        assertEquals(0x4D053B20, LavaVisionPalettePolicy.fillColorForDistance(5.0));
        assertEquals(0x4D021A0E, LavaVisionPalettePolicy.fillColorForDistance(8.0));
        assertEquals(0x4D021A0E, LavaVisionPalettePolicy.fillColorForDistance(Double.NaN));
    }
}
