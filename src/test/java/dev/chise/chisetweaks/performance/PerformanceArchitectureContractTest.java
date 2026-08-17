package dev.chise.chisetweaks.performance;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("performance")
final class PerformanceArchitectureContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void performanceEvidenceLogicStaysOutOfTheRuntimeJar() throws IOException {
        Path mainJava = ROOT.resolve("src/main/java");
        List<String> forbiddenRuntimeProfilerTokens = List.of(
                "import jdk.jfr.",
                "RecordingFile",
                "FlightRecorder",
                "com.sun.management");

        try (var paths = Files.walk(mainJava)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.toString().endsWith(".java"))
                    .toList()) {
                String source = Files.readString(path);
                for (String token : forbiddenRuntimeProfilerTokens) {
                    assertFalse(source.contains(token),
                            () -> "Runtime source must not embed profiler token " + token + ": " + path);
                }
            }
        }
    }

    @Test
    void performanceSpecificationIsExecutableCodeNotTrackedDocsOrPython() {
        assertFalse(Files.exists(ROOT.resolve("docs/PERFORMANCE_SQA.md")));
        assertFalse(Files.exists(ROOT.resolve("docs/performance-capture.example.json")));
        assertFalse(Files.exists(ROOT.resolve("scripts/performance_compare.py")));
        assertTrue(Files.exists(ROOT.resolve(
                "src/test/java/dev/chise/chisetweaks/performance/PerformanceComparison.java")));
        assertTrue(Files.exists(ROOT.resolve(
                "src/test/java/dev/chise/chisetweaks/performance/JfrPerformanceAnalyzer.java")));
        assertTrue(Files.exists(ROOT.resolve(
                "src/test/java/dev/chise/chisetweaks/performance/PerformanceEvidenceCli.java")));
    }

    @Test
    void ciUsesOneGradleGateAndReleaseReusesExactCiEvidence() throws IOException {
        String build = Files.readString(ROOT.resolve("build.gradle"));
        String verify = Files.readString(ROOT.resolve(".github/workflows/verify-build.yml"));
        String release = Files.readString(ROOT.resolve(".github/workflows/release.yml"));

        assertTrue(build.contains("tasks.register('ciGate')"));
        assertTrue(build.contains("tasks.register('comparePerformanceEvidence', JavaExec)"));
        assertFalse(build.contains("tasks.register('performanceGate', Test)"));

        assertTrue(verify.contains("./gradlew --no-daemon --stacktrace clean ciGate"));
        assertFalse(verify.contains("clean qualityGate build"));
        assertFalse(verify.contains("Wait before retrying"));

        assertFalse(release.contains("uses: ./.github/workflows/verify-build.yml"));
        assertTrue(release.contains("--commit \"$GITHUB_SHA\""));
        assertTrue(release.contains("gh run download \"$RUN_ID\""));
    }

    @Test
    void zeroScanOreCompatibilityDoesNotReintroduceTheOldAllModWrapperPattern() throws IOException {
        Path plugin = ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String source = Files.readString(plugin);

        assertFalse(source.contains("if (!\"minecraft\".equals(namespace))"),
                "Non-Minecraft blocks must not be wrapped unconditionally in the steady-state visual path");
    }
}
