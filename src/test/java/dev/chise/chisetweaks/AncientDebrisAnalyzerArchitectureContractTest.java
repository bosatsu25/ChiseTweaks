package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the loaded-chunk-only, Ancient-Debris-only retained analyzer architecture. */
final class AncientDebrisAnalyzerArchitectureContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void discoveryIsAncientDebrisOnlyAndDrivenByClientChunkLifecycle() throws IOException {
        String feature = source("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");

        assertTrue(feature.contains("ClientChunkEvents.CHUNK_LOAD.register"));
        assertTrue(feature.contains("ClientChunkEvents.CHUNK_UNLOAD.register"));
        assertTrue(feature.contains("Blocks.ANCIENT_DEBRIS"));
        assertFalse(feature.contains("Blocks.DIAMOND_ORE"));
        assertFalse(feature.contains("Blocks.NETHER_GOLD_ORE"));
        assertFalse(feature.contains("Blocks.NETHER_QUARTZ_ORE"));
    }

    @Test
    void discoveryNeverRequestsOrGeneratesAnUnloadedChunk() throws IOException {
        String feature = source("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");

        assertTrue(feature.contains("getChunkNow("));
        assertFalse(feature.contains(".getChunk("));
        assertFalse(feature.contains("Executor"));
        assertFalse(feature.contains("new Thread("));
        assertFalse(feature.contains("CompletableFuture"));
    }

    @Test
    void loadedChunkDiscoveryIsRestrictedToTheCurrentAnalyzerNeighborhood() throws IOException {
        String feature = source("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");

        assertTrue(feature.contains("AncientDebrisAnalyzerPolicy.isChunkRelevant("));
        assertTrue(feature.contains("pruneTrackedChunksOutsideNeighborhood("));
        assertTrue(feature.contains("AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks("));
        assertTrue(feature.contains("client.level != level"));
        assertTrue(feature.contains("level != lastLevel"));
    }

    @Test
    void productRangeAndMarkerBudgetsRemainHardBounded() throws IOException {
        String policy = source("src/main/java/dev/chise/chisetweaks/core/policy/AncientDebrisAnalyzerPolicy.java");

        assertTrue(policy.contains("DEFAULT_RANGE_BLOCKS = 64"));
        assertTrue(policy.contains("MIN_RANGE_BLOCKS = 16"));
        assertTrue(policy.contains("MAX_RANGE_BLOCKS = 256"));
        assertTrue(policy.contains("DEFAULT_MAX_MARKERS = 64"));
        assertTrue(policy.contains("MAX_MAX_MARKERS = 128"));
        assertTrue(policy.contains("MAX_BOOTSTRAP_CHUNK_RADIUS = 17"));
        assertTrue(policy.contains("MAX_TRACKED_CHUNKS = 4096"));
        assertTrue(policy.contains("MAX_DEBRIS_PER_CHUNK = 256"));
    }

    @Test
    void analyzerPolicyParticipatesInBothCoverageAndMutationQualityGates() throws IOException {
        String build = source("build.gradle");

        assertTrue(build.contains("core/policy/AncientDebrisAnalyzerPolicy*.class"));
        assertTrue(build.contains("core.policy.AncientDebrisAnalyzerPolicy'"));
    }

    @Test
    void rendererUsesRetainedRevisionDrivenGeometry() throws IOException {
        String renderer = source("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisThroughWallRenderer.java");

        assertTrue(renderer.contains("sources.renderRevision() != uploadedRevision"));
        assertTrue(renderer.contains("MappableRingBuffer"));
        assertTrue(renderer.contains("rebuildAndUpload(capture)"));
        assertTrue(renderer.contains("anchorX - camera.x"));
        assertFalse(renderer.contains("getBlockState("));
        assertFalse(renderer.contains("getFluidState("));
    }

    @Test
    void featureIsNetherOnlyClientOnlyAndSessionIsolated() throws IOException {
        String feature = source("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String metadata = source("src/main/resources/fabric.mod.json");

        assertTrue(feature.contains("Level.NETHER.equals(level.dimension())"));
        assertTrue(feature.contains("positionsByChunk.clear()"));
        assertTrue(feature.contains("visibleMarkers.clear()"));
        assertTrue(feature.contains("resetSession(Minecraft client)"));
        assertTrue(metadata.contains("\"environment\": \"client\""));
        assertTrue(metadata.contains("\"serverInstallationRequired\": false"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
