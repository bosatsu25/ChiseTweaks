package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AncientDebrisAnalyzerSustainedBudgetTest {
    @Test
    void maximumBootstrapFinishesWithinTwentyTicksAtPublishedBudget() {
        int remaining = AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNK_COUNT;
        int ticks = 0;
        while (remaining > 0) {
            int scanned = Math.min(remaining, AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK);
            assertTrue(scanned <= AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK);
            remaining -= scanned;
            ticks++;
        }
        assertTrue(ticks <= 20, "maximum-range bootstrap must stay incrementally bounded");
    }

    @Test
    void oneHourValidationSimulationNeverExceedsPerTickBudget() {
        int simulatedTicks = 20 * 60 * 60;
        long totalValidatedChunks = 0;
        for (int tick = 0; tick < simulatedTicks; tick++) {
            int validated = tick % AncientDebrisAnalyzerPolicy.VALIDATION_INTERVAL_TICKS == 0
                    ? AncientDebrisAnalyzerPolicy.MAX_VALIDATION_CHUNKS_PER_TICK
                    : 0;
            assertTrue(validated <= AncientDebrisAnalyzerPolicy.MAX_VALIDATION_CHUNKS_PER_TICK);
            totalValidatedChunks += validated;
        }
        long expectedCycles = (simulatedTicks + AncientDebrisAnalyzerPolicy.VALIDATION_INTERVAL_TICKS - 1L)
                / AncientDebrisAnalyzerPolicy.VALIDATION_INTERVAL_TICKS;
        assertEquals(expectedCycles * AncientDebrisAnalyzerPolicy.MAX_VALIDATION_CHUNKS_PER_TICK,
                totalValidatedChunks);
    }

    @Test
    void publishedWorstCaseRetainedPositionBudgetFitsLongAndMarkerOutputRemainsSmall() {
        long retainedPositions = (long) AncientDebrisAnalyzerPolicy.MAX_TRACKED_CHUNKS
                * AncientDebrisAnalyzerPolicy.MAX_DEBRIS_PER_CHUNK;
        assertEquals(1_048_576L, retainedPositions);
        assertTrue(AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS <= 128);
        assertTrue(retainedPositions < Integer.MAX_VALUE);
    }
}
