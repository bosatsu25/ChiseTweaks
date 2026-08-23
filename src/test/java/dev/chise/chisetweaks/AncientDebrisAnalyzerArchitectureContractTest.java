package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.exists;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Guards the loaded-chunk-only, Ancient-Debris-only retained analyzer architecture. */
final class AncientDebrisAnalyzerArchitectureContractTest {
    @Test
    void discoveryIsAncientDebrisOnlyAndDrivenByClientChunkLifecycle() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");

        assertContainsAll(feature,
                "ClientChunkEvents.CHUNK_LOAD.register",
                "ClientChunkEvents.CHUNK_UNLOAD.register",
                "Blocks.ANCIENT_DEBRIS");
        assertContainsNone(feature,
                "Blocks.DIAMOND_ORE",
                "Blocks.NETHER_GOLD_ORE",
                "Blocks.NETHER_QUARTZ_ORE");
    }

    @Test
    void discoveryNeverRequestsOrGeneratesAnUnloadedChunk() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");

        assertContainsAll(feature, "getChunkNow(");
        assertContainsNone(feature,
                ".getChunk(",
                "Executor",
                "new Thread(",
                "CompletableFuture");
    }

    @Test
    void loadedChunkDiscoveryIsRestrictedToTheCurrentAnalyzerNeighborhood() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");

        assertContainsAll(feature,
                "AncientDebrisAnalyzerPolicy.isChunkRelevant(",
                "pruneTrackedChunksOutsideNeighborhood(",
                "AncientDebrisAnalyzerPolicy.chunkRadiusForRangeBlocks(",
                "client.level != level",
                "level != lastLevel");
    }

    @Test
    void productRangeAndMarkerBudgetsRemainHardBounded() throws IOException {
        String policy = read("src/main/java/dev/chise/chisetweaks/core/policy/AncientDebrisAnalyzerPolicy.java");

        assertContainsAll(policy,
                "DEFAULT_RANGE_BLOCKS = 64",
                "MIN_RANGE_BLOCKS = 16",
                "MAX_RANGE_BLOCKS = 256",
                "DEFAULT_MAX_MARKERS = 64",
                "MAX_MAX_MARKERS = 128",
                "MAX_BOOTSTRAP_CHUNK_RADIUS = 17",
                "MAX_BOOTSTRAP_CHUNKS_PER_TICK = 64",
                "MAX_BOOTSTRAP_CHUNK_COUNT",
                "MAX_TRACKED_CHUNKS = 4096",
                "MAX_DEBRIS_PER_CHUNK = 256");
    }

    @Test
    void analyzerPolicyParticipatesInBothCoverageAndMutationQualityGates() throws IOException {
        String build = read("build.gradle");

        assertContainsAll(build,
                "core/policy/AncientDebrisAnalyzerPolicy*.class",
                "core.policy.AncientDebrisAnalyzerPolicy'");
    }

    @Test
    void analyzersShareRetentionSnapshotAndRevisionDrivenRenderer() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");
        String nearest = read("src/main/java/dev/chise/chisetweaks/feature/rendering/NearestPositionBuffer.java");
        String retained = read("src/main/java/dev/chise/chisetweaks/feature/rendering/RetainedThroughWallBuffer.java");

        assertContainsAll(feature,
                "new ThroughWallPositionSnapshot(",
                "new NearestPositionBuffer(",
                "ThroughWallMarkerRenderer.Style.ANCIENT_DEBRIS");
        assertContainsAll(renderer,
                "sources.renderRevision() != uploadedRevision",
                "RetainedThroughWallBuffer",
                "rebuildAndUpload(capture)");
        assertContainsAll(nearest, "double[] distanceSquared");
        assertContainsAll(retained,
                "MappableRingBuffer",
                "anchorX - camera.x",
                "vertexBuffer.rotate()");
        assertContainsNone(renderer,
                "getBlockState(",
                "getFluidState(");
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisThroughWallRenderer.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisSnapshot.java"));
    }

    @Test
    void featureIsNetherOnlyClientOnlyAndSessionIsolated() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String metadata = read("src/main/resources/fabric.mod.json");

        assertContainsAll(feature,
                "Level.NETHER.equals(level.dimension())",
                "positionsByChunk.clear()",
                "visibleMarkers.clear()",
                "clearPendingBootstrap()",
                "resetSession(Minecraft client)");
        assertContainsAll(metadata,
                "\"environment\": \"client\"",
                "\"serverInstallationRequired\": false");
    }
}
