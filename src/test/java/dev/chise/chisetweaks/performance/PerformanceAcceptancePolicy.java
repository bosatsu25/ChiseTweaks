package dev.chise.chisetweaks.performance;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Regression-oriented acceptance rules for repeatable Prism performance captures. */
final class PerformanceAcceptancePolicy {
    private static final Set<PerformanceMetric> REQUIRED_METRICS = Set.of(
            PerformanceMetric.STARTUP_MS,
            PerformanceMetric.P50_FRAMETIME_MS,
            PerformanceMetric.P95_FRAMETIME_MS,
            PerformanceMetric.P99_FRAMETIME_MS,
            PerformanceMetric.HEAP_MIB,
            PerformanceMetric.ALLOCATION_MIB_S,
            PerformanceMetric.RENDER_THREAD_CPU_PCT,
            PerformanceMetric.AVERAGE_FPS);

    private static final Map<PerformanceMetric, Double> MAX_REGRESSION_PERCENT = thresholds();

    record Verdict(boolean accepted, List<String> violations) {
        Verdict {
            violations = List.copyOf(violations);
        }
    }

    private PerformanceAcceptancePolicy() {}

    static Verdict evaluate(Map<PerformanceMetric, PerformanceComparison.Result> comparison) {
        if (comparison == null) throw new IllegalArgumentException("comparison is required");
        List<String> violations = new ArrayList<>();

        for (PerformanceMetric required : REQUIRED_METRICS) {
            if (!comparison.containsKey(required)) {
                violations.add(required + " requires at least three samples in baseline and candidate");
            }
        }

        for (Map.Entry<PerformanceMetric, PerformanceComparison.Result> entry : comparison.entrySet()) {
            PerformanceMetric metric = entry.getKey();
            PerformanceComparison.Result result = entry.getValue();
            Double delta = result.deltaPercent();
            if (delta == null) {
                violations.add(metric + " has an undefined percentage delta because the baseline median is zero");
                continue;
            }
            double allowed = MAX_REGRESSION_PERCENT.get(metric);
            double regression = metric.lowerIsBetter() ? delta : -delta;
            if (regression > allowed) {
                violations.add(String.format(
                        java.util.Locale.ROOT,
                        "%s regressed %.3f%% (allowed %.3f%%)",
                        metric,
                        regression,
                        allowed));
            }
        }
        return new Verdict(violations.isEmpty(), violations);
    }

    static double maxRegressionPercent(PerformanceMetric metric) {
        Double threshold = MAX_REGRESSION_PERCENT.get(metric);
        if (threshold == null) throw new IllegalArgumentException("unknown metric: " + metric);
        return threshold;
    }

    private static Map<PerformanceMetric, Double> thresholds() {
        EnumMap<PerformanceMetric, Double> values = new EnumMap<>(PerformanceMetric.class);
        values.put(PerformanceMetric.STARTUP_MS, 10.0);
        values.put(PerformanceMetric.P50_FRAMETIME_MS, 5.0);
        values.put(PerformanceMetric.P95_FRAMETIME_MS, 5.0);
        values.put(PerformanceMetric.P99_FRAMETIME_MS, 10.0);
        values.put(PerformanceMetric.HEAP_MIB, 10.0);
        values.put(PerformanceMetric.ALLOCATION_MIB_S, 10.0);
        values.put(PerformanceMetric.RENDER_THREAD_CPU_PCT, 10.0);
        values.put(PerformanceMetric.AVERAGE_FPS, 5.0);
        return Map.copyOf(values);
    }
}
