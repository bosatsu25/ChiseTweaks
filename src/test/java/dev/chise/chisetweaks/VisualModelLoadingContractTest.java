package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualModelLoadingContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path GENERATED_ROOT = ROOT.resolve("build/generated/chiseVisualAssets/assets/chisetweaks");
    private static final Path GENERATED_VISUAL_MODELS = GENERATED_ROOT.resolve("models/block/visual/material");
    private static final Path GENERATED_VISUAL_TEXTURES = GENERATED_ROOT.resolve("textures/block/visual/material");
    private static final List<String> MATERIAL_KEYS = List.of(
            "diamond", "gold", "emerald", "coal", "iron",
            "copper", "lapis", "redstone", "ancient_debris", "obsidian");

    @Test
    void allMaterialHighlightsUseFabricModelLoadingInsteadOfTheWorldLinePass() throws IOException {
        String plugin = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java"));

        assertTrue(plugin.contains("ModelLoadingPlugin.register"));
        assertTrue(plugin.contains("modifyBlockModelOnLoad"));
        assertTrue(plugin.contains("ModelModifier.OVERRIDE_PHASE"));
        assertTrue(plugin.contains("new SingleVariant.Unbaked(new Variant(replacement)).asRoot()"));
        for (String block : List.of(
                "OBSIDIAN", "ANCIENT_DEBRIS",
                "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE",
                "GOLD_ORE", "DEEPSLATE_GOLD_ORE",
                "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE",
                "COAL_ORE", "DEEPSLATE_COAL_ORE",
                "IRON_ORE", "DEEPSLATE_IRON_ORE",
                "COPPER_ORE", "DEEPSLATE_COPPER_ORE",
                "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE",
                "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE")) {
            assertTrue(plugin.contains("Blocks." + block), block);
        }
        assertFalse(plugin.contains("RenderTypes.lines"));
        assertFalse(plugin.contains("SurfaceLineVisualGeometry"));
    }

    @Test
    void generatedModelsKeepVanillaTexturesAndAddGeometryOnlyHighlightFrames() throws IOException {
        try (var models = Files.list(GENERATED_VISUAL_MODELS)) {
            assertEquals(18, models.filter(path -> path.toString().endsWith(".json")).count());
        }

        String diamond = Files.readString(GENERATED_VISUAL_MODELS.resolve("diamond_ore.json"));
        String deepslateDiamond = Files.readString(
                GENERATED_VISUAL_MODELS.resolve("deepslate_diamond_ore.json"));
        String debris = Files.readString(GENERATED_VISUAL_MODELS.resolve("ancient_debris.json"));

        assertTrue(diamond.contains("minecraft:block/diamond_ore"));
        assertTrue(diamond.contains("chisetweaks:block/visual/material/diamond_highlight"));
        assertTrue(deepslateDiamond.contains("minecraft:block/deepslate_diamond_ore"));
        assertTrue(deepslateDiamond.contains("chisetweaks:block/visual/material/diamond_highlight"));
        assertTrue(debris.contains("minecraft:block/ancient_debris_top"));
        assertTrue(debris.contains("minecraft:block/ancient_debris_side"));
        assertTrue(diamond.contains("-0.06"));
        assertTrue(diamond.contains("\"shade\": false"));
    }

    @Test
    void generatedModelsUseThirtyOrFewerRawFacesWithoutCornerCubes() throws IOException {
        try (var models = Files.list(GENERATED_VISUAL_MODELS)) {
            for (Path model : models.filter(path -> path.toString().endsWith(".json")).toList()) {
                String json = Files.readString(model);
                int rawFaceCount = countOccurrences(json, "\"texture\"");
                assertTrue(rawFaceCount <= 30, model.getFileName() + " raw faces=" + rawFaceCount);
            }
        }

        String generator = Files.readString(ROOT.resolve("gradle/chise-visual-assets.gradle"));
        assertTrue(generator.contains("outwardEdgeFaces"));
        assertFalse(generator.contains("addCornerNodes"));
    }

    @Test
    void generatedMaterialBordersHaveFastEightFrameAnimations() throws IOException {
        for (String key : MATERIAL_KEYS) {
            Path texture = GENERATED_VISUAL_TEXTURES.resolve(key + "_highlight.png");
            BufferedImage image = ImageIO.read(texture.toFile());
            assertNotNull(image, key);
            assertEquals(16, image.getWidth(), key);
            assertEquals(128, image.getHeight(), key);

            String meta = Files.readString(GENERATED_VISUAL_TEXTURES.resolve(key + "_highlight.png.mcmeta"));
            assertTrue(meta.contains("\"frametime\": 1"), key);
            assertTrue(meta.contains("\"interpolate\": true"), key);
            assertTrue(frameBrightness(image, 4) > frameBrightness(image, 0), key + " peak brightness");
            assertFalse(framesEqual(image, 0, 2), key + " animated sparkle/motif");
        }
    }

    @Test
    void generatedBorderFamiliesMatchTheirMaterialIdentity() throws IOException {
        int[] diamond = averageFrame("diamond", 4);
        int[] gold = averageFrame("gold", 4);
        int[] emerald = averageFrame("emerald", 4);
        int[] coal = averageFrame("coal", 4);
        int[] iron = averageFrame("iron", 4);
        int[] copper = averageFrame("copper", 4);
        int[] lapis = averageFrame("lapis", 4);
        int[] redstone = averageFrame("redstone", 4);
        int[] debris = averageFrame("ancient_debris", 4);
        int[] obsidian = averageFrame("obsidian", 4);

        assertTrue(diamond[1] > diamond[0] && diamond[2] > diamond[0]);
        assertTrue(gold[0] > gold[2] && gold[1] > gold[2]);
        assertTrue(emerald[1] > emerald[0] && emerald[1] > emerald[2]);
        assertTrue(max(coal) - min(coal) < 40);
        assertTrue(iron[0] > iron[1] && iron[1] > iron[2]);
        assertTrue(copper[0] > copper[1] && copper[0] > copper[2]);
        assertTrue(lapis[2] > lapis[0] && lapis[2] > lapis[1]);
        assertTrue(redstone[0] > redstone[1] && redstone[0] > redstone[2]);
        assertTrue(debris[0] > debris[1] && debris[1] > debris[2]);
        assertTrue(obsidian[0] > obsidian[1] && obsidian[2] > obsidian[1]);
    }

    @Test
    void oreHighlightLocalizationMatchesTheModelBackedImplementation() throws IOException {
        String ja = Files.readString(ROOT.resolve("src/main/resources/assets/chisetweaks/lang/ja_jp.json"));
        String en = Files.readString(ROOT.resolve("src/main/resources/assets/chisetweaks/lang/en_us.json"));

        assertTrue(ja.contains("\"config.name.materialhighlights\": \"鉱石ハイライト\""));
        assertTrue(en.contains("\"config.name.materialhighlights\": \"Ore Highlights\""));
        assertTrue(ja.contains("固有色のアニメーション枠"));
        assertTrue(en.contains("ore-matched animated frames"));
        assertFalse(ja.contains("件数制限付き"));
        assertFalse(ja.contains("独自生成した線描画で、見えている鉱石"));
        assertFalse(en.contains("Only loaded blocks with direct line of sight are considered"));
    }

    @Test
    void buildLogicGeneratesModelsTexturesAndAnimationMetadataTogether() throws IOException {
        String generator = Files.readString(ROOT.resolve("gradle/chise-visual-assets.gradle"));
        String settings = Files.readString(ROOT.resolve("settings.gradle"));

        assertTrue(settings.contains("chise-visual-assets.gradle"));
        assertTrue(generator.contains("generateChiseVisualAssets"));
        assertTrue(generator.contains("writeBorderTexture"));
        assertTrue(generator.contains("drawSparkle"));
        assertTrue(generator.contains("addBorderRods"));
        assertTrue(generator.contains("writeAnimationMeta"));
        assertTrue(generator.contains("ancientDebrisModel"));
    }

    private static int[] averageFrame(String key, int frame) throws IOException {
        BufferedImage image = ImageIO.read(
                GENERATED_VISUAL_TEXTURES.resolve(key + "_highlight.png").toFile());
        long r = 0;
        long g = 0;
        long b = 0;
        int top = frame * 16;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int rgb = image.getRGB(x, top + y);
                r += (rgb >>> 16) & 0xFF;
                g += (rgb >>> 8) & 0xFF;
                b += rgb & 0xFF;
            }
        }
        return new int[] {(int) (r / 256), (int) (g / 256), (int) (b / 256)};
    }

    private static long frameBrightness(BufferedImage image, int frame) {
        long total = 0;
        int top = frame * 16;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int rgb = image.getRGB(x, top + y);
                total += ((rgb >>> 16) & 0xFF) + ((rgb >>> 8) & 0xFF) + (rgb & 0xFF);
            }
        }
        return total;
    }

    private static boolean framesEqual(BufferedImage image, int firstFrame, int secondFrame) {
        int firstTop = firstFrame * 16;
        int secondTop = secondFrame * 16;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (image.getRGB(x, firstTop + y) != image.getRGB(x, secondTop + y)) return false;
            }
        }
        return true;
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static int max(int[] value) {
        return Math.max(value[0], Math.max(value[1], value[2]));
    }

    private static int min(int[] value) {
        return Math.min(value[0], Math.min(value[1], value[2]));
    }
}
