package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class KelpHighlightStyleContractTest {
    private static final Path GENERATED = Path.of(
            "build/generated/chiseKelpVisualAssets/assets/chisetweaks");

    @Test
    void kelpPartyAssetsUseRequestedMagentaAndOrangePalette() throws IOException {
        String generator = Files.readString(Path.of("gradle/chise-kelp-visual-assets.gradle"));
        assertTrue(generator.contains("def KELP_MAGENTA = [255, 79, 216]"));
        assertTrue(generator.contains("def KELP_ORANGE = [255, 138, 0]"));
        assertTrue(generator.contains("animation: [frametime: 3, interpolate: true]"));
    }

    @Test
    void kelpUsesCrossedAnimatedPlantPlanesInsteadOfCubeMarkers() throws IOException {
        String generator = Files.readString(Path.of("gradle/chise-kelp-visual-assets.gradle"));
        assertTrue(generator.contains("partyPlane('#highlight', 45)"));
        assertTrue(generator.contains("partyPlane('#highlight', -45)"));
        assertTrue(generator.contains("kelp_party.png"));
        assertTrue(generator.contains("models/block/visual/kelp"));
        assertFalse(generator.contains("cullface"));
    }

    @Test
    void generatedKelpAssetsArePresentWellFormedAndSeparatedFromOreAssets() throws IOException {
        Path model = GENERATED.resolve("models/block/visual/kelp/party_overlay.json");
        Path texture = GENERATED.resolve("textures/block/visual/kelp/kelp_party.png");
        Path metadata = GENERATED.resolve("textures/block/visual/kelp/kelp_party.png.mcmeta");
        Path oreNamespaceCollision = GENERATED.resolve("models/block/visual/overlay/kelp_party.json");

        assertTrue(Files.isRegularFile(model));
        assertTrue(Files.isRegularFile(texture));
        assertTrue(Files.isRegularFile(metadata));
        assertFalse(Files.exists(oreNamespaceCollision));

        String modelJson = Files.readString(model);
        assertTrue(modelJson.contains("\"ambientocclusion\": false"));
        assertTrue(modelJson.contains("\"angle\": 45"));
        assertTrue(modelJson.contains("\"angle\": -45"));
        assertTrue(modelJson.contains("chisetweaks:block/visual/kelp/kelp_party"));

        String animationJson = Files.readString(metadata);
        assertTrue(animationJson.contains("\"frametime\": 3"));
        assertTrue(animationJson.contains("\"interpolate\": true"));

        var image = ImageIO.read(texture.toFile());
        assertNotNull(image);
        assertEquals(16, image.getWidth());
        assertEquals(128, image.getHeight());
    }

    @Test
    void kelpAndOreShareTheSameFullbrightLightingTransform() throws IOException {
        String kelpModel = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightKelpHighlightModel.java"));
        String oreModel = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java"));
        String lighting = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayLighting.java"));

        assertTrue(kelpModel.contains("FullbrightOverlayLighting.apply(quad)"));
        assertTrue(oreModel.contains("FullbrightOverlayLighting.apply(quad)"));
        assertTrue(lighting.contains("quad.emissive(true)"));
        assertTrue(lighting.contains("quad.diffuseShade(false)"));
        assertTrue(lighting.contains("ambientOcclusion(TriState.FALSE)"));
    }

    @Test
    void kelpModelPluginWrapsBothVanillaKelpBlocks() throws IOException {
        String plugin = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java"));
        String catalog = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/KelpHighlightOverlayCatalog.java"));
        assertTrue(plugin.contains("\"kelp\".equals(path)"));
        assertTrue(plugin.contains("\"kelp_plant\".equals(path)"));
        assertTrue(plugin.contains("new FullbrightKelpHighlightModel(model)"));
        assertTrue(plugin.contains("KelpHighlightOverlayCatalog.KEY"));
        assertTrue(catalog.contains("block/visual/kelp/party_overlay"));
    }

    @Test
    void kelpIsNoLongerAWorksiteScannerFeature() {
        assertFalse(FeatureDefinition.KELP_HIGHLIGHT.isWorksiteVisibilityMode());
        assertNull(FeatureDefinition.KELP_HIGHLIGHT.worksiteMode());
        assertNull(FeatureDefinition.KELP_HIGHLIGHT.inspectionCategory());
    }
}
