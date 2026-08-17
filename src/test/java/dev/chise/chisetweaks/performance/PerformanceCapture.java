package dev.chise.chisetweaks.performance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

record PerformanceCapture(Map<String, String> metadata, List<PerformanceRun> runs) {
    private static final Set<String> REQUIRED_METADATA = Set.of(
            "scenario",
            "minecraft",
            "java",
            "fabric_loader",
            "environment_id",
            "variant");
    private static final Set<String> COMPARABLE_METADATA = Set.of(
            "scenario",
            "minecraft",
            "java",
            "fabric_loader",
            "environment_id");

    PerformanceCapture {
        if (metadata == null) {
            throw new IllegalArgumentException("performance capture metadata is required");
        }
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.strip();
            String normalizedValue = value == null ? "" : value.strip();
            if (normalizedKey.isEmpty() || normalizedValue.isEmpty()) {
                throw new IllegalArgumentException("performance capture metadata keys and values must be non-empty");
            }
            normalized.put(normalizedKey, normalizedValue);
        });
        for (String key : REQUIRED_METADATA) {
            if (!normalized.containsKey(key)) {
                throw new IllegalArgumentException("performance capture metadata is missing required key: " + key);
            }
        }
        if (runs == null || runs.size() < 3) {
            throw new IllegalArgumentException("performance capture must contain at least three repeated runs");
        }
        metadata = Map.copyOf(normalized);
        runs = List.copyOf(runs);
    }

    void requireComparableWith(PerformanceCapture other) {
        if (other == null) {
            throw new IllegalArgumentException("candidate performance capture is required");
        }
        for (String key : COMPARABLE_METADATA) {
            String baselineValue = metadata.get(key);
            String candidateValue = other.metadata.get(key);
            if (!baselineValue.equals(candidateValue)) {
                throw new IllegalArgumentException(
                        "performance captures are not comparable: metadata '" + key + "' differs (" +
                        baselineValue + " != " + candidateValue + ")");
            }
        }
    }
}
