package dev.chise.chisetweaks.performance;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

final class JfrPerformanceAnalyzer {
    enum AllocationMode {
        EXACT,
        SAMPLED,
        NONE
    }

    record Summary(
            long allocationBytes,
            AllocationMode allocationMode,
            long allocationEvents,
            long garbageCollections,
            Duration garbageCollectionPause,
            Duration observedSpan) {}

    private JfrPerformanceAnalyzer() {}

    static Summary analyze(Path recording) throws IOException {
        if (!Files.isRegularFile(recording)) {
            throw new IllegalArgumentException("JFR recording does not exist: " + recording);
        }

        long exactAllocationBytes = 0L;
        long exactAllocationEvents = 0L;
        long sampledAllocationBytes = 0L;
        long sampledAllocationEvents = 0L;
        long garbageCollections = 0L;
        Duration garbageCollectionPause = Duration.ZERO;
        Instant first = null;
        Instant last = null;

        // Stream events one at a time so large Minecraft recordings do not need to fit in heap.
        try (RecordingFile file = new RecordingFile(recording)) {
            while (file.hasMoreEvents()) {
                RecordedEvent event = file.readEvent();
                Instant start = event.getStartTime();
                Instant end = event.getEndTime();
                if (first == null || start.isBefore(first)) {
                    first = start;
                }
                if (last == null || end.isAfter(last)) {
                    last = end;
                }

                String type = event.getEventType().getName();
                switch (type) {
                    case "jdk.ObjectAllocationInNewTLAB" -> {
                        exactAllocationBytes = Math.addExact(
                                exactAllocationBytes, longField(event, "tlabSize"));
                        exactAllocationEvents++;
                    }
                    case "jdk.ObjectAllocationOutsideTLAB" -> {
                        exactAllocationBytes = Math.addExact(
                                exactAllocationBytes, longField(event, "allocationSize"));
                        exactAllocationEvents++;
                    }
                    case "jdk.ObjectAllocationSample" -> {
                        sampledAllocationBytes = Math.addExact(
                                sampledAllocationBytes, longField(event, "weight"));
                        sampledAllocationEvents++;
                    }
                    case "jdk.GarbageCollection" -> {
                        garbageCollections++;
                        if (event.hasField("sumOfPauses")) {
                            garbageCollectionPause = garbageCollectionPause.plus(
                                    event.getDuration("sumOfPauses"));
                        }
                    }
                    default -> {
                        // Other JFR events are intentionally ignored by this small aggregate reader.
                    }
                }
            }
        }

        AllocationMode allocationMode;
        long allocationBytes;
        long allocationEvents;
        if (exactAllocationEvents > 0L) {
            // Prefer exact allocation events if an explicitly high-detail recording enabled them;
            // do not add sampled weights as that would double count allocation pressure.
            allocationMode = AllocationMode.EXACT;
            allocationBytes = exactAllocationBytes;
            allocationEvents = exactAllocationEvents;
        } else if (sampledAllocationEvents > 0L) {
            allocationMode = AllocationMode.SAMPLED;
            allocationBytes = sampledAllocationBytes;
            allocationEvents = sampledAllocationEvents;
        } else {
            allocationMode = AllocationMode.NONE;
            allocationBytes = 0L;
            allocationEvents = 0L;
        }

        Duration observedSpan = first == null || last == null
                ? Duration.ZERO
                : Duration.between(first, last);
        return new Summary(
                allocationBytes,
                allocationMode,
                allocationEvents,
                garbageCollections,
                garbageCollectionPause,
                observedSpan);
    }

    private static long longField(RecordedEvent event, String name) {
        if (!event.hasField(name)) {
            return 0L;
        }
        return event.getLong(name);
    }
}
