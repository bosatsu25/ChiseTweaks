package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.exists;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Retained Lava Analyzer must use one retained GPU renderer/geometry path. */
final class ThroughWallRendererSharingContractTest {
    @Test
    void lavaAnalyzerUsesOneRetainedRendererAndFilledWireBoxGeometry() throws IOException {
        String lava = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");
        String retained = read("src/main/java/dev/chise/chisetweaks/feature/rendering/RetainedThroughWallBuffer.java");
        String geometry = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallWireBoxGeometry.java");

        assertContainsAll(lava,
                "ThroughWallMarkerRenderer",
                "ThroughWallMarkerRenderer.Style.LAVA_SOURCE");
        assertContainsAll(renderer,
                "RetainedThroughWallBuffer",
                "ThroughWallWireBoxGeometry.drawFilledBox",
                "ThroughWallWireBoxGeometry.drawWireBox",
                "fillColorForDistance(distance)",
                "enum Style",
                "LAVA_SOURCE");
        assertContainsNone(renderer,
                "ANCIENT_DEBRIS",
                "MappableRingBuffer",
                "MemoryUtil.memCopy",
                "createRenderPass(",
                "private static void bar(");

        assertContainsAll(retained,
                "MappableRingBuffer",
                "MemoryUtil.memCopy",
                "void resetAfterFailure()",
                "public void close()");
        assertContainsAll(geometry,
                "static void drawFilledBox(",
                "static void drawWireBox(",
                "private static void bar(");

        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisThroughWallRenderer.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaSourceSnapshot.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisSnapshot.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java"));
    }
}
