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
    void performanceSpecificationAndAcceptanceAreExecutableTestCode() throws IOException {
        assertFalse(Files.exists(ROOT.resolve("docs/PERFORMANCE_SQA.md")));
        assertFalse(Files.exists(ROOT.resolve("docs/performance-capture.example.json")));
        assertFalse(Files.exists(ROOT.resolve("scripts/performance_compare.py")));
        assertTrue(Files.exists(ROOT.resolve(
                "src/test/java/dev/chise/chisetweaks/performance/PerformanceComparison.java")));
        assertTrue(Files.exists(ROOT.resolve(
                "src/test/java/dev/chise/chisetweaks/performance/PerformanceAcceptancePolicy.java")));
        assertTrue(Files.exists(ROOT.resolve(
                "src/test/java/dev/chise/chisetweaks/performance/JfrPerformanceAnalyzer.java")));
        Path cli = ROOT.resolve(
                "src/test/java/dev/chise/chisetweaks/performance/PerformanceEvidenceCli.java");
        assertTrue(Files.exists(cli));
        String cliSource = Files.readString(cli);
        assertTrue(cliSource.contains("PerformanceAcceptancePolicy.evaluate"));
        assertTrue(cliSource.contains("performance_acceptance=PASS"));
        assertTrue(cliSource.contains("performance_acceptance=FAIL"));
    }

    @Test
    void ciOwnsQualityGatesAndReleaseOnlyPromotesTheVerifiedArtifact() throws IOException {
        String build = Files.readString(ROOT.resolve("build.gradle"));
        String ci = Files.readString(ROOT.resolve(".github/workflows/ci.yml"));
        String release = Files.readString(ROOT.resolve(".github/workflows/release.yml"));

        assertTrue(build.contains("tasks.register('ciGate')"));
        assertTrue(build.contains("tasks.register('comparePerformanceEvidence', JavaExec)"));
        assertFalse(build.contains("tasks.register('performanceGate', Test)"));

        assertTrue(ci.contains("./gradlew --stacktrace ciGate"));
        assertTrue(ci.contains("actions/upload-artifact@"));
        assertTrue(ci.contains("path: build/libs/${{ steps.artifacts.outputs.runtime_jar }}"));
        assertTrue(ci.contains("archive: false"));
        assertFalse(ci.contains("continue-on-error"));
        assertFalse(ci.contains("gh release"));
        assertFalse(ci.contains("git tag"));

        assertTrue(release.contains("actions/download-artifact@"));
        assertTrue(release.contains("run-id: ${{ github.event.workflow_run.id }}"));
        assertTrue(release.contains("Exact CI artifact promoted: `PASS`"));
        assertFalse(release.contains("./gradlew"));
        assertFalse(release.contains("python scripts/artifact_audit.py"));
        assertFalse(release.contains("python scripts/visual_asset_audit.py"));
        assertFalse(release.contains("python scripts/release_residue_audit.py"));
        assertFalse(release.contains("gh release download"));
    }

    @Test
    void releaseDependencyNotesComeFromThePromotedRuntimeJar() throws IOException {
        String release = Files.readString(ROOT.resolve(".github/workflows/release.yml"));

        assertTrue(release.contains("runtime=\"${candidates[0]}\""));
        assertTrue(release.contains("unzip -p \"$runtime\" fabric.mod.json"));
        assertTrue(release.contains(".depends.fabricloader"));
        assertTrue(release.contains(".depends[\"fabric-api\"]"));
        assertTrue(release.contains(".depends.java"));
        assertTrue(release.contains("serverInstallationRequired"));
        assertTrue(release.contains("server_required\" != 'false'"));
        assertTrue(release.contains("\"$RUNTIME_PATH\""));
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

    @Test
    void patternConsistencyBudgetsArePartOfTheRepositoryPerformanceAudit() throws IOException {
        String audit = Files.readString(ROOT.resolve("scripts/runtime_performance_contract_audit.py"));

        assertTrue(audit.contains("PATTERN_INSPECTOR"));
        assertTrue(audit.contains("MAX_BLOCKS_PER_TICK = 256"));
        assertTrue(audit.contains("MAX_RETAINED_MISMATCHES = 64"));
        assertTrue(audit.contains("pattern_consistency_scan=bounded_loaded_chunks_only"));
    }
}
