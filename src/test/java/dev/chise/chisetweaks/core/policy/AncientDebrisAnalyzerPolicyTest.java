package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AncientDebrisAnalyzerPolicyTest {
    @Test
    void rangeIsBoundedFromSixteenToTwoHundredFiftySixBlocks() {
        assertEquals(16, AncientDebrisAnalyzerPolicy.clampRangeBlocks(-1));
        assertEquals(16, AncientDebrisAnalyzerPolicy.clampRangeBlocks(16));
        assertEquals(64, AncientDebrisAnalyzerPolicy.clampRangeBlocks(64));
        assertEquals(256, AncientDebrisAnalyzerPolicy.clampRangeBlocks(9999));
    }

    @Test
    void markerCountIsBounded() {
        assertEquals(8, AncientDebrisAnalyzerPolicy.clampMaxMarkers(0));
        assertEquals(64, AncientDebrisAnalyzerPolicy.clampMaxMarkers(64));
        assertEquals(128, AncientDebrisAnalyzerPolicy.clampMaxMarkers(999));
    }

    @Test
    void rangeCheckUsesSquaredDistanceWithoutSquareRoot() {
        assertTrue(AncientDebrisAnalyzerPolicy.withinRangeSquared(64.0 * 64.0, 64));
        assertFalse(AncientDebrisAnalyzerPolicy.withinRangeSquared(64.0 * 64.0 + 0.01, 64));
    }

    @Test
    void lodGetsFainterAndThinnerAtDistance() {
        int nearAlpha = AncientDebrisAnalyzerPolicy.colorForDistance(8.0) >>> 24;
        int farAlpha = AncientDebrisAnalyzerPolicy.colorForDistance(200.0) >>> 24;
        assertTrue(nearAlpha > farAlpha);
        assertTrue(AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(8.0)
                > AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(200.0));
        assertTrue(AncientDebrisAnalyzerPolicy.boxInsetForDistance(8.0)
                < AncientDebrisAnalyzerPolicy.boxInsetForDistance(200.0));
    }
}
