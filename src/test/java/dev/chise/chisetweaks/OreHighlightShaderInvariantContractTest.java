package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-level contract for the Ore Highlights shader-invariant rendering requirement.
 *
 * <p>Headless CI cannot prove the final pixels produced by an arbitrary shader pack. It can,
 * however, prevent Chise itself from disabling, restyling, or switching renderer paths merely
 * because shaders are enabled. Real shader-pack appearance remains part of the Prism runtime gate.</p>
 */
final class OreHighlightShaderInvariantContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void oreHighlightsAlwaysSubmitTheSameEmissiveOverlayMaterial() throws IOException {
        String model = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java"));

        assertTrue(model.contains("applyShaderInvariantHighlightLighting(quad)"));
        assertTrue(model.contains("quad.emissive(true)"));
        assertTrue(model.contains("quad.diffuseShade(false)"));
        assertTrue(model.contains("quad.ambientOcclusion(TriState.FALSE)"));
        assertTrue(model.contains("OreHighlightLightingPolicy.isOverlayVertex"));

        assertFalse(model.contains("IrisApi"));
        assertFalse(model.contains("isShaderPackInUse"));
        assertFalse(model.contains("shaderPackName"));
        assertFalse(model.contains("ShaderRenderer"));
    }

    @Test
    void modelPluginDoesNotDisableOrRestyleOreHighlightsForShaders() throws IOException {
        String plugin = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java"));

        assertTrue(plugin.contains("ore-highlight-emissive-overlay-4-shader-invariant"));
        assertTrue(plugin.contains("ModelModifier.WRAP_PHASE"));
        assertTrue(plugin.contains("new FullbrightOreHighlightModel(model)"));
        assertTrue(plugin.contains("VisualModelSelectionPolicy.activeMaterialModelMask"));

        assertFalse(plugin.contains("IrisApi"));
        assertFalse(plugin.contains("isShaderPackInUse"));
        assertFalse(plugin.contains("shaderPackName"));
        assertFalse(plugin.contains("ShaderRenderer"));
    }

    @Test
    void shaderCompatibilityDoesNotAddAHardIrisDependency() throws IOException {
        String build = Files.readString(ROOT.resolve("build.gradle"));
        String metadata = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));

        assertFalse(build.contains("maven.modrinth:iris"));
        assertFalse(metadata.contains("\"iris\""));
        assertFalse(metadata.contains("irisshaders"));
    }
}
