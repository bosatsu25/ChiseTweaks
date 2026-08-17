package dev.chise.chisetweaks.performance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PerformanceCaptureCsv {
    private static final String METADATA_PREFIX = "# ";
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

    static PerformanceCapture read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Map<String, String> metadata = new LinkedHashMap<>();
        List<PerformanceRun> runs = new ArrayList<>();
        boolean headerSeen = false;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).strip();
            if (line.isEmpty()) {
                continue;
            }
            if (!headerSeen && line.startsWith(METADATA_PREFIX)) {
                String declaration = line.substring(METADATA_PREFIX.length());
                int separator = declaration.indexOf('=');
                if (separator <= 0 || separator == declaration.length() - 1) {
                    throw new IllegalArgumentException(path + ": invalid metadata line " + (index + 1));
                }
                metadata.put(
                        declaration.substring(0, separator).strip(),
                        declaration.substring(separator + 1).strip());
                continue;
            }
            if (!headerSeen) {
                if (!HEADER.equals(line)) {
                    throw new IllegalArgumentException(path + ": unsupported performance capture header");
                }
                headerSeen = true;
                continue;
            }

            String[] values = line.split(",", -1);
            if (values.length != 8) {
                throw new IllegalArgumentException(path + ": line " + (index + 1) + " must have 8 values");
            }
            runs.add(new PerformanceRun(
                    parseOptional(values[0]), parseOptional(values[1]),
                    parseOptional(values[2]), parseOptional(values[3]),
                    parseOptional(values[4]), parseOptional(values[5]),
                    parseOptional(values[6]), parseOptional(values[7])));
        }
        if (!headerSeen) {
            throw new IllegalArgumentException(path + ": performance capture header is missing");
        }
        return new PerformanceCapture(metadata, runs);
    }

    static void write(Path path, PerformanceCapture capture) throws IOException {
        Files.createDirectories(path.toAbsolutePath().normalize().getParent());
        List<String> lines = new ArrayList<>();
        capture.metadata().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> lines.add(METADATA_PREFIX + entry.getKey() + "=" + entry.getValue()));
        lines.add(HEADER);
        for (PerformanceRun run : capture.runs()) {
            lines.add(String.join(",",
                    format(run.startupMs()),
                    format(run.p50FrametimeMs()),
                    format(run.p95FrametimeMs()),
                    format(run.p99FrametimeMs()),
                    format(run.heapMiB()),
                    format(run.allocationMiBS()),
                    format(run.renderThreadCpuPct()),
                    format(run.averageFps())));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static Double parseOptional(String value) {
        String stripped = value.strip();
        return stripped.isEmpty() ? null : Double.parseDouble(stripped);
    }

    private static String format(Double value) {
        return value == null ? "" : Double.toString(value);
    }
}
