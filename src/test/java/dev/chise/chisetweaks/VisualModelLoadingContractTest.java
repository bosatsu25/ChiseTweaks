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

final class VisualModelLoadingContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path VISUAL_MODELS = ROOT.resolve(
            "src/main/resources/assets/chisetweaks/models/block/visual");
    private static final Path VISUAL_TEXTURES = ROOT.resolve(
            "src/main/resources/assets/chisetweaks/textures/block/visual");

    @Test
    void diamondPocUsesFabricModelLoadingInsteadOfAnotherWorldLinePass() throws IOException {
        String plugin = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java"));

        assertTrue(plugin.contains("ModelLoadingPlugin.register"));
        assertTrue(plugin.contains("modifyBlockModelOnLoad"));
        assertTrue(plugin.contains("ModelModifier.OVERRIDE_PHASE"));
        assertTrue(plugin.contains("new SingleVariant.Unbaked(new Variant(replacement)).asRoot()"));
        assertTrue(plugin.contains("Blocks.DIAMOND_ORE"));
        assertTrue(plugin.contains("Blocks.DEEPSLATE_DIAMOND_ORE"));
        assertFalse(plugin.contains("RenderTypes.lines"));
        assertFalse(plugin.contains("SurfaceLineVisualGeometry"));
    }

    @Test
    void diamondModelsUseOnlyChiseOwnedTextureResources() throws IOException {
        String diamond = Files.readString(VISUAL_MODELS.resolve("diamond_ore.json"));
        String deepslate = Files.readString(VISUAL_MODELS.resolve("deepslate_diamond_ore.json"));

        assertTrue(diamond.contains("\"parent\": \"minecraft:block/cube_all\""));
        assertTrue(diamond.contains("chisetweaks:block/visual/diamond_ore_chise"));
        assertTrue(deepslate.contains("\"parent\": \"minecraft:block/cube_all\""));
        assertTrue(deepslate.contains("chisetweaks:block/visual/deepslate_diamond_ore_chise"));
        assertFalse(diamond.contains("amateras"));
        assertFalse(deepslate.contains("amateras"));
    }

    @Test
    void animatedDiamondTexturesHaveEightSixteenPixelFrames() throws IOException {
        assertAnimatedTexture(VISUAL_TEXTURES.resolve("diamond_ore_chise.png"));
        assertAnimatedTexture(VISUAL_TEXTURES.resolve("deepslate_diamond_ore_chise.png"));

        String diamondMeta = Files.readString(VISUAL_TEXTURES.resolve("diamond_ore_chise.png.mcmeta"));
        String deepslateMeta = Files.readString(
                VISUAL_TEXTURES.resolve("deepslate_diamond_ore_chise.png.mcmeta"));

        assertTrue(diamondMeta.contains("\"frametime\": 3"));
        assertTrue(diamondMeta.contains("\"interpolate\": true"));
        assertTrue(deepslateMeta.contains("\"frametime\": 3"));
        assertTrue(deepslateMeta.contains("\"interpolate\": true"));
    }

    private static void assertAnimatedTexture(Path texture) throws IOException {
        BufferedImage image = ImageIO.read(texture.toFile());
        assertNotNull(image);
        assertEquals(16, image.getWidth());
        assertEquals(128, image.getHeight());
    }
}
