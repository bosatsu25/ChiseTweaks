package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WardenRiskAnalyzerPolicyTest {
    @Test
    void rangeAndMarkerSettingsStayWithinReviewedBounds() {
        assertEquals(16, WardenRiskAnalyzerPolicy.clampRangeBlocks(-1));
        assertEquals(48, WardenRiskAnalyzerPolicy.clampRangeBlocks(48));
        assertEquals(64, WardenRiskAnalyzerPolicy.clampRangeBlocks(999));
        assertEquals(8, WardenRiskAnalyzerPolicy.clampMaxMarkers(0));
        assertEquals(64, WardenRiskAnalyzerPolicy.clampMaxMarkers(999));
    }

    @Test
    void chunkNeighborhoodIsBoundedForHardMaximumRange() {
        assertEquals(2, WardenRiskAnalyzerPolicy.chunkRadiusForRangeBlocks(16));
        assertEquals(4, WardenRiskAnalyzerPolicy.chunkRadiusForRangeBlocks(48));
        assertEquals(5, WardenRiskAnalyzerPolicy.chunkRadiusForRangeBlocks(64));
        assertTrue(WardenRiskAnalyzerPolicy.isChunkRelevant(0, 0, 5, -5, 64));
        assertFalse(WardenRiskAnalyzerPolicy.isChunkRelevant(0, 0, 6, 0, 64));
    }

    @Test
    void distanceCheckUsesConfiguredRadius() {
        assertTrue(WardenRiskAnalyzerPolicy.withinRangeSquared(48.0 * 48.0, 48));
        assertFalse(WardenRiskAnalyzerPolicy.withinRangeSquared(48.0 * 48.0 + 0.01, 48));
    }

    @Test
    void fillAlphaAlwaysStaysBelowOutlineAlpha() {
        int outline = WardenRiskAnalyzerPolicy.colorForDistance(8.0);
        int fill = WardenRiskAnalyzerPolicy.fillColorForDistance(8.0);
        assertTrue(((fill >>> 24) & 0xFF) < ((outline >>> 24) & 0xFF));
        assertEquals(outline & 0x00FFFFFF, fill & 0x00FFFFFF);
    }
}
