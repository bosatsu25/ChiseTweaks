package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.VanillaOreVisualCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Headless release contracts for Ore Highlights that can be proven without launching Minecraft. */
final class OreHighlightsReleaseReadinessContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    private static final Set<String> EXPECTED_ORE_IDS = Set.of(
            "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
            "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
            "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
            "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
            "minecraft:ancient_debris");

    private static final Set<String> EXPECTED_HIGHLIGHT_KEYS = Set.of(
            "coal", "iron", "copper", "gold", "lapis", "redstone", "diamond", "emerald",
            "nether_gold", "nether_quartz", "ancient_debris");

    @Test
    void canonicalCatalogCoversExactlyAllVanillaOreVariants() {
        assertEquals(11, VanillaOreVisualCatalog.familyCount());
        assertEquals(19, VanillaOreVisualCatalog.blockVariantCount());
        assertEquals(EXPECTED_ORE_IDS, VanillaOreVisualCatalog.blockIds());
        assertEquals(EXPECTED_HIGHLIGHT_KEYS,
                VanillaOreVisualCatalog.families().stream()
                        .map(VanillaOreVisualCatalog.Family::highlightKey)
                        .collect(Collectors.toSet()));
    }

    @Test
    void oreHighlightsStayModelBackedAndCannotBecomeAWorldScanXrayPath() throws IOException {
        String engine = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java");
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");

        assertTrue(engine.contains(
                "definition.inspectionCategory() != BlockInspectionCategory.MATERIAL_HIGHLIGHT"));
        assertTrue(plugin.contains("PreparableModelLoadingPlugin.register"));
        assertTrue(plugin.contains("ModelModifier.WRAP_PHASE"));
        assertTrue(plugin.contains("pluginContext.addModel("));

        for (String forbidden : Set.of(
                "WorksiteScanner", "LavaAnalyzerThroughWallRenderer",
                "withDepthStencilState(Optional.empty())", "RenderPipeline",
                "ClientLevel", "BlockPos.betweenClosed")) {
            assertFalse(plugin.contains(forbidden), forbidden);
            assertFalse(model.contains(forbidden), forbidden);
        }
    }

    @Test
    void resourcePackFinalModelIsNeverReplacedByChise() throws IOException {
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");
        String generator = source("gradle/chise-visual-assets.gradle");

        assertTrue(plugin.contains("modifyBlockModelAfterBake"));
        assertTrue(plugin.contains("SimpleUnbakedExtraModel.blockStateModel"));
        assertFalse(plugin.contains("OVERRIDE_PHASE"));
        assertFalse(plugin.contains("modifyBlockModelOnLoad"));
        assertFalse(plugin.contains("SingleVariant"));
        assertTrue(model.contains("super.emitQuads(emitter, level, pos, state, random, cullTest);"));
        assertTrue(model.indexOf("super.emitQuads") < model.indexOf("emitter.pushTransform"));
        assertFalse(generator.contains("minecraft:block/diamond_ore"));
        assertFalse(generator.contains("cubeModel"));
        assertFalse(generator.contains("ancientDebrisModel"));
        assertTrue(generator.contains("overlayOnlyModel"));
    }

    @Test
    void oreSettingsNeverTriggerFullResourcePackReload() throws IOException {
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String invalidation = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightRenderInvalidation.java");
        String bindings = source(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        String editor = source(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseOreCompatibilityScreen.java");

        assertFalse(plugin.contains("reloadResourcePacks"));
        assertFalse(invalidation.contains("reloadResourcePacks"));
        assertFalse(bindings.contains("reloadResourcePacks"));
        assertFalse(editor.contains("reloadResourcePacks"));
        assertTrue(invalidation.contains("client.levelRenderer.allChanged()"));
        assertTrue(invalidation.contains("AtomicBoolean REQUESTED"));
        assertTrue(bindings.contains("OreHighlightRenderInvalidation.request"));
        assertTrue(editor.contains("OreHighlightRenderInvalidation.request()"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualModelReloadCoordinator.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/performance/VisualModelReloadThrottlePolicy.java")));
    }

    @Test
    void overlayOwnershipIsExplicitRatherThanCoordinateInferred() throws IOException {
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");
        assertTrue(model.contains("overlay.emitQuads"));
        assertTrue(model.contains("emitter.pushTransform"));
        assertTrue(model.indexOf("emitter.pushTransform") < model.indexOf("overlay.emitQuads"));
        assertTrue(model.indexOf("overlay.emitQuads") < model.indexOf("emitter.popTransform"));
        assertFalse(model.contains("isOverlayVertex"));
        assertFalse(model.contains("OreHighlightLightingPolicy"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/vision/OreHighlightLightingPolicy.java")));
    }

    @Test
    void shaderStateCannotChangeTheSubmittedOreHighlightVisualLanguage() throws IOException {
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");
        String lighting = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayLighting.java");
        assertTrue(plugin.contains("visual-model-overlay-7-ore-kelp-party"));
        assertTrue(model.contains("FullbrightOverlayLighting.apply(quad)"));
        assertTrue(lighting.contains("quad.emissive(true)"));
        assertTrue(lighting.contains("quad.diffuseShade(false)"));
        assertTrue(lighting.contains("quad.ambientOcclusion(TriState.FALSE)"));
        for (String forbidden : Set.of("IrisApi", "isShaderPackInUse", "shaderPackName", "ShaderRenderer")) {
            assertFalse(plugin.contains(forbidden), forbidden);
            assertFalse(model.contains(forbidden), forbidden);
            assertFalse(lighting.contains(forbidden), forbidden);
        }
    }

    @Test
    void generatedHighlightAssetsProvideStaticDefaultAndOptionalEightFrameMotion() throws IOException {
        String generator = source("gradle/chise-visual-assets.gradle");
        assertTrue(generator.contains("new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)"));
        assertTrue(generator.contains("new BufferedImage(16, 128, BufferedImage.TYPE_INT_ARGB)"));
        assertTrue(generator.contains("(0..<8).each"));
        assertTrue(generator.contains("frametime: 1"));
        assertTrue(generator.contains("interpolate: true"));
        assertTrue(generator.contains("_highlight_static.png"));
        assertTrue(generator.contains("_static.json"));
        assertTrue(generator.contains("_animated.json"));
        assertTrue(generator.contains("overlayOnlyModel"));

        Pattern motifPattern = Pattern.compile("motif: '([^']+)'");
        Matcher matcher = motifPattern.matcher(generator);
        Set<String> motifs = new java.util.LinkedHashSet<>();
        while (matcher.find()) motifs.add(matcher.group(1));
        assertEquals(13, motifs.size());
        assertTrue(motifs.containsAll(Set.of(
                "crystal", "dotted", "runes", "brackets", "double", "wave", "nodes",
                "sparks", "whorl", "jagged", "tears", "nether_gold", "quartz")));
    }

    @Test
    void firstLaunchDefaultsAreNonIntrusiveReducedMotionAndReadyWhenEnabled() throws IOException {
        String featureSwitch = source("src/main/java/dev/chise/chisetweaks/config/FeatureSwitch.java");
        String localConfig = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureConfig.java");
        assertTrue(featureSwitch.contains("private static final boolean DEFAULT_ENABLED = false"));
        assertTrue(localConfig.contains("public boolean oreHighlightAnimationEnabled = false"));
        assertTrue(localConfig.contains(
                "public int visualTargetMask = VisualTargetSelectionPolicy.ALL_TARGETS_MASK"));
    }

    @Test
    void moddedOreCompatibilityRemainsLayeredClientOnlyAndBounded() throws IOException {
        String resolver = source(
                "src/main/java/dev/chise/chisetweaks/core/vision/OreHighlightResolver.java");
        String loader = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightResourceCompatibilityLoader.java");
        String api = source(
                "src/main/java/dev/chise/chisetweaks/api/ore/OreHighlightApi.java");
        String config = source(
                "src/main/java/dev/chise/chisetweaks/config/OreHighlightCompatibilityConfig.java");

        assertTrue(resolver.contains("ConventionalBlockTags.ORES"));
        assertTrue(resolver.contains("ModdedOreIdPolicy.looksLikeOre"));
        assertTrue(loader.contains("chisetweaks/ore_compat"));
        assertTrue(loader.contains("MAX_RESOURCE_ENTRIES = 512"));
        assertTrue(loader.contains("StrictJsonSecurityPolicy.validateObjectDocument"));
        assertTrue(api.contains("registerBlock"));
        assertTrue(api.contains("registerTag"));
        assertTrue(config.contains("MAX_ENTRIES = 256"));
        assertTrue(config.contains("SecureConfigStorage.writeUtf8Atomic"));
        assertFalse(resolver.contains("ClientLevel"));
        assertFalse(loader.contains("HttpClient"));
        assertFalse(api.contains("network"));
    }

    @Test
    void requiredGithubStatusNameStaysAlignedWithBranchProtection() throws IOException {
        String workflow = source(".github/workflows/verify-build.yml");
        assertTrue(workflow.contains("name: Java 25 quality gate"));
        assertFalse(workflow.contains("name: Java 25 retained-scope quality gate"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
