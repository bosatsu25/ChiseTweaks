package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression contract for the source-only, full-bright lava visibility guide. */
final class LavaSourceGuideContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void semanticStyleUsesReservedDeepGreenAndSourceOnlySelection() throws IOException {
        assertEquals(0xFF075B32, LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB);
        assertTrue(LavaVisionPalettePolicy.SOURCE_LINE_WIDTH >= 3.0f);

        assertFalse(LavaVisionPalettePolicy.shouldHighlight(false, true, true));
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(true, false, true));
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(true, true, false));
        assertTrue(LavaVisionPalettePolicy.shouldHighlight(true, true, true));

        // Keep the semantic source marker distinct from every generated ore/material RGB triplet.
        String visualAssets = read("gradle/chise-visual-assets.gradle");
        assertFalse(visualAssets.contains("[7, 91, 50]"));
    }

    @Test
    void runtimeUsesBoundedCachedWorldLinesInsteadOfFluidTinting() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");

        assertTrue(feature.contains("implements TickingFeature, SessionAwareFeature"));
        assertTrue(feature.contains("FluidState"));
        assertTrue(feature.contains("isSource()"));
        assertTrue(feature.contains("WorldLineGeometry.drawBox"));
        assertTrue(feature.contains("LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB"));
        assertTrue(feature.contains("LavaVisionPalettePolicy.shouldHighlight"));
        assertTrue(feature.contains("RenderTypes.lines()"));
        assertTrue(feature.contains("MAX_OVERLAY_RESULTS"));
        assertTrue(feature.contains("getChunkSource().hasChunk("));
        assertTrue(feature.contains("visibleSources = List.copyOf(prepared)"));
        assertFalse(feature.contains("FluidRenderingRegistry"));
        assertFalse(feature.contains("LavaFluidRenderHandler"));
        assertFalse(feature.contains("System.nanoTime()"));
    }

    @Test
    void legacySodiumTintMixinIsNotRegisteredForRuntime() throws IOException {
        String mixins = read("src/main/resources/chisetweaks.sodium.mixins.json");
        assertTrue(mixins.contains("\"client\": []"));
        assertFalse(mixins.contains("\"LavaHighlightRendererMixin\""));
    }

    @Test
    void userFacingMetadataUsesLavaAnalyzerWithoutRequiringSodium() throws IOException {
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
