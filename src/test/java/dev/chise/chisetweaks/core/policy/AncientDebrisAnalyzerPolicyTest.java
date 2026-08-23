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
    void chunkNeighborhoodIsBoundedAndCoversTheRangeEdge() {
        assertEquals(2, AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks(16));
        assertEquals(5, AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks(64));
        assertEquals(17, AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks(256));
        assertEquals(17, AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks(Integer.MAX_VALUE));
    }

    @Test
    void bootstrapNeighborhoodSizeMatchesTheBoundedChunkRadius() {
        assertEquals(25, AncientDebrisAnalyzerPolicy.bootstrapChunkCountForRangeBlocks(16));
        assertEquals(121, AncientDebrisAnalyzerPolicy.bootstrapChunkCountForRangeBlocks(64));
        assertEquals(1225, AncientDebrisAnalyzerPolicy.bootstrapChunkCountForRangeBlocks(256));
        assertEquals(
                AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNK_COUNT,
                AncientDebrisAnalyzerPolicy.bootstrapChunkCountForRangeBlocks(
                        AncientDebrisAnalyzerPolicy.MAX_RANGE_BLOCKS));
    }

    @Test
    void bootstrapPerTickBudgetIsPositiveAndSmallerThanTheMaximumNeighborhood() {
        assertTrue(AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK > 0);
        assertTrue(
                AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK
                        < AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNK_COUNT);
        int defaultChunkCount = AncientDebrisAnalyzerPolicy.bootstrapChunkCountForRangeBlocks(
                AncientDebrisAnalyzerPolicy.DEFAULT_RANGE_BLOCKS);
        int defaultTicks = (defaultChunkCount
                + AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK - 1)
                / AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK;
        assertEquals(2, defaultTicks);
    }

    @Test
    void chunkRelevanceUsesSymmetricBoundariesIncludingNegativeCoordinates() {
        assertTrue(AncientDebrisAnalyzerPolicy.isChunkRelevant(10, -10, 15, -15, 64));
        assertTrue(AncientDebrisAnalyzerPolicy.isChunkRelevant(-10, 10, -15, 15, 64));
        assertFalse(AncientDebrisAnalyzerPolicy.isChunkRelevant(10, -10, 16, -15, 64));
        assertFalse(AncientDebrisAnalyzerPolicy.isChunkRelevant(-10, 10, -15, 16, 64));
    }

    @Test
    void chunkRelevanceCannotOverflowAtExtremeCoordinates() {
        assertFalse(AncientDebrisAnalyzerPolicy.isChunkRelevant(
                Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0, 256));
        assertFalse(AncientDebrisAnalyzerPolicy.isChunkRelevant(
                0, Integer.MAX_VALUE, 0, Integer.MIN_VALUE, 256));
    }

    @Test
    void colorLodHasExactClosedBoundariesAndDistinctDistanceBands() {
        assertEquals(0xFFF8D56B, AncientDebrisAnalyzerPolicy.colorForDistance(0.0));
        assertEquals(0xFFF8D56B, AncientDebrisAnalyzerPolicy.colorForDistance(16.0));
        assertEquals(0xE6FFB347, AncientDebrisAnalyzerPolicy.colorForDistance(16.01));
        assertEquals(0xE6FFB347, AncientDebrisAnalyzerPolicy.colorForDistance(64.0));
        assertEquals(0xB3FF8C42, AncientDebrisAnalyzerPolicy.colorForDistance(64.01));
        assertEquals(0xB3FF8C42, AncientDebrisAnalyzerPolicy.colorForDistance(128.0));
        assertEquals(0x80FF6B35, AncientDebrisAnalyzerPolicy.colorForDistance(128.01));
        assertEquals(0x80FF6B35, AncientDebrisAnalyzerPolicy.colorForDistance(200.0));
    }

    @Test
    void edgeThicknessLodHasExactClosedBoundariesAndNonZeroReturns() {
        assertEquals(0.036f, AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(0.0), 0.000001f);
        assertEquals(0.036f, AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(16.0), 0.000001f);
        assertEquals(0.028f, AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(16.01), 0.000001f);
        assertEquals(0.028f, AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(64.0), 0.000001f);
        assertEquals(0.020f, AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(64.01), 0.000001f);
        assertEquals(0.020f, AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(128.0), 0.000001f);
        assertEquals(0.014f, AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(128.01), 0.000001f);
        assertEquals(0.014f, AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(200.0), 0.000001f);
    }

    @Test
    void boxInsetLodHasExactClosedBoundariesAndNonZeroReturns() {
        assertEquals(0.018f, AncientDebrisAnalyzerPolicy.boxInsetForDistance(0.0), 0.000001f);
        assertEquals(0.018f, AncientDebrisAnalyzerPolicy.boxInsetForDistance(64.0), 0.000001f);
        assertEquals(0.18f, AncientDebrisAnalyzerPolicy.boxInsetForDistance(64.01), 0.000001f);
        assertEquals(0.18f, AncientDebrisAnalyzerPolicy.boxInsetForDistance(128.0), 0.000001f);
        assertEquals(0.32f, AncientDebrisAnalyzerPolicy.boxInsetForDistance(128.01), 0.000001f);
        assertEquals(0.32f, AncientDebrisAnalyzerPolicy.boxInsetForDistance(200.0), 0.000001f);
    }
}
