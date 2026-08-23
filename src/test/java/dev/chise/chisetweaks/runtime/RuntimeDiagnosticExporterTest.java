package dev.chise.chisetweaks.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeDiagnosticExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void pruningKeepsTenNewestReportsAndLeavesUnrelatedFilesUntouched() throws Exception {
        for (int index = 0; index < 12; index++) {
            Files.writeString(tempDir.resolve(String.format(
                    "chisetweaks-diagnostic-20260823-1200%02d-000.txt", index)), "snapshot");
        }
        Path unrelated = tempDir.resolve("notes.txt");
        Files.writeString(unrelated, "keep");

        RuntimeDiagnosticExporter.pruneOldReports(tempDir);

        try (var files = Files.list(tempDir)) {
            long reportCount = files
                    .filter(path -> path.getFileName().toString().startsWith("chisetweaks-diagnostic-"))
                    .count();
            assertEquals(RuntimeDiagnosticExporter.MAX_RETAINED_REPORTS, reportCount);
        }
        assertTrue(Files.exists(unrelated));
        assertTrue(Files.exists(tempDir.resolve("chisetweaks-diagnostic-20260823-120002-000.txt")));
        assertTrue(Files.exists(tempDir.resolve("chisetweaks-diagnostic-20260823-120011-000.txt")));
    }
}
