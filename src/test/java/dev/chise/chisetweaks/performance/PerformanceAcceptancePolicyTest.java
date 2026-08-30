package dev.chise.chisetweaks.performance;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformanceAcceptancePolicyTest {
    @Test
    void representativeStableCandidatePasses() {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results = completeResults(0.0);
        results.put(PerformanceMetric.P50_FRAMETIME_MS, result(4.9));
        results.put(PerformanceMetric.P95_FRAMETIME_MS, result(4.9));
        results.put(PerformanceMetric.P99_FRAMETIME_MS, result(9.9));
        results.put(PerformanceMetric.ALLOCATION_MIB_S, result(9.9));
        results.put(PerformanceMetric.AVERAGE_FPS, result(-4.9));
        assertTrue(PerformanceAcceptancePolicy.evaluate(Map.copyOf(results)).accepted());
    }

    @Test
    void frameTimeMemoryAllocationAndFpsRegressionsFailPastTheirBudgets() {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results = completeResults(0.0);
        results.put(PerformanceMetric.P95_FRAMETIME_MS, result(5.01));
        results.put(PerformanceMetric.HEAP_MIB, result(10.01));
        results.put(PerformanceMetric.ALLOCATION_MIB_S, result(10.01));
        results.put(PerformanceMetric.AVERAGE_FPS, result(-5.01));
        var verdict = PerformanceAcceptancePolicy.evaluate(Map.copyOf(results));
        assertFalse(verdict.accepted());
        assertTrue(verdict.violations().stream().anyMatch(value -> value.contains("P95_FRAMETIME_MS")));
        assertTrue(verdict.violations().stream().anyMatch(value -> value.contains("HEAP_MIB")));
        assertTrue(verdict.violations().stream().anyMatch(value -> value.contains("ALLOCATION_MIB_S")));
        assertTrue(verdict.violations().stream().anyMatch(value -> value.contains("AVERAGE_FPS")));
    }

    @Test
    void anyMissingRequiredMetricEvidenceCannotPass() {
        for (PerformanceMetric missing : PerformanceMetric.values()) {
            EnumMap<PerformanceMetric, PerformanceComparison.Result> results = completeResults(0.0);
            results.remove(missing);
            var verdict = PerformanceAcceptancePolicy.evaluate(Map.copyOf(results));
            assertFalse(verdict.accepted(), () -> missing + " unexpectedly became optional");
            assertTrue(verdict.violations().stream().anyMatch(value -> value.contains(missing.name())));
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
