package dev.chise.chisetweaks.performance;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S4: locks the real-device performance acceptance budget and required evidence set. */
final class PerformanceAcceptanceSGradeTest {

    @Test
    void sGradeThresholdsStayStrict() {
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
    void allEightMetricsAreRequiredForAcceptance() {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results = zeroRegressionResults();
        assertTrue(PerformanceAcceptancePolicy.evaluate(results).accepted());

        results.remove(PerformanceMetric.ALLOCATION_MIB_S);
        PerformanceAcceptancePolicy.Verdict verdict = PerformanceAcceptancePolicy.evaluate(results);
        assertFalse(verdict.accepted());
        assertTrue(verdict.violations().stream().anyMatch(value -> value.contains("ALLOCATION_MIB_S")));
    }

    @Test
    void p95RegressionAboveFivePercentFails() {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results = zeroRegressionResults();
        results.put(PerformanceMetric.P95_FRAMETIME_MS, result(5.01));
        assertFalse(PerformanceAcceptancePolicy.evaluate(results).accepted());
    }

    @Test
    void p99AtBudgetPassesButAboveBudgetFails() {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results = zeroRegressionResults();
        results.put(PerformanceMetric.P99_FRAMETIME_MS, result(10.0));
        assertTrue(PerformanceAcceptancePolicy.evaluate(results).accepted());
        results.put(PerformanceMetric.P99_FRAMETIME_MS, result(10.01));
        assertFalse(PerformanceAcceptancePolicy.evaluate(results).accepted());
    }

    private static EnumMap<PerformanceMetric, PerformanceComparison.Result> zeroRegressionResults() {
        EnumMap<PerformanceMetric, PerformanceComparison.Result> results = new EnumMap<>(PerformanceMetric.class);
        for (PerformanceMetric metric : PerformanceMetric.values()) results.put(metric, result(0.0));
        return results;
    }

    private static PerformanceComparison.Result result(double delta) {
        return new PerformanceComparison.Result(
                100.0,
                100.0 * (1.0 + delta / 100.0),
                delta,
                Math.abs(delta) < 0.01
                        ? PerformanceComparison.Direction.EQUIVALENT
                        : PerformanceComparison.Direction.WORSE,
                3,
                3);
    }
}
