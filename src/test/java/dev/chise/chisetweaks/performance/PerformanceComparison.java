package dev.chise.chisetweaks.performance;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PerformanceComparison {
    private static final double EQUIVALENT_DELTA_PERCENT = 0.01;

    enum Direction {
        BETTER,
        WORSE,
        EQUIVALENT,
        UNDEFINED
    }

    record Result(
            double baselineMedian,
            double candidateMedian,
            Double deltaPercent,
            Direction direction) {}

    private PerformanceComparison() {}

    static Map<PerformanceMetric, Result> compare(
            List<PerformanceRun> baseline,
            List<PerformanceRun> candidate) {
        requireRepeatedRuns("baseline", baseline);
        requireRepeatedRuns("candidate", candidate);

        EnumMap<PerformanceMetric, Result> results = new EnumMap<>(PerformanceMetric.class);
        for (PerformanceMetric metric : PerformanceMetric.values()) {
            double baselineMedian = median(baseline, metric);
            double candidateMedian = median(candidate, metric);
            Double delta = baselineMedian == 0.0
                    ? null
                    : ((candidateMedian - baselineMedian) / baselineMedian) * 100.0;
            results.put(metric, new Result(
                    baselineMedian,
                    candidateMedian,
                    delta,
                    direction(metric, delta)));
        }
        return Map.copyOf(results);
    }

    static double median(List<PerformanceRun> runs, PerformanceMetric metric) {
        requireRepeatedRuns("runs", runs);
        List<Double> values = new ArrayList<>(runs.size());
        for (PerformanceRun run : runs) {
            values.add(metric.valueOf(run));
        }
        values.sort(Double::compareTo);
        int middle = values.size() / 2;
        if ((values.size() & 1) == 1) {
            return values.get(middle);
        }
        return (values.get(middle - 1) + values.get(middle)) / 2.0;
    }

    private static Direction direction(PerformanceMetric metric, Double delta) {
        if (delta == null) {
            return Direction.UNDEFINED;
        }
        if (Math.abs(delta) < EQUIVALENT_DELTA_PERCENT) {
            return Direction.EQUIVALENT;
        }
        boolean improved = metric.lowerIsBetter() ? delta < 0.0 : delta > 0.0;
        return improved ? Direction.BETTER : Direction.WORSE;
    }

    private static void requireRepeatedRuns(String label, List<PerformanceRun> runs) {
        if (runs == null || runs.size() < 3) {
            throw new IllegalArgumentException(label + " must contain at least three repeated runs");
        }
    }
}
