package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression contract for the bounded, full-bright Lava Analyzer. */
final class LavaSourceGuideContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void semanticStyleUsesReservedDeepGreenAndSmoothProximityGradient() throws IOException {
        assertEquals(0xFF075B32, LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB);
        assertEquals(0xFF021A0E, LavaVisionPalettePolicy.FAR_OUTLINE_ARGB);
        assertEquals(2.0, LavaVisionPalettePolicy.NEAR_DISTANCE_BLOCKS);
        assertEquals(8.0, LavaVisionPalettePolicy.FAR_DISTANCE_BLOCKS);
        assertTrue(LavaVisionPalettePolicy.SOURCE_LINE_WIDTH >= 3.0f);
        assertTrue(LavaVisionPalettePolicy.ANALYZER_EDGE_THICKNESS > 0.0f);

        assertEquals(LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(0.0));
        assertEquals(LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(2.0));
        assertEquals(LavaVisionPalettePolicy.FAR_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(8.0));
        assertEquals(LavaVisionPalettePolicy.FAR_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(Double.POSITIVE_INFINITY));

        int mid = LavaVisionPalettePolicy.colorForDistance(5.0);
        assertNotEquals(LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB, mid);
        assertNotEquals(LavaVisionPalettePolicy.FAR_OUTLINE_ARGB, mid);

        String visualAssets = read("gradle/chise-visual-assets.gradle");
        assertFalse(visualAssets.contains("[7, 91, 50]"));
        assertFalse(visualAssets.contains("[2, 26, 14]"));
    }

    @Test
    void selectionRemainsSourceOnlyAndSkipsDenseInteriorLava() {
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(false, true, true));
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(true, false, true));
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(true, true, false));
        assertTrue(LavaVisionPalettePolicy.shouldHighlight(true, true, true));
    }

    @Test
    void runtimeUsesBoundedCachedSourcesAndDedicatedAnalyzerRenderer() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java");

        assertTrue(feature.contains("implements TickingFeature, SessionAwareFeature"));
        assertTrue(feature.contains("FluidState"));
        assertTrue(feature.contains("isSource()"));
        assertTrue(feature.contains("LavaVisionPalettePolicy.shouldHighlight"));
        assertTrue(feature.contains("MAX_OVERLAY_RESULTS"));
        assertTrue(feature.contains("getChunkSource().hasChunk("));
        assertTrue(feature.contains("analyzedSources = List.copyOf(prepared)"));
        assertTrue(feature.contains("analyzerRenderer.render(context, snapshot)"));
        assertTrue(feature.contains("ClientLifecycleEvents.CLIENT_STOPPING"));
        assertFalse(feature.contains("FluidRenderingRegistry"));
        assertFalse(feature.contains("LavaFluidRenderHandler"));
        assertFalse(feature.contains("System.nanoTime()"));

        assertTrue(renderer.contains("RenderPipelines.DEBUG_FILLED_SNIPPET"));
        assertTrue(renderer.contains("withDepthStencilState(Optional.empty())"));
        assertTrue(renderer.contains("pipeline/lava_analyzer_through_walls"));
        assertTrue(renderer.contains("LavaVisionPalettePolicy.colorForDistance"));
        assertTrue(renderer.contains("ANALYZER_EDGE_THICKNESS"));
        assertTrue(renderer.contains("drawWireBox"));
        assertTrue(renderer.contains("MappableRingBuffer"));
        assertTrue(renderer.contains("allocator.close()"));
    }

    @Test
    void helpExplainsBoundedWallThroughProximityAnalysis() throws IOException {
        String ja = read("src/main/resources/assets/chisetweaks/lang/ja_jp.json");
        String en = read("src/main/resources/assets/chisetweaks/lang/en_us.json");

        assertTrue(ja.contains("近くの溶岩源を壁越しに解析"));
        assertTrue(ja.contains("2ブロック以内で #075B32"));
        assertTrue(en.contains("Analyzes nearby lava sources through terrain"));
        assertTrue(en.contains("within 2 blocks"));
        assertFalse(en.contains("It does not display through terrain"));
    }

    @Test
    void legacySodiumTintMixinIsNotRegisteredForRuntime() throws IOException {
        String mixins = read("src/main/resources/chisetweaks.sodium.mixins.json");
        assertTrue(mixins.contains("\"client\": []"));
        assertFalse(mixins.contains("\"LavaHighlightRendererMixin\""));
    }

    @Test
    void userFacingMetadataUsesLavaAnalyzerWithoutSodiumDependency() throws IOException {
        String definitions = read("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java");
        String lavaDefinition = between(definitions, "LAVA_HIGHLIGHT(", ");\n\n    public static final");

        assertTrue(lavaDefinition.contains("\"Lava Analyzer\""));
        assertFalse(lavaDefinition.contains("\"Lava Source Guide\""));
        assertFalse(lavaDefinition.contains("\"Sodium\""));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, startToken);
        assertTrue(end > start, endToken);
        return source.substring(start, end);
    }
}
