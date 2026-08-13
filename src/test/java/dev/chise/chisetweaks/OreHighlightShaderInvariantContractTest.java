package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Headless contract for shader-invariant, non-destructive Ore Highlight rendering. */
final class OreHighlightShaderInvariantContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void oreHighlightsAlwaysSubmitTheSameEmissiveChiseOwnedOverlayMaterial() throws IOException {
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");

        assertTrue(model.contains("super.emitQuads(emitter, level, pos, state, random, cullTest);"));
        assertTrue(model.contains("applyShaderInvariantHighlightLighting(quad)"));
        assertTrue(model.contains("quad.emissive(true)"));
        assertTrue(model.contains("quad.diffuseShade(false)"));
        assertTrue(model.contains("quad.ambientOcclusion(TriState.FALSE)"));
        assertTrue(model.contains("overlay.emitQuads(emitter, level, pos, state, random, cullTest)"));
        assertFalse(model.contains("OreHighlightLightingPolicy"));
        assertFalse(model.contains("isOverlayVertex"));
        assertFalse(model.contains("IrisApi"));
        assertFalse(model.contains("isShaderPackInUse"));
        assertFalse(model.contains("shaderPackName"));
        assertFalse(model.contains("ShaderRenderer"));
    }

    @Test
    void modelPluginWrapsFinalModelInsteadOfReplacingItForShadersOrResourcePacks() throws IOException {
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");

        assertTrue(plugin.contains("ore-highlight-composed-overlay-5-nondestructive"));
        assertTrue(plugin.contains("pluginContext.addModel("));
        assertTrue(plugin.contains("SimpleUnbakedExtraModel.blockStateModel"));
        assertTrue(plugin.contains("ModelModifier.WRAP_PHASE"));
        assertTrue(plugin.contains("new FullbrightOreHighlightModel("));
        assertFalse(plugin.contains("ModelModifier.OVERRIDE_PHASE"));
        assertFalse(plugin.contains("modifyBlockModelOnLoad"));
        assertFalse(plugin.contains("SingleVariant.Unbaked"));
        assertFalse(plugin.contains("IrisApi"));
        assertFalse(plugin.contains("isShaderPackInUse"));
        assertFalse(plugin.contains("shaderPackName"));
        assertFalse(plugin.contains("ShaderRenderer"));
    }

    @Test
    void shaderCompatibilityDoesNotAddAHardIrisDependency() throws IOException {
        String build = source("build.gradle");
        String metadata = source("src/main/resources/fabric.mod.json");
        assertFalse(build.contains("maven.modrinth:iris"));
        assertFalse(metadata.contains("\"iris\""));
        assertFalse(metadata.contains("irisshaders"));
    }

    private static String source(String path) throws IOException {
        return Files.readString(ROOT.resolve(path));
    }
}
