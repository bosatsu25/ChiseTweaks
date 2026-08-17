package dev.chise.chisetweaks.performance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class PerformanceCaptureCsv {
    private static final String HEADER = String.join(",",
            "startup_ms",
            "p50_frametime_ms",
            "p95_frametime_ms",
            "p99_frametime_ms",
            "heap_mib",
            "allocation_mib_s",
            "render_thread_cpu_pct",
            "average_fps");

    private PerformanceCaptureCsv() {}

    static List<PerformanceRun> read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst().strip())) {
            throw new IllegalArgumentException(path + ": unsupported performance capture header");
        }
        List<PerformanceRun> runs = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] values = line.split(",", -1);
            if (values.length != 8) {
                throw new IllegalArgumentException(path + ": line " + (index + 1) + " must have 8 values");
            }
            runs.add(new PerformanceRun(
                    parse(values[0]), parse(values[1]), parse(values[2]), parse(values[3]),
                    parse(values[4]), parse(values[5]), parse(values[6]), parse(values[7])));
        }
        if (runs.size() < 3) {
            throw new IllegalArgumentException(path + ": expected at least three repeated runs");
        }
        return List.copyOf(runs);
    }

    static void write(Path path, List<PerformanceRun> runs) throws IOException {
        if (runs == null || runs.size() < 3) {
            throw new IllegalArgumentException("performance capture must contain at least three repeated runs");
        }
        Files.createDirectories(path.toAbsolutePath().normalize().getParent());
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (PerformanceRun run : runs) {
            lines.add(String.join(",",
                    Double.toString(run.startupMs()),
                    Double.toString(run.p50FrametimeMs()),
                    Double.toString(run.p95FrametimeMs()),
                    Double.toString(run.p99FrametimeMs()),
                    Double.toString(run.heapMiB()),
                    Double.toString(run.allocationMiBS()),
                    Double.toString(run.renderThreadCpuPct()),
                    Double.toString(run.averageFps())));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static double parse(String value) {
        return Double.parseDouble(value.strip());
    }
}
