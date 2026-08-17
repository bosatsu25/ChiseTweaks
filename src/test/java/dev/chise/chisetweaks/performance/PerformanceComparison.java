package dev.chise.chisetweaks.performance;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PerformanceComparison {
    private static final int MIN_SAMPLES_PER_METRIC = 3;
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
            Direction direction,
            int baselineSamples,
            int candidateSamples) {}

    private PerformanceComparison() {}

    static Map<PerformanceMetric, Result> compare(
            PerformanceCapture baseline,
            PerformanceCapture candidate) {
        baseline.requireComparableWith(candidate);
        return compareRuns(baseline.runs(), candidate.runs());
    }

    static Map<PerformanceMetric, Result> compareRuns(
            List<PerformanceRun> baseline,
            List<PerformanceRun> candidate) {
        requireCaptureRuns("baseline", baseline);
        requireCaptureRuns("candidate", candidate);

        EnumMap<PerformanceMetric, Result> results = new EnumMap<>(PerformanceMetric.class);
        for (PerformanceMetric metric : PerformanceMetric.values()) {
            List<Double> baselineValues = values(baseline, metric);
            List<Double> candidateValues = values(candidate, metric);
            if (baselineValues.size() < MIN_SAMPLES_PER_METRIC ||
                    candidateValues.size() < MIN_SAMPLES_PER_METRIC) {
                continue;
            }
            double baselineMedian = medianValues(baselineValues);
            double candidateMedian = medianValues(candidateValues);
            Double delta = baselineMedian == 0.0
                    ? null
                    : ((candidateMedian - baselineMedian) / baselineMedian) * 100.0;
            results.put(metric, new Result(
                    baselineMedian,
                    candidateMedian,
                    delta,
                    direction(metric, delta),
                    baselineValues.size(),
                    candidateValues.size()));
        }
        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "performance captures share no metric with at least three samples on both sides");
        }
        return Map.copyOf(results);
    }

    static double median(List<PerformanceRun> runs, PerformanceMetric metric) {
        requireCaptureRuns("runs", runs);
        List<Double> values = values(runs, metric);
        if (values.size() < MIN_SAMPLES_PER_METRIC) {
            throw new IllegalArgumentException(
                    metric + " must contain at least three available samples");
        }
        return medianValues(values);
    }

    private static List<Double> values(List<PerformanceRun> runs, PerformanceMetric metric) {
        List<Double> values = new ArrayList<>(runs.size());
        for (PerformanceRun run : runs) {
            Double value = metric.valueOf(run);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static double medianValues(List<Double> values) {
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

    private static void requireCaptureRuns(String label, List<PerformanceRun> runs) {
        if (runs == null || runs.size() < MIN_SAMPLES_PER_METRIC) {
            throw new IllegalArgumentException(label + " must contain at least three repeated runs");
        }
    }
}
