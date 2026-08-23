package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.exists;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** 壁越しAnalyzerがGPU保持処理と輪郭＋面geometryを重複実装しないことを固定する。 */
final class ThroughWallRendererSharingContractTest {
    @Test
    void analyzersShareOneRetainedRendererAndFilledWireBoxGeometry() throws IOException {
        String lava = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String debris = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");
        String retained = read("src/main/java/dev/chise/chisetweaks/feature/rendering/RetainedThroughWallBuffer.java");
        String geometry = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallWireBoxGeometry.java");

        assertContainsAll(lava,
                "ThroughWallMarkerRenderer",
                "ThroughWallMarkerRenderer.Style.LAVA_SOURCE");
        assertContainsAll(debris,
                "ThroughWallMarkerRenderer",
                "ThroughWallMarkerRenderer.Style.ANCIENT_DEBRIS");
        assertContainsAll(renderer,
                "RetainedThroughWallBuffer",
                "ThroughWallWireBoxGeometry.drawFilledBox",
                "ThroughWallWireBoxGeometry.drawWireBox",
                "fillColorForDistance(distance)",
                "enum Style",
                "LAVA_SOURCE",
                "ANCIENT_DEBRIS");
        assertContainsNone(renderer,
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
    }
}
