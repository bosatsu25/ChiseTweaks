package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 壁越しAnalyzerがGPU保持処理と箱枠geometryを重複実装しないことを固定する。 */
final class ThroughWallRendererSharingContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void analyzersShareRetainedGpuLifecycleAndWireBoxGeometry() throws IOException {
        String lava = source("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java");
        String debris = source("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisThroughWallRenderer.java");
        String retained = source("src/main/java/dev/chise/chisetweaks/feature/rendering/RetainedThroughWallBuffer.java");
        String geometry = source("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallWireBoxGeometry.java");

        for (String analyzer : new String[]{lava, debris}) {
            assertTrue(analyzer.contains("RetainedThroughWallBuffer"));
            assertTrue(analyzer.contains("ThroughWallWireBoxGeometry.drawWireBox"));
            assertFalse(analyzer.contains("MappableRingBuffer"));
            assertFalse(analyzer.contains("MemoryUtil.memCopy"));
            assertFalse(analyzer.contains("createRenderPass("));
            assertFalse(analyzer.contains("private static void bar("));
        }

        assertTrue(retained.contains("MappableRingBuffer"));
        assertTrue(retained.contains("MemoryUtil.memCopy"));
        assertTrue(retained.contains("void resetAfterFailure()"));
        assertTrue(retained.contains("public void close()"));
        assertTrue(geometry.contains("static void drawWireBox("));
        assertTrue(geometry.contains("private static void bar("));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
