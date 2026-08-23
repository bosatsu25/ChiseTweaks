package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LavaVisionPalettePolicyTest {
    @Test
    void fillPalettePreservesDistanceRgbAndUsesTranslucentFaceAlpha() {
        assertEquals(0x4D075B32, LavaVisionPalettePolicy.fillColorForDistance(0.0));
        assertEquals(0x4D053B20, LavaVisionPalettePolicy.fillColorForDistance(5.0));
        assertEquals(0x4D021A0E, LavaVisionPalettePolicy.fillColorForDistance(8.0));
        assertEquals(0x4D021A0E, LavaVisionPalettePolicy.fillColorForDistance(Double.NaN));
    }

    @Test
    void filledMarkerInsetCannotInvertTheCube() {
        assertEquals(0.018f, LavaVisionPalettePolicy.ANALYZER_BOX_INSET, 0.000001f);
        float filledInset = LavaVisionPalettePolicy.ANALYZER_BOX_INSET
                + LavaVisionPalettePolicy.ANALYZER_EDGE_THICKNESS;
        assertTrue(filledInset > 0.0f && filledInset < 0.5f);
    }
}
