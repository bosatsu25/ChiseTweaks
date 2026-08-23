package dev.chise.chisetweaks;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.read;

@Tag("performance")
final class RetainedVisualPerformanceArchitectureContractTest {
    @Test
    void lavaScannerPublishesPrimitiveSnapshotsAndReadsOnlyAlreadyLoadedChunks() throws IOException {
        String source = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String nearest = read("src/main/java/dev/chise/chisetweaks/feature/rendering/NearestPositionBuffer.java");
        String snapshot = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallPositionSnapshot.java");

        assertContainsAll(source,
                "new ThroughWallPositionSnapshot(",
                "new NearestPositionBuffer(",
                "LevelChunk[] loadedChunkBuffer",
                "nearestSources.offer(",
                "nearestSources.sortPositions()",
                "highlightedSources.publish(",
                "getChunkNow(",
                "LevelChunk sourceChunk");
        assertContainsAll(nearest,
                "long[] positions",
                "double[] distanceSquared");
        assertContainsAll(snapshot,
                "long[][] positions",
                "System.arraycopy(");
        assertContainsNone(source,
                "new ArrayList",
                "List<BlockPos>",
                ".getChunk(");
    }

    @Test
    void sharedAnalyzerRendererRetainsGeometryAcrossUnchangedFrames() throws IOException {
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");
        String retained = read("src/main/java/dev/chise/chisetweaks/feature/rendering/RetainedThroughWallBuffer.java");

        assertContainsAll(renderer,
                "sources.renderRevision() != uploadedRevision",
                "RetainedThroughWallBuffer",
                "rebuildAndUpload(capture)",
                "ThroughWallWireBoxGeometry.drawWireBox");
        assertContainsAll(retained,
                "drawVertexBuffer",
                "vertexBuffer.rotate()",
                "anchorX - camera.x");
        assertContainsNone(renderer,
                "PoseStack",
                "matrices.translate(-camera",
                "MappableRingBuffer",
                "MemoryUtil.memCopy");
    }

    @Test
    void ancientDebrisBootstrapHasAFrameTimeBudget() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String policy = read("src/main/java/dev/chise/chisetweaks/core/policy/AncientDebrisAnalyzerPolicy.java");

        assertContainsAll(feature,
                "pendingBootstrapChunks",
                "scheduleLoadedChunkBootstrap(",
                "processPendingLoadedChunks(",
                "MAX_BOOTSTRAP_CHUNKS_PER_TICK",
                "getChunkNow(");
        assertContainsAll(policy,
                "MAX_BOOTSTRAP_CHUNKS_PER_TICK",
                "MAX_BOOTSTRAP_CHUNK_COUNT");
        assertContainsNone(feature,
                "Executor",
                "new Thread(",
                "CompletableFuture");
    }

    @Test
    void modelBackedHighlightsUseOneImmutableRuntimeSnapshot() throws IOException {
        String ore = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");
        String kelp = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightKelpHighlightModel.java");
        String glass = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightGlassHighlightModel.java");
        String state = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualRenderState.java");

        assertContainsAll(ore, "VisualRenderState.current()");
        assertContainsAll(kelp, "VisualRenderState.current().kelpEnabled()");
        assertContainsAll(glass, "VisualRenderState.current().glassEnabled()");
        assertContainsAll(state, "private static volatile Snapshot current");
        assertContainsNone(ore,
                "LocalFeatureConfig.getInstance()",
                "FeatureSwitches.MATERIAL_HIGHLIGHTS");
        assertContainsNone(kelp, "FeatureSwitches.KELP_HIGHLIGHT");
        assertContainsNone(glass, "FeatureSwitches.GLASS_INSPECTION");
    }

    @Test
    void targetClassificationCacheIsScopedToOneModelReload() throws IOException {
        String plugin = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");

        assertContainsAll(plugin,
                "ConcurrentHashMap<Block, VisualModelClassification> classificationCache",
                "classificationCache.computeIfAbsent(",
                "VisualModelClassification classify(BlockState state)");
    }
}
