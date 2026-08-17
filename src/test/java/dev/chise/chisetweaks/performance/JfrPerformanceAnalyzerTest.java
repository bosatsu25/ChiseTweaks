package dev.chise.chisetweaks.performance;

import jdk.jfr.Recording;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(summary.garbageCollections() >= 0L);
        assertTrue(!summary.garbageCollectionPause().isNegative());
        assertTrue(!summary.observedSpan().isNegative());
    }

    @Test
    void rejectsMissingRecording() {
        Path missing = temporaryDirectory.resolve("missing.jfr");
        assertThrows(IllegalArgumentException.class,
                () -> JfrPerformanceAnalyzer.analyze(missing));
    }
}
