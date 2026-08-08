package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualAssistanceRenderingContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void visualAssistanceUsesFeatureSpecificHighContrastGeometry() throws IOException {
        String geometry = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/WorldLineGeometry.java"));
        String renderer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java"));

        assertTrue(geometry.contains("drawThreadSignal"));
        assertTrue(geometry.contains("drawGlassGrid"));
        assertTrue(geometry.contains("drawSurfaceHatch"));
        assertTrue(geometry.contains("drawMaterialPulse"));
        assertTrue(geometry.contains("drawNetherGrid"));

        assertTrue(renderer.contains("case TECHNICAL_TRACE"));
        assertTrue(renderer.contains("case GLASS_INSPECTION"));
        assertTrue(renderer.contains("case HIDDEN_SURFACE"));
        assertTrue(renderer.contains("case MATERIAL_HIGHLIGHT"));
        assertTrue(renderer.contains("case NETHER_PALETTE"));
        assertTrue(renderer.contains("powered=true"));
        assertTrue(renderer.contains("PULSE_COLORS"));
        assertTrue(renderer.contains("glassColor"));
    }

    @Test
    void visibilitySamplingTargetsThinAndShapedBlockGeometryWithoutWallThroughFallback() throws IOException {
        String scanner = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java"));

        assertTrue(scanner.contains("THIN_TECHNICAL_SAMPLES"));
        assertTrue(scanner.contains("GLASS_SAMPLES"));
        assertTrue(scanner.contains("SHAPED_BLOCK_SAMPLES"));
        assertTrue(scanner.contains("{0.50, 0.08, 0.50}"));
        assertTrue(scanner.contains("result.getBlockPos().equals(position)"));
        assertFalse(scanner.contains("return true; // wall-through"));
    }
}
