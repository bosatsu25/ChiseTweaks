package dev.chise.chisetweaks;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("performance")
final class RetainedVisualPerformanceArchitectureContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void lavaScannerPublishesPrimitiveSnapshotsAndReadsOnlyAlreadyLoadedChunks() throws IOException {
        String source = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");

        assertTrue(source.contains("LavaSourceSnapshot"));
        assertTrue(source.contains("long[] packedCandidatePositions"));
        assertTrue(source.contains("Arrays.sort(packedCandidatePositions, 0, count)"));
        assertTrue(source.contains("getChunkNow("));
        assertTrue(source.contains("LevelChunk sourceChunk"));
        assertFalse(source.contains("new ArrayList"));
        assertFalse(source.contains("List<BlockPos>"));
    }

    @Test
    void lavaRendererRetainsGeometryAcrossUnchangedFrames() throws IOException {
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java");
        String retained = read("src/main/java/dev/chise/chisetweaks/feature/rendering/RetainedThroughWallBuffer.java");

        assertTrue(renderer.contains("sources.renderRevision() != uploadedRevision"));
        assertTrue(renderer.contains("RetainedThroughWallBuffer"));
        assertTrue(renderer.contains("rebuildAndUpload(capture)"));
        assertTrue(retained.contains("drawVertexBuffer"));
        assertTrue(retained.contains("vertexBuffer.rotate()"));
        assertTrue(retained.contains("anchorX - camera.x"));
        assertFalse(renderer.contains("PoseStack"));
        assertFalse(renderer.contains("matrices.translate(-camera"));
    }

    @Test
    void modelBackedHighlightsUseOneImmutableRuntimeSnapshot() throws IOException {
        String ore = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");
        String kelp = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightKelpHighlightModel.java");
        String glass = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightGlassHighlightModel.java");
        String state = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualRenderState.java");

        assertTrue(ore.contains("VisualRenderState.current()"));
        assertTrue(kelp.contains("VisualRenderState.current().kelpEnabled()"));
        assertTrue(glass.contains("VisualRenderState.current().glassEnabled()"));
        assertTrue(state.contains("private static volatile Snapshot current"));
        assertFalse(ore.contains("LocalFeatureConfig.getInstance()"));
        assertFalse(ore.contains("FeatureSwitches.MATERIAL_HIGHLIGHTS"));
        assertFalse(kelp.contains("FeatureSwitches.KELP_HIGHLIGHT"));
        assertFalse(glass.contains("FeatureSwitches.GLASS_INSPECTION"));
    }

    @Test
    void targetClassificationCacheIsScopedToOneModelReload() throws IOException {
        String plugin = read("src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");

        assertTrue(plugin.contains("ConcurrentHashMap<Block, VisualModelClassification> classificationCache"));
        assertTrue(plugin.contains("classificationCache.computeIfAbsent("));
        assertTrue(plugin.contains("VisualModelClassification classify(BlockState state)"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
