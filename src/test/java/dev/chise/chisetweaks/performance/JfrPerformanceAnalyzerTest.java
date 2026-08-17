package dev.chise.chisetweaks.performance;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("performance")
final class JfrPerformanceAnalyzerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readsARealJava25JfrWithoutAddingRuntimeInstrumentationToTheMod() throws Exception {
        Path recordingPath = temporaryDirectory.resolve("minimal.jfr");
        try (Recording recording = new Recording()) {
            recording.start();
            recording.stop();
            recording.dump(recordingPath);
        }

        JfrPerformanceAnalyzer.Summary summary = JfrPerformanceAnalyzer.analyze(recordingPath);

        assertTrue(summary.allocationBytes() >= 0L);
        assertTrue(summary.allocationEvents() >= 0L);
        assertTrue(summary.garbageCollections() >= 0L);
        assertFalse(summary.garbageCollectionPause().isNegative());
        assertFalse(summary.observedSpan().isNegative());
    }

    @Test
    void analyzerSourceUsesStreamingAndStandardAllocationSampleContract() throws Exception {
        String source = Files.readString(Path.of(
                "src/test/java/dev/chise/chisetweaks/performance/JfrPerformanceAnalyzer.java"));
        assertTrue(source.contains("new RecordingFile(recording)"));
        assertTrue(source.contains("while (file.hasMoreEvents())"));
        assertTrue(source.contains("file.readEvent()"));
        assertFalse(source.contains("RecordingFile.readAllEvents"));
        assertTrue(source.contains("jdk.ObjectAllocationSample"));
        assertTrue(source.contains("longField(event, \"weight\")"));
        assertTrue(source.contains("event.getDuration(\"sumOfPauses\")"));
    }

    @Test
    void rejectsMissingRecording() {
        Path missing = temporaryDirectory.resolve("missing.jfr");
        assertThrows(IllegalArgumentException.class,
                () -> JfrPerformanceAnalyzer.analyze(missing));
    }
}
