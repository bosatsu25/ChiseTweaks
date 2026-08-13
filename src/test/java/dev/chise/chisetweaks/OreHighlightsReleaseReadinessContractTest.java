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

/**
 * Headless release contracts for Ore Highlights that can be proven without launching Minecraft.
 *
 * <p>These tests intentionally stop short of claiming final-pixel equivalence under shader packs.
 * They prove the deterministic preconditions that runtime visual QA depends on: complete vanilla
 * coverage, model-backed rendering rather than a world scanner, shader-invariant emissive material
 * semantics, independently identifiable highlight motifs, safe defaults, fail-soft reload handling,
 * and CI status-name enforcement.</p>
 */
final class OreHighlightsReleaseReadinessContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    private static final Set<String> EXPECTED_ORE_IDS = Set.of(
            "minecraft:coal_ore",
            "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore",
            "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore",
            "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore",
            "minecraft:deepslate_gold_ore",
            "minecraft:lapis_ore",
            "minecraft:deepslate_lapis_ore",
            "minecraft:redstone_ore",
            "minecraft:deepslate_redstone_ore",
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:deepslate_emerald_ore",
            "minecraft:nether_gold_ore",
            "minecraft:nether_quartz_ore",
            "minecraft:ancient_debris");

    private static final Set<String> EXPECTED_HIGHLIGHT_KEYS = Set.of(
            "coal",
            "iron",
            "copper",
            "gold",
            "lapis",
            "redstone",
            "diamond",
            "emerald",
            "nether_gold",
            "nether_quartz",
            "ancient_debris");

    @Test
    void canonicalCatalogCoversExactlyAllVanillaOreVariants() {
        assertEquals(11, VanillaOreVisualCatalog.familyCount());
        assertEquals(19, VanillaOreVisualCatalog.blockVariantCount());
        assertEquals(EXPECTED_ORE_IDS, VanillaOreVisualCatalog.blockIds());
        assertEquals(
                EXPECTED_HIGHLIGHT_KEYS,
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

        // MATERIAL_HIGHLIGHT is deliberately excluded from the bounded worksite scanner.
        assertTrue(engine.contains(
                "definition.inspectionCategory() != BlockInspectionCategory.MATERIAL_HIGHLIGHT"));
        assertTrue(plugin.contains("ModelLoadingPlugin.register"));
        assertTrue(plugin.contains("ModelModifier.WRAP_PHASE"));
        assertTrue(plugin.contains("new FullbrightOreHighlightModel(model)"));

        // Ore visibility must not grow a second through-wall/world-overlay backend.
        for (String forbidden : Set.of(
                "WorksiteScanner",
                "LavaAnalyzerThroughWallRenderer",
                "withDepthStencilState(Optional.empty())",
                "RenderPipeline",
                "ClientLevel",
                "BlockPos.betweenClosed")) {
            assertFalse(plugin.contains(forbidden), forbidden);
            assertFalse(model.contains(forbidden), forbidden);
        }
    }

    @Test
    void shaderStateCannotChangeTheSubmittedOreHighlightVisualLanguage() throws IOException {
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");

        assertTrue(plugin.contains("ore-highlight-emissive-overlay-4-shader-invariant"));
        assertTrue(model.contains("quad.emissive(true)"));
        assertTrue(model.contains("quad.diffuseShade(false)"));
        assertTrue(model.contains("quad.ambientOcclusion(TriState.FALSE)"));

        for (String forbidden : Set.of("IrisApi", "isShaderPackInUse", "shaderPackName", "ShaderRenderer")) {
            assertFalse(plugin.contains(forbidden), forbidden);
            assertFalse(model.contains(forbidden), forbidden);
        }
    }

    @Test
    void generatedHighlightAssetsKeepEightFrameAnimationAndDistinctMotifs() throws IOException {
        String generator = source("gradle/chise-visual-assets.gradle");

        assertTrue(generator.contains("new BufferedImage(16, 128, BufferedImage.TYPE_INT_ARGB)"));
        assertTrue(generator.contains("(0..<8).each { frame ->"));
        assertTrue(generator.contains("frametime: 1"));
        assertTrue(generator.contains("interpolate: true"));
        assertTrue(generator.contains("minecraft:block/"));
        assertTrue(generator.contains("overlayElement('#highlight')"));

        Pattern motifPattern = Pattern.compile("motif: '([^']+)'");
        Matcher matcher = motifPattern.matcher(generator);
        Set<String> motifs = new java.util.LinkedHashSet<>();
        while (matcher.find()) motifs.add(matcher.group(1));

        // 11 ore families + Obsidian + Crying Obsidian each retain an independent motif.
        assertEquals(13, motifs.size());
        assertTrue(motifs.containsAll(Set.of(
                "crystal", "dotted", "runes", "brackets", "double", "wave", "nodes",
                "sparks", "whorl", "jagged", "tears", "nether_gold", "quartz")));
    }

    @Test
    void firstLaunchDefaultsAreNonIntrusiveButReadyWhenEnabled() throws IOException {
        String featureSwitch = source(
                "src/main/java/dev/chise/chisetweaks/config/FeatureSwitch.java");
        String localConfig = source(
                "src/main/java/dev/chise/chisetweaks/config/LocalFeatureConfig.java");

        // No visual feature changes the player's view merely by installing the JAR.
        assertTrue(featureSwitch.contains("private static final boolean DEFAULT_ENABLED = false"));

        // Once the user enables Ore Highlights, every retained ore/material target is preselected.
        assertTrue(localConfig.contains(
                "public int visualTargetMask = VisualTargetSelectionPolicy.ALL_TARGETS_MASK"));
        assertTrue(localConfig.contains(
                "visualTargetMask = VisualTargetSelectionPolicy.ALL_TARGETS_MASK"));
    }

    @Test
    void modelReloadCoordinatorCommitsOnlySuccessfulReloadStateAndBacksOffFailures() throws IOException {
        String coordinator = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualModelReloadCoordinator.java");

        assertTrue(coordinator.contains("client.reloadResourcePacks().whenComplete"));
        assertTrue(coordinator.contains("if (failure == null)"));
        assertTrue(coordinator.contains("commitStagedState();"));
        assertTrue(coordinator.contains("RELOAD_THROTTLE.onReloadSucceeded();"));
        assertTrue(coordinator.contains("discardStagedState();"));
        assertTrue(coordinator.contains("RELOAD_THROTTLE.onReloadFailed();"));
        assertTrue(coordinator.contains("catch (RuntimeException failure)"));
        assertTrue(coordinator.contains("RELOAD_IN_FLIGHT.set(false);"));

        // The last successfully applied mask is not overwritten inside the async failure branch.
        String failureBranch = coordinator.substring(
                coordinator.indexOf("} else {", coordinator.indexOf("if (failure == null)")),
                coordinator.indexOf("RELOAD_IN_FLIGHT.set(false);", coordinator.indexOf("if (failure == null)")));
        assertFalse(failureBranch.contains("appliedMaterialModelMask ="));
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
