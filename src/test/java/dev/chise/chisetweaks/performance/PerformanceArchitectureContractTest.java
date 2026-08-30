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

        assertTrue(build.contains("tasks.register('ciGate')"));
        assertTrue(build.contains("tasks.register('comparePerformanceEvidence', JavaExec)"));
        assertFalse(build.contains("tasks.register('performanceGate', Test)"));

        assertTrue(ci.contains("./gradlew --stacktrace ciGate"));
        assertTrue(ci.contains("actions/upload-artifact@"));
        assertTrue(ci.contains("path: build/libs/${{ steps.runtime.outputs.runtime_jar }}"));
        assertTrue(ci.contains("archive: false"));
        assertFalse(ci.contains("continue-on-error"));

        assertTrue(ci.contains("release:"));
        assertTrue(ci.contains("name: release / Publish verified runtime JAR"));
        assertTrue(ci.contains("actions/download-artifact@"));
        assertTrue(ci.contains("name: ${{ needs.verify.outputs.runtime_jar }}"));
        assertTrue(ci.contains("gh release create"));
        assertFalse(ci.contains("gh release download"));
    }

    @Test
    void releaseDependencyNotesComeFromThePromotedRuntimeJar() throws IOException {
        String ci = Files.readString(ROOT.resolve(".github/workflows/ci.yml"));

        assertTrue(ci.contains("runtime=\"${candidates[0]}\""));
        assertTrue(ci.contains("unzip -p \"$runtime\" fabric.mod.json"));
        assertTrue(ci.contains(".depends.fabricloader"));
        assertTrue(ci.contains(".depends[\"fabric-api\"]"));
        assertTrue(ci.contains(".depends.java"));
        assertTrue(ci.contains("serverInstallationRequired"));
        assertTrue(ci.contains("server_required\" != 'false'"));
        assertTrue(ci.contains("\"$RUNTIME_PATH\""));
        assertFalse(ci.contains("Fabric Loader 0.19.3"));
        assertFalse(ci.contains("- Java: \\`25\\`"));
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
    void hotWorldScanLoopsReuseMutableBlockPositions() throws IOException {
        String infrastructure = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/InfrastructureRangeFeature.java"));
        String villager = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/VillagerJobSiteLinksFeature.java"));
        String worksite = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java"));
        String manager = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));
        String loadedChunkWindow = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LoadedChunkWindow.java"));

        assertTrue(loadedChunkWindow.contains("getChunkNow("));
        assertFalse(loadedChunkWindow.contains(".getChunk("));
        assertTrue(loadedChunkWindow.contains("long required = (long) resolvedSpanX * resolvedSpanZ"));

        assertTrue(infrastructure.contains("BlockPos.MutableBlockPos scanCursor"));
        assertTrue(infrastructure.contains("LoadedChunkWindow loadedChunks"));
        assertTrue(infrastructure.contains("loadedChunks.load("));
        assertTrue(infrastructure.contains("loadedChunks.atBlock("));
        assertTrue(infrastructure.contains("LevelChunk sourceChunk"));
        assertTrue(infrastructure.contains("sourceChunk.getBlockState(scanCursor)"));
        assertFalse(infrastructure.contains("BlockPos pos = new BlockPos(x, y, z)"));
        assertFalse(infrastructure.contains("client.level.getBlockState(scanCursor)"));
        assertFalse(infrastructure.substring(infrastructure.indexOf("private int beaconLevel("))
                .contains("getChunkNow("));

        assertTrue(infrastructure.contains("implements TickingRuntimeComponent, SessionAwareRuntimeComponent"));
        assertTrue(infrastructure.contains("state.is(Blocks.BEACON)"));
        assertTrue(infrastructure.contains("state.is(Blocks.LIGHTNING_ROD)"));
        assertFalse(infrastructure.contains("enum Mode"));
        assertTrue(manager.contains("registerComponent(new InfrastructureRangeFeature())"));
        assertFalse(manager.contains("InfrastructureRangeFeature.Mode"));

        String analyzers = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/OccludedHighlightsFeature.java"));
        assertTrue(analyzers.contains("implements TickingRuntimeComponent, SessionAwareRuntimeComponent"));
        assertTrue(analyzers.contains("FeatureSwitches.LAVA_HIGHLIGHT.getBooleanValue()"));
        assertTrue(analyzers.contains("FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue()"));
        assertTrue(analyzers.contains("local.occludedHighlightHorizontalRadius"));
        assertTrue(analyzers.contains("local.occludedHighlightIntervalTicks"));
        assertTrue(analyzers.contains("LoadedChunkWindow loadedChunks"));
        assertTrue(analyzers.contains("loadedChunks.load("));
        assertTrue(analyzers.contains("loadedChunks.atBlock("));
        assertTrue(analyzers.contains("getChunkNow("));
        assertTrue(analyzers.contains(
                "BlockState state = scanHidden ? sourceChunk.getBlockState(cursor) : null"));
        assertTrue(analyzers.contains("state.getFluidState()"));
        assertFalse(analyzers.contains("client.level.getBlockState(cursor)"));
        assertTrue(manager.contains("registerComponent(new OccludedHighlightsFeature())"));
        assertFalse(manager.contains("new LavaHighlightFeature()"));
        assertFalse(manager.contains("new HiddenBlockAnalyzerFeature()"));

        assertTrue(villager.contains("MemoryModuleType.JOB_SITE"));
        assertTrue(villager.contains("memory.isEmpty()"));
        assertFalse(villager.contains("LoadedChunkWindow"));
        assertFalse(villager.contains("findNearestLoadedWorkstation"));
        assertFalse(villager.contains("getBlockState("));

        assertTrue(worksite.contains("LoadedChunkWindow loadedChunks"));
        assertTrue(worksite.contains("loadedChunks.load("));
        assertTrue(worksite.contains("loadedChunks.atBlock("));
        assertTrue(worksite.contains("sourceChunk.getBlockState(position)"));
        assertFalse(worksite.contains("client.level.getBlockState(position)"));
    }

    @Test
    void patternConsistencyBudgetsArePartOfTheRepositoryPerformanceAudit() throws IOException {
        String audit = Files.readString(ROOT.resolve("scripts/runtime_performance_contract_audit.py"));

        assertTrue(audit.contains("PATTERN_INSPECTOR"));
        assertTrue(audit.contains("MAX_BLOCKS_PER_TICK = 256"));
        assertTrue(audit.contains("pattern_consistency_position_retention=false"));
        assertTrue(audit.contains("pattern_consistency_scan=bounded_loaded_chunks_only"));
    }
}
