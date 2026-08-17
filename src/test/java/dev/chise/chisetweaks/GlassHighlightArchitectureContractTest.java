package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GlassHighlightArchitectureContractTest {
    private static final Path GENERATED = Path.of(
            "build", "generated", "chiseGlassVisualAssets", "assets", "chisetweaks");

    @Test
    void generatedGlassMarkersAreSmallStaticSparseAssets() throws IOException {
        BufferedImage block = ImageIO.read(GENERATED.resolve(
                "textures/block/visual/glass/glass_block_marker.png").toFile());
        BufferedImage pane = ImageIO.read(GENERATED.resolve(
                "textures/block/visual/glass/glass_pane_marker.png").toFile());
        assertNotNull(block);
        assertNotNull(pane);
        assertEquals(16, block.getWidth());
        assertEquals(16, block.getHeight());
        assertEquals(16, pane.getWidth());
        assertEquals(16, pane.getHeight());

        assertTrue(nonTransparentPixels(block) > 0);
        assertTrue(nonTransparentPixels(block) < 80);
        assertTrue(nonTransparentPixels(pane) > 0);
        assertTrue(nonTransparentPixels(pane) < 80);
        assertTrue(containsRgb(block, 0x5EEBFF));
        assertTrue(containsRgb(pane, 0xFFD166));

        String generator = Files.readString(Path.of("gradle/chise-glass-visual-assets.gradle"));
        assertFalse(generator.contains(".mcmeta"));
        assertFalse(generator.contains("System.nanoTime"));
    }

    @Test
    void blockAndPaneModelsUseDifferentLowDensityGeometry() throws IOException {
        String blockModel = Files.readString(GENERATED.resolve(
                "models/block/visual/glass/block_overlay.json"));
        String paneModel = Files.readString(GENERATED.resolve(
                "models/block/visual/glass/pane_overlay.json"));

        assertTrue(blockModel.contains("\"ambientocclusion\": false"));
        assertTrue(paneModel.contains("\"ambientocclusion\": false"));
        assertTrue(blockModel.contains("\"shade\": false"));
        assertTrue(paneModel.contains("\"shade\": false"));
        assertTrue(blockModel.contains("-0.04"));
        assertTrue(blockModel.contains("16.04"));
        assertTrue(paneModel.contains("7.35"));
        assertTrue(paneModel.contains("8.65"));
    }

    @Test
    void glassUsesBakedModelCompositionAndNoLongerUsesWorksiteScanner() throws IOException {
        String plugin = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java"));
        String wrapper = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightGlassHighlightModel.java"));
        String scanner = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java"));
        String surfaceGeometry = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/SurfaceLineVisualGeometry.java"));

        assertTrue(plugin.contains("GlassHighlightTargetPolicy.classify"));
        assertTrue(plugin.contains("FullbrightGlassHighlightModel"));
        assertTrue(wrapper.contains("super.emitQuads"));
        assertTrue(wrapper.contains("FeatureSwitches.GLASS_INSPECTION"));
        assertTrue(wrapper.contains("FullbrightOverlayLighting.apply"));
        assertFalse(scanner.contains("GLASS_SAMPLES"));
        assertFalse(scanner.contains("GLASS_INSPECTION"));
        assertFalse(surfaceGeometry.contains("drawGlassSkin"));
    }

    private static int nonTransparentPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) count++;
            }
        }
        return count;
    }

    private static boolean containsRgb(BufferedImage image, int rgb) {
        int expected = rgb & 0xFFFFFF;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) != 0 && (argb & 0xFFFFFF) == expected) return true;
            }
        }
        return false;
    }
}
