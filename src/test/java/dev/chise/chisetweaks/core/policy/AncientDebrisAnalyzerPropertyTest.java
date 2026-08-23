package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AncientDebrisAnalyzerPropertyTest {
    @Test
    void clampingAlwaysReturnsValuesInsidePublishedBounds() {
        for (int value : new int[] {
                Integer.MIN_VALUE, -1000, -1, 0, 15, 16, 17, 63, 64, 255, 256, 257, 1000, Integer.MAX_VALUE}) {
            int range = AncientDebrisAnalyzerPolicy.clampRangeBlocks(value);
            assertTrue(range >= AncientDebrisAnalyzerPolicy.MIN_RANGE_BLOCKS);
            assertTrue(range <= AncientDebrisAnalyzerPolicy.MAX_RANGE_BLOCKS);

            int markers = AncientDebrisAnalyzerPolicy.clampMaxMarkers(value);
            assertTrue(markers >= AncientDebrisAnalyzerPolicy.MIN_MAX_MARKERS);
            assertTrue(markers <= AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS);
        }
    }

    @Test
    void bootstrapNeighborhoodNeverExceedsDeclaredCapacity() {
        for (int range = -512; range <= 512; range += 7) {
            int radius = AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks(range);
            int count = AncientDebrisAnalyzerPolicy.bootstrapChunkCountForRangeBlocks(range);
            assertTrue(radius >= 0 && radius <= AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNK_RADIUS);
            assertEquals((radius * 2 + 1) * (radius * 2 + 1), count);
            assertTrue(count <= AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNK_COUNT);
        }
    }

    @Test
    void chunkRelevanceIsSymmetricAroundTheCenterForRepresentativeRanges() {
        for (int range : new int[] {16, 31, 32, 63, 64, 127, 128, 255, 256}) {
            int radius = AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks(range);
            for (int delta = 0; delta <= radius; delta++) {
                assertTrue(AncientDebrisAnalyzerPolicy.isChunkRelevant(40, -70, 40 + delta, -70 - delta, range));
                assertTrue(AncientDebrisAnalyzerPolicy.isChunkRelevant(40, -70, 40 - delta, -70 + delta, range));
            }
            assertFalse(AncientDebrisAnalyzerPolicy.isChunkRelevant(40, -70, 40 + radius + 1, -70, range));
            assertFalse(AncientDebrisAnalyzerPolicy.isChunkRelevant(40, -70, 40, -70 - radius - 1, range));
        }
    }

    @Test
    void squaredRangeBoundaryIsClosedForEverySupportedRangeBand() {
        for (int range = AncientDebrisAnalyzerPolicy.MIN_RANGE_BLOCKS;
                range <= AncientDebrisAnalyzerPolicy.MAX_RANGE_BLOCKS;
                range += 13) {
            double boundary = (double) range * range;
            assertTrue(AncientDebrisAnalyzerPolicy.withinRangeSquared(boundary, range));
            assertFalse(AncientDebrisAnalyzerPolicy.withinRangeSquared(Math.nextUp(boundary), range));
        }
    }

    @Test
    void fillAlphaNeverExceedsOutlineAndGeometryNeverInverts() {
        for (double distance = 0.0; distance <= 512.0; distance += 0.5) {
            int outline = AncientDebrisAnalyzerPolicy.colorForDistance(distance);
            int fill = AncientDebrisAnalyzerPolicy.fillColorForDistance(distance);
            int outlineAlpha = (outline >>> 24) & 0xFF;
            int fillAlpha = (fill >>> 24) & 0xFF;
            assertTrue(fillAlpha > 0 && fillAlpha <= outlineAlpha);
            assertEquals(outline & 0x00FFFFFF, fill & 0x00FFFFFF);

            float occupiedInset = AncientDebrisAnalyzerPolicy.boxInsetForDistance(distance)
                    + AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(distance);
            assertTrue(occupiedInset > 0.0f && occupiedInset < 0.5f, "distance=" + distance);
        }
    }
}
