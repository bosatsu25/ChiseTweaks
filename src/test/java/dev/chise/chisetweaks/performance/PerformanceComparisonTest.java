package dev.chise.chisetweaks.performance;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("performance")
final class PerformanceComparisonTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void comparesRepeatedMediansWithMetricDirection() {
        PerformanceCapture baseline = capture("baseline", runs(10.0, 10.0, 10.0, 100.0));
        PerformanceCapture candidate = capture("candidate", runs(9.0, 9.0, 9.0, 110.0));

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
        PerformanceCapture baseline = capture("baseline", runs(0.0, 0.0, 0.0, 0.0));
        PerformanceCapture candidate = capture("candidate", runs(1.0, 1.0, 1.0, 1.0));

        PerformanceComparison.Result result = PerformanceComparison.compare(baseline, candidate)
                .get(PerformanceMetric.P95_FRAMETIME_MS);

        assertEquals(PerformanceComparison.Direction.UNDEFINED, result.direction());
        assertEquals(null, result.deltaPercent());
    }

    @Test
    void csvCaptureRoundTripsMetadataAndMissingMetrics() throws Exception {
        PerformanceCapture expected = capture("candidate", List.of(
                run(10.0, 8.0, 9.0, 10.0, 512.0, null, 25.0, 120.0),
                run(11.0, 8.5, 9.5, 10.5, 520.0, null, 26.0, 118.0),
                run(9.0, 7.5, 8.5, 9.5, 508.0, null, 24.0, 122.0)));
        Path capture = temporaryDirectory.resolve("candidate.csv");

        PerformanceCaptureCsv.write(capture, expected);

        assertEquals(expected, PerformanceCaptureCsv.read(capture));
    }

    @Test
    void unavailableMetricIsSkippedInsteadOfFabricatingZero() {
        PerformanceCapture baseline = capture("baseline", List.of(
                run(1000.0, 8.0, 9.0, 10.0, 512.0, null, 20.0, 120.0),
                run(1000.0, 8.0, 9.0, 10.0, 512.0, null, 20.0, 120.0),
                run(1000.0, 8.0, 9.0, 10.0, 512.0, null, 20.0, 120.0)));
        PerformanceCapture candidate = capture("candidate", List.of(
                run(1000.0, 7.0, 8.0, 9.0, 500.0, null, 19.0, 130.0),
                run(1000.0, 7.0, 8.0, 9.0, 500.0, null, 19.0, 130.0),
                run(1000.0, 7.0, 8.0, 9.0, 500.0, null, 19.0, 130.0)));

        Map<PerformanceMetric, PerformanceComparison.Result> result =
                PerformanceComparison.compare(baseline, candidate);

        assertFalse(result.containsKey(PerformanceMetric.ALLOCATION_MIB_S));
    }

    @Test
    void rejectsDifferentControlledEnvironments() {
        Map<String, String> candidateMetadata = metadata("candidate");
        candidateMetadata.put("environment_id", "different-instance");
        PerformanceCapture baseline = capture("baseline", runs(10, 10, 10, 100));
        PerformanceCapture candidate = new PerformanceCapture(candidateMetadata, runs(9, 9, 9, 110));

        assertThrows(IllegalArgumentException.class,
                () -> PerformanceComparison.compare(baseline, candidate));
    }

    @Test
    void rejectsSingleRunEvidence() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerformanceCapture(
                        metadata("candidate"),
                        List.of(run(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0))));
    }

    private static PerformanceCapture capture(String variant, List<PerformanceRun> runs) {
        return new PerformanceCapture(metadata(variant), runs);
    }

    private static Map<String, String> metadata(String variant) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("scenario", "dense-kelp");
        metadata.put("minecraft", "26.1.2");
        metadata.put("java", "25");
        metadata.put("fabric_loader", "0.19.3");
        metadata.put("environment_id", "prism-clone-a");
        metadata.put("variant", variant);
        return metadata;
    }

    private static List<PerformanceRun> runs(
            double p50,
            double p95,
            double p99,
            double fps) {
        return List.of(
                run(1000.0, p50, p95, p99, 512.0, 2.0, 20.0, fps),
                run(1000.0, p50, p95, p99, 512.0, 2.0, 20.0, fps),
                run(1000.0, p50, p95, p99, 512.0, 2.0, 20.0, fps));
    }

    private static PerformanceRun run(
            Double startup,
            Double p50,
            Double p95,
            Double p99,
            Double heap,
            Double allocation,
            Double cpu,
            Double fps) {
        return new PerformanceRun(startup, p50, p95, p99, heap, allocation, cpu, fps);
    }
}
