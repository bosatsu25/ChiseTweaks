package dev.chise.chisetweaks.feature.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class OccludedNearestBufferTest {
    @Test
    void retainsOnlyTheNearestRequestedPositionsAndSortsForStableSnapshots() {
        OccludedHighlightsFeature.NearestBuffer buffer = new OccludedHighlightsFeature.NearestBuffer(4);

        buffer.offer(40L, 40.0, 3);
        buffer.offer(10L, 10.0, 3);
        buffer.offer(30L, 30.0, 3);
        buffer.offer(20L, 20.0, 3);
        buffer.offer(50L, 50.0, 3);

        assertEquals(3, buffer.count());
        buffer.sortPositions();
        assertArrayEquals(new long[] {10L, 20L, 30L},
                java.util.Arrays.copyOf(buffer.positions(), buffer.count()));
    }

    @Test
    void clearAndZeroLimitDoNotLeakPreviousCandidates() {
        OccludedHighlightsFeature.NearestBuffer buffer = new OccludedHighlightsFeature.NearestBuffer(2);
        buffer.offer(1L, 1.0, 2);
        buffer.offer(2L, 2.0, 2);
        assertEquals(2, buffer.count());

        buffer.clear();
        buffer.offer(3L, 0.5, 0);
        assertEquals(0, buffer.count());

        buffer.offer(4L, 0.25, 99);
        assertEquals(1, buffer.count());
    }

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new OccludedHighlightsFeature.NearestBuffer(0));
    }
}
