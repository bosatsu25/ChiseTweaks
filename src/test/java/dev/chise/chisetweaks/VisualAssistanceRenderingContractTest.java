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
        String facade = read("src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLineVisualGeometry.java");
        String placement = read("src/main/java/dev/chise/chisetweaks/feature/rendering/PlacementGuideLineGeometry.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java");
        String engine = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java");

        assertTrue(facade.contains("drawThreadSkin"));
        assertTrue(facade.contains("drawGlassSkin"));
        assertTrue(facade.contains("drawHiddenSurfaceSkin"));
        assertTrue(facade.contains("drawNetherSkin"));
        assertTrue(facade.contains("drawPlacementSkin"));
        assertTrue(placement.contains("drawSlab"));
        assertTrue(placement.contains("drawStairs"));
        assertTrue(placement.contains("drawTrapdoor"));
        assertTrue(placement.contains("drawFenceGate"));
        assertTrue(placement.contains("drawAxisSkin"));

        assertTrue(renderer.contains("case TECHNICAL_TRACE"));
        assertTrue(renderer.contains("case GLASS_INSPECTION"));
        assertTrue(renderer.contains("case HIDDEN_SURFACE"));
        assertTrue(renderer.contains("case MATERIAL_HIGHLIGHT -> { }"));
        assertTrue(renderer.contains("case NETHER_PALETTE"));
        assertTrue(renderer.contains("SurfaceLineVisualGeometry.drawPlacementSkin"));
        assertFalse(renderer.contains("SurfaceLineVisualGeometry.drawMaterialSkin"));
        assertFalse(renderer.contains("PULSE_COLORS"));
        assertFalse(renderer.contains("glassColor("));
        assertFalse(renderer.contains("WorldLineGeometry.drawBox"));

        assertTrue(engine.contains(
                "definition.inspectionCategory() != BlockInspectionCategory.MATERIAL_HIGHLIGHT"));
    }

    @Test
    void rendererPreparesFrameStateOnceAndBoundsEveryDistantWorldLineMode() throws IOException {
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java");
        String target = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteRenderTarget.java");
        String detail = read("src/main/java/dev/chise/chisetweaks/core/performance/WorksiteOverlayDetailPolicy.java");

        assertTrue(renderer.contains("WorksiteRenderTarget.prepare(target)"));
        assertTrue(renderer.contains("WorksiteOverlayDetailPolicy.Detail.COMPACT"));
        assertTrue(renderer.contains("drawCompactFrame"));
        assertTrue(renderer.contains("drawCompactPlacementSkin"));
        assertTrue(renderer.contains("vertices, pose, target.position(), stateColor, 1.8f"));
        assertTrue(target.contains("details.contains(\"powered=true\")"));
        assertTrue(target.contains("blockId.endsWith(\"tripwire_hook\")"));
        assertTrue(target.contains("Math.floorMod(source.position().hashCode(), 8)"));
        assertTrue(detail.contains("FULL_DETAIL_DISTANCE_SQUARED = 49.0"));

        assertFalse(renderer.contains("presentation().details().contains"));
        assertFalse(renderer.contains("position().hashCode()"));
        assertFalse(renderer.contains("endsWith(\"tripwire_hook\")"));
    }

    @Test
    void rendererBuildCanBeIdentifiedFromTheRuntimeLog() throws IOException {
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java");
        String properties = read("gradle.properties");

        assertTrue(properties.contains("mod_version=0.7.0+mc26.1.2"));
        assertTrue(renderer.contains("RENDERER_REVISION = \"surface-line-v4-budgeted\""));
        assertTrue(renderer.contains("Visual renderer {} active in ChiseTweaks {}"));
        assertTrue(renderer.contains("rendererIdentityLogged"));
    }

    @Test
    void nonMaterialWorldLinePaletteAvoidsConfiguredSchematicOverlayColors() throws IOException {
        String style = read("src/main/java/dev/chise/chisetweaks/core/vision/VisualAssistanceStylePolicy.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java");
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
        String scanner = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java");

        assertTrue(scanner.contains("THIN_TECHNICAL_SAMPLES"));
        assertTrue(scanner.contains("GLASS_SAMPLES"));
        assertTrue(scanner.contains("SHAPED_BLOCK_SAMPLES"));
        assertTrue(scanner.contains("{0.50, 0.08, 0.50}"));
        assertTrue(scanner.contains("result.getBlockPos().equals(position)"));
        assertTrue(scanner.contains("remainingLineOfSightRays--"));
        assertFalse(scanner.contains("return true; // wall-through"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
