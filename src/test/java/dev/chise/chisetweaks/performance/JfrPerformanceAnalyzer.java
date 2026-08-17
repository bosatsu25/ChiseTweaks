package dev.chise.chisetweaks.performance;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

final class JfrPerformanceAnalyzer {
    record Summary(
            long allocationBytes,
            long garbageCollections,
            Duration garbageCollectionPause,
            Duration observedSpan) {}

    private JfrPerformanceAnalyzer() {}

    static Summary analyze(Path recording) throws IOException {
        if (!Files.isRegularFile(recording)) {
            throw new IllegalArgumentException("JFR recording does not exist: " + recording);
        }

        List<RecordedEvent> events = RecordingFile.readAllEvents(recording);
        long allocationBytes = 0L;
        long garbageCollections = 0L;
        Duration garbageCollectionPause = Duration.ZERO;
        Instant first = null;
        Instant last = null;

        for (RecordedEvent event : events) {
            Instant start = event.getStartTime();
            Instant end = event.getEndTime();
            if (first == null || start.isBefore(first)) {
                first = start;
            }
            if (last == null || end.isAfter(last)) {
                last = end;
            }

            String type = event.getEventType().getName();
            if ("jdk.ObjectAllocationInNewTLAB".equals(type)) {
                allocationBytes = Math.addExact(allocationBytes, longField(event, "tlabSize"));
            } else if ("jdk.ObjectAllocationOutsideTLAB".equals(type)) {
                allocationBytes = Math.addExact(allocationBytes, longField(event, "allocationSize"));
            } else if ("jdk.GarbageCollection".equals(type)) {
                garbageCollections++;
                garbageCollectionPause = garbageCollectionPause.plus(event.getDuration());
            }
        }

        Duration observedSpan = first == null || last == null
                ? Duration.ZERO
                : Duration.between(first, last);
        return new Summary(
                allocationBytes,
                garbageCollections,
                garbageCollectionPause,
                observedSpan);
    }

    private static long longField(RecordedEvent event, String name) {
        if (event.getEventType().getField(name) == null) {
            return 0L;
        }
        return event.getLong(name);
    }
}
