package dev.chise.chisetweaks.performance;

import java.nio.file.Path;
import java.util.Map;

/** Manual entry point for comparing real Prism performance captures without runtime instrumentation. */
public final class PerformanceEvidenceCli {
    private PerformanceEvidenceCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2 && args.length != 4) {
            throw new IllegalArgumentException(
                    "expected: <baseline.csv> <candidate.csv> [<baseline.jfr> <candidate.jfr>]");
        }

        Path baselinePath = Path.of(args[0]);
        Path candidatePath = Path.of(args[1]);
        PerformanceCapture baseline = PerformanceCaptureCsv.read(baselinePath);
        PerformanceCapture candidate = PerformanceCaptureCsv.read(candidatePath);
        Map<PerformanceMetric, PerformanceComparison.Result> comparison =
                PerformanceComparison.compare(baseline, candidate);

        System.out.println("baseline=" + baseline.metadata().get("variant") + " (" + baselinePath + ")");
        System.out.println("candidate=" + candidate.metadata().get("variant") + " (" + candidatePath + ")");
        System.out.println("scenario=" + baseline.metadata().get("scenario"));
        System.out.println("environment_id=" + baseline.metadata().get("environment_id"));
        System.out.println("metric\tbaseline_median\tcandidate_median\tdelta_pct\tdirection\tsamples");
        for (PerformanceMetric metric : PerformanceMetric.values()) {
            PerformanceComparison.Result result = comparison.get(metric);
            if (result == null) {
                continue;
            }
            String delta = result.deltaPercent() == null
                    ? "n/a"
                    : String.format(java.util.Locale.ROOT, "%+.3f", result.deltaPercent());
            System.out.printf(
                    java.util.Locale.ROOT,
                    "%s\t%.3f\t%.3f\t%s\t%s\t%d/%d%n",
                    metric,
                    result.baselineMedian(),
                    result.candidateMedian(),
                    delta,
                    result.direction(),
                    result.baselineSamples(),
                    result.candidateSamples());
        }

        if (args.length == 4) {
            printJfr("baseline_jfr", Path.of(args[2]));
            printJfr("candidate_jfr", Path.of(args[3]));
        }
    }

    private static void printJfr(String label, Path path) throws Exception {
        JfrPerformanceAnalyzer.Summary summary = JfrPerformanceAnalyzer.analyze(path);
        System.out.printf(
                java.util.Locale.ROOT,
                "%s=%s allocation_bytes=%d allocation_mode=%s allocation_events=%d gc_count=%d gc_pause_ms=%.3f span_ms=%.3f%n",
                label,
                path,
                summary.allocationBytes(),
                summary.allocationMode(),
                summary.allocationEvents(),
                summary.garbageCollections(),
                summary.garbageCollectionPause().toNanos() / 1_000_000.0,
                summary.observedSpan().toNanos() / 1_000_000.0);
    }
}
