package dev.chise.chisetweaks.performance;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("performance")
final class PerformanceComparisonTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void comparesRepeatedMediansWithMetricDirection() {
        List<PerformanceRun> baseline = runs(10.0, 10.0, 10.0, 100.0);
        List<PerformanceRun> candidate = runs(9.0, 9.0, 9.0, 110.0);

        Map<PerformanceMetric, PerformanceComparison.Result> result =
                PerformanceComparison.compare(baseline, candidate);

        assertEquals(PerformanceComparison.Direction.BETTER,
                result.get(PerformanceMetric.P99_FRAMETIME_MS).direction());
        assertEquals(PerformanceComparison.Direction.BETTER,
                result.get(PerformanceMetric.AVERAGE_FPS).direction());
        assertEquals(-10.0,
                result.get(PerformanceMetric.P99_FRAMETIME_MS).deltaPercent(), 0.0001);
        assertEquals(10.0,
                result.get(PerformanceMetric.AVERAGE_FPS).deltaPercent(), 0.0001);
    }

    @Test
    void zeroBaselineIsReportedAsUndefinedInsteadOfDividingByZero() {
        List<PerformanceRun> baseline = runs(0.0, 0.0, 0.0, 0.0);
        List<PerformanceRun> candidate = runs(1.0, 1.0, 1.0, 1.0);

        PerformanceComparison.Result result = PerformanceComparison.compare(baseline, candidate)
                .get(PerformanceMetric.P95_FRAMETIME_MS);

        assertEquals(PerformanceComparison.Direction.UNDEFINED, result.direction());
        assertEquals(null, result.deltaPercent());
    }

    @Test
    void csvCaptureRoundTripsWithoutTrackedExampleFiles() throws Exception {
        List<PerformanceRun> expected = List.of(
                run(10.0, 8.0, 9.0, 10.0, 512.0, 3.0, 25.0, 120.0),
                run(11.0, 8.5, 9.5, 10.5, 520.0, 3.2, 26.0, 118.0),
                run(9.0, 7.5, 8.5, 9.5, 508.0, 2.8, 24.0, 122.0));
        Path capture = temporaryDirectory.resolve("candidate.csv");

        PerformanceCaptureCsv.write(capture, expected);

        assertEquals(expected, PerformanceCaptureCsv.read(capture));
    }

    @Test
    void rejectsSingleRunEvidence() {
        assertThrows(IllegalArgumentException.class,
                () -> PerformanceComparison.compare(
                        List.of(run(1, 1, 1, 1, 1, 1, 1, 1)),
                        List.of(run(1, 1, 1, 1, 1, 1, 1, 1))));
    }

    private static List<PerformanceRun> runs(
            double p50,
            double p95,
            double p99,
            double fps) {
        return List.of(
                run(1000, p50, p95, p99, 512, 2, 20, fps),
                run(1000, p50, p95, p99, 512, 2, 20, fps),
                run(1000, p50, p95, p99, 512, 2, 20, fps));
    }

    private static PerformanceRun run(
            double startup,
            double p50,
            double p95,
            double p99,
            double heap,
            double allocation,
            double cpu,
            double fps) {
        return new PerformanceRun(startup, p50, p95, p99, heap, allocation, cpu, fps);
    }
}
