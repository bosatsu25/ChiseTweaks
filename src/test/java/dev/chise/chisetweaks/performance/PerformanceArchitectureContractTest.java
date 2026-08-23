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
    void ciOwnsQualityGatesWhileReleaseOnlyRebuildsAndAuditsArtifacts() throws IOException {
        String build = Files.readString(ROOT.resolve("build.gradle"));
        String ci = Files.readString(ROOT.resolve(".github/workflows/ci.yml"));
        String verify = Files.readString(ROOT.resolve(".github/workflows/verify-build.yml"));
        String release = Files.readString(ROOT.resolve(".github/workflows/release.yml"));

        assertTrue(build.contains("tasks.register('ciGate')"));
        assertTrue(build.contains("tasks.register('comparePerformanceEvidence', JavaExec)"));
        assertFalse(build.contains("tasks.register('performanceGate', Test)"));

        assertTrue(verify.contains("clean assemble testClasses"));
        assertTrue(verify.contains("--stacktrace test"));
        assertTrue(verify.contains("jacocoTestReport jacocoTestCoverageVerification"));
        assertTrue(verify.contains("--stacktrace pitest"));
        assertTrue(verify.contains("Enforce aggregate quality gate"));
        assertTrue(verify.contains("actions/upload-artifact@"));
        assertTrue(verify.contains("path: build/libs/${{ steps.artifacts.outputs.runtime_jar }}"));
        assertTrue(verify.contains("archive: false"));
        assertFalse(verify.contains("gh release"));
        assertFalse(ci.contains("gh release"));
        assertFalse(ci.contains("git tag"));

        assertTrue(release.contains("./gradlew --no-daemon --stacktrace clean assemble"));
        assertTrue(release.contains("python scripts/artifact_audit.py"));
        assertTrue(release.contains("python scripts/visual_asset_audit.py"));
        assertTrue(release.contains("python scripts/release_residue_audit.py"));
        assertFalse(release.contains("clean ciGate"));
        assertFalse(release.contains("--stacktrace test"));
        assertFalse(release.contains("jacocoTestReport"));
        assertFalse(release.contains("pitest"));
        assertFalse(release.contains("python scripts/quality_summary.py"));
        assertFalse(release.contains("gh run download"));
        assertFalse(release.contains("actions/download-artifact"));
        assertFalse(release.contains("verification-evidence"));
    }

    @Test
    void releaseDependencyNotesComeFromTheRuntimeJarBuiltByTheReleaseGate() throws IOException {
        String release = Files.readString(ROOT.resolve(".github/workflows/release.yml"));

        assertTrue(release.contains("runtime=\"build/libs/$RUNTIME_JAR\""));
        assertTrue(release.contains("unzip -p \"$runtime\" fabric.mod.json"));
        assertTrue(release.contains(".depends.fabricloader"));
        assertTrue(release.contains(".depends[\"fabric-api\"]"));
        assertTrue(release.contains(".depends.java"));
        assertTrue(release.contains("serverInstallationRequired"));
        assertTrue(release.contains("server_required\" != 'false'"));
        assertTrue(release.contains("\"build/libs/$RUNTIME_JAR\""));
        assertFalse(release.contains("Fabric Loader 0.19.3"));
        assertFalse(release.contains("- Java: \\`25\\`"));
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
