package dev.chise.chisetweaks.performance;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformanceAcceptancePolicyTest {
    @Test
    void representativeStableCandidatePassesWithinSGradeBudgets() {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results = completeResults(0.0);
        results.put(PerformanceMetric.P50_FRAMETIME_MS, result(4.9));
        results.put(PerformanceMetric.P95_FRAMETIME_MS, result(4.9));
        results.put(PerformanceMetric.P99_FRAMETIME_MS, result(9.9));
        results.put(PerformanceMetric.ALLOCATION_MIB_S, result(9.9));
        results.put(PerformanceMetric.AVERAGE_FPS, result(-4.9));
        assertTrue(PerformanceAcceptancePolicy.evaluate(Map.copyOf(results)).accepted());
    }

    @Test
    void everyMetricHasAnExplicitSGradeRegressionBudget() {
        assertEquals(10.0, PerformanceAcceptancePolicy.maxRegressionPercent(PerformanceMetric.STARTUP_MS));
        assertEquals(5.0, PerformanceAcceptancePolicy.maxRegressionPercent(PerformanceMetric.P50_FRAMETIME_MS));
        assertEquals(5.0, PerformanceAcceptancePolicy.maxRegressionPercent(PerformanceMetric.P95_FRAMETIME_MS));
        assertEquals(10.0, PerformanceAcceptancePolicy.maxRegressionPercent(PerformanceMetric.P99_FRAMETIME_MS));
        assertEquals(10.0, PerformanceAcceptancePolicy.maxRegressionPercent(PerformanceMetric.HEAP_MIB));
        assertEquals(10.0, PerformanceAcceptancePolicy.maxRegressionPercent(PerformanceMetric.ALLOCATION_MIB_S));
        assertEquals(10.0, PerformanceAcceptancePolicy.maxRegressionPercent(PerformanceMetric.RENDER_THREAD_CPU_PCT));
        assertEquals(5.0, PerformanceAcceptancePolicy.maxRegressionPercent(PerformanceMetric.AVERAGE_FPS));
    }

    @Test
    void operationalFrameMemoryAllocationCpuAndFpsRegressionsFailPastTheirBudgets() {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results = completeResults(0.0);
        results.put(PerformanceMetric.STARTUP_MS, result(10.01));
        results.put(PerformanceMetric.P95_FRAMETIME_MS, result(5.01));
        results.put(PerformanceMetric.HEAP_MIB, result(10.01));
        results.put(PerformanceMetric.ALLOCATION_MIB_S, result(10.01));
        results.put(PerformanceMetric.RENDER_THREAD_CPU_PCT, result(10.01));
        results.put(PerformanceMetric.AVERAGE_FPS, result(-5.01));

        var verdict = PerformanceAcceptancePolicy.evaluate(Map.copyOf(results));

        assertFalse(verdict.accepted());
        for (PerformanceMetric metric : new PerformanceMetric[]{
                PerformanceMetric.STARTUP_MS,
                PerformanceMetric.P95_FRAMETIME_MS,
                PerformanceMetric.HEAP_MIB,
                PerformanceMetric.ALLOCATION_MIB_S,
                PerformanceMetric.RENDER_THREAD_CPU_PCT,
                PerformanceMetric.AVERAGE_FPS}) {
            assertTrue(verdict.violations().stream().anyMatch(value -> value.contains(metric.name())), metric.name());
        }
    }

    @Test
    void missingAnyOperationalEvidenceCannotPass() {
        for (PerformanceMetric missing : PerformanceMetric.values()) {
            EnumMap<PerformanceMetric, PerformanceComparison.Result> results = completeResults(0.0);
            results.remove(missing);
            var verdict = PerformanceAcceptancePolicy.evaluate(Map.copyOf(results));
            assertFalse(verdict.accepted(), missing.name());
            assertTrue(
                    verdict.violations().stream().anyMatch(value -> value.contains(missing.name())),
                    missing.name());
        }
    }

    private static EnumMap<PerformanceMetric, PerformanceComparison.Result> completeResults(double delta) {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results =
                new EnumMap<>(PerformanceMetric.class);
        for (PerformanceMetric metric : PerformanceMetric.values()) results.put(metric, result(delta));
        return results;
    }

    private static PerformanceComparison.Result result(double delta) {
        return new PerformanceComparison.Result(
                100.0,
                100.0 + delta,
                delta,
                PerformanceComparison.Direction.EQUIVALENT,
                3,
                3);
    }
}
