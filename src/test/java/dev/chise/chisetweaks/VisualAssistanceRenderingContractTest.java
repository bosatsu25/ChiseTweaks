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
    void visualAssistanceKeepsSurfaceLinesForNonMaterialModesOnly() throws IOException {
        String geometry = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLineVisualGeometry.java"));
        String renderer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java"));
        String engine = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java"));

        assertTrue(geometry.contains("drawThreadSkin"));
        assertTrue(geometry.contains("drawGlassSkin"));
        assertTrue(geometry.contains("drawHiddenSurfaceSkin"));
        assertTrue(geometry.contains("drawNetherSkin"));
        assertTrue(geometry.contains("drawPlacementSkin"));
        assertTrue(geometry.contains("drawSlab"));
        assertTrue(geometry.contains("drawStairs"));
        assertTrue(geometry.contains("drawTrapdoor"));
        assertTrue(geometry.contains("drawFenceGate"));
        assertTrue(geometry.contains("drawAxisSkin"));

        assertTrue(renderer.contains("case TECHNICAL_TRACE"));
        assertTrue(renderer.contains("case GLASS_INSPECTION"));
        assertTrue(renderer.contains("case HIDDEN_SURFACE"));
        assertTrue(renderer.contains("case MATERIAL_HIGHLIGHT -> { }"));
        assertTrue(renderer.contains("case NETHER_PALETTE"));
        assertTrue(renderer.contains("SurfaceLineVisualGeometry.drawPlacementSkin"));
        assertTrue(renderer.contains("powered=true"));
        assertFalse(renderer.contains("SurfaceLineVisualGeometry.drawMaterialSkin"));
        assertFalse(renderer.contains("PULSE_COLORS"));
        assertFalse(renderer.contains("glassColor("));
        assertFalse(renderer.contains("WorldLineGeometry.drawBox"));

        assertTrue(engine.contains(
                "definition.inspectionCategory() != BlockInspectionCategory.MATERIAL_HIGHLIGHT"));
    }

    @Test
    void rendererBuildCanBeIdentifiedFromTheRuntimeLog() throws IOException {
        String renderer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java"));
        String properties = Files.readString(ROOT.resolve("gradle.properties"));

        assertTrue(properties.contains("mod_version=0.6.4+mc26.1.2"));
        assertTrue(renderer.contains("RENDERER_REVISION = \"surface-line-v2\""));
        assertTrue(renderer.contains("Visual renderer {} active in ChiseTweaks {}"));
        assertTrue(renderer.contains("rendererIdentityLogged"));
    }

    @Test
    void nonMaterialWorldLinePaletteAvoidsConfiguredSchematicOverlayColors() throws IOException {
        String style = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/vision/VisualAssistanceStylePolicy.java"));
        String renderer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java"));
        String combined = style + "\n" + renderer;

        for (String reserved : new String[] {
                "0xFFFFFFFF",
                "0xFFFF30FF",
                "0xFFFFAA00",
                "0xFF33CC33",
                "0xFFF03030",
                "0xFFF8D650",
                "0xFFFF4CE6",
                "0xFF33B3E6",
                "0xFFFF3333",
                "0xFFFF9010"
        }) {
            assertFalse(combined.contains(reserved), reserved);
        }
        assertTrue(combined.contains("0xFFB29CFF"));
        assertTrue(combined.contains("0xFF4E3A8C"));
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
