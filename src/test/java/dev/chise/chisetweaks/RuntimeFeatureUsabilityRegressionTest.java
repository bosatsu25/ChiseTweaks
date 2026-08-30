package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression contract for the 15 user-facing runtime features.
 *
 * This test proves registration/config/runtime wiring. Visual correctness still belongs to
 * Client GameTest / Prism acceptance because source contracts cannot prove pixels on screen.
 */
final class RuntimeFeatureUsabilityRegressionTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void allFifteenFeaturesHaveOneIndependentToggleAndStableDefaults() {
        assertEquals(15, FeatureDefinition.VALUES.size());
        assertEquals(15, FeatureSwitches.VALUES.size());

        Set<FeatureDefinition> definitions = new HashSet<>();
        Set<String> settingNames = new HashSet<>();
        for (FeatureSwitch setting : FeatureSwitches.VALUES) {
            definitions.add(setting.definition());
            assertTrue(settingNames.add(setting.getName()), setting.getName());
        }
        assertEquals(Set.copyOf(FeatureDefinition.VALUES), definitions);

        Set<FeatureDefinition> enabledByDefault = new HashSet<>();
        for (FeatureSwitch setting : FeatureSwitches.VALUES) {
            if (setting.getDefaultBooleanValue()) enabledByDefault.add(setting.definition());
        }
        assertEquals(
                Set.of(FeatureDefinition.BRIGHT_CHEST, FeatureDefinition.BRIGHT_CONCRETE),
                enabledByDefault);
    }

    @Test
    void everyFeatureHasAConcreteRuntimeOrRenderingRoute() throws IOException {
        Map<FeatureDefinition, Route> routes = new EnumMap<>(FeatureDefinition.class);
        routes.put(FeatureDefinition.BUILDER_FOCUS_BLOCKS, route(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/BuilderFocusBlockMixin.java",
                "BuilderFocusVisibility.shouldHide(asState().getBlock())"));
        routes.put(FeatureDefinition.BUILDER_FOCUS_ENTITIES, route(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/BuilderFocusEntityMixin.java",
                "FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue()"));
        routes.put(FeatureDefinition.FINE_THREAD_TRACE, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java",
                "toggle.definition().inspectionCategory()"));
        routes.put(FeatureDefinition.HIDDEN_SURFACE_TRACE, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java",
                "FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue()"));
        routes.put(FeatureDefinition.GLASS_INSPECTION, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualRenderState.java",
                "FeatureSwitches.GLASS_INSPECTION.getBooleanValue()"));
        routes.put(FeatureDefinition.MATERIAL_HIGHLIGHTS, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualRenderState.java",
                "FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue()"));
        routes.put(FeatureDefinition.NETHER_PALETTE, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java",
                "toggle.definition().inspectionCategory()"));
        routes.put(FeatureDefinition.KELP_HIGHLIGHT, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualRenderState.java",
                "FeatureSwitches.KELP_HIGHLIGHT.getBooleanValue()"));
        routes.put(FeatureDefinition.FIRE_VISIBILITY, route(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/FireVisibilityMixin.java",
                "FeatureSwitches.FIRE_VISIBILITY.getBooleanValue()"));
        routes.put(FeatureDefinition.LAVA_HIGHLIGHT, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java",
                "FeatureSwitches.LAVA_HIGHLIGHT.getBooleanValue()"));
        routes.put(FeatureDefinition.VILLAGER_ANALYZER, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/VillagerAnalyzerFeature.java",
                "LocalFeatureConfig.getInstance().villagerAnalyzerEnabled"));
        routes.put(FeatureDefinition.BEACON_RANGE, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/InfrastructureRangeFeature.java",
                "config.beaconRangeEnabled"));
        routes.put(FeatureDefinition.LIGHTNING_ROD_RANGE, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/InfrastructureRangeFeature.java",
                "config.lightningRodRangeEnabled"));
        routes.put(FeatureDefinition.BRIGHT_CHEST, route(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/ChestVisibilityMixin.java",
                "FeatureSwitches.BRIGHT_CHEST.getBooleanValue()"));
        routes.put(FeatureDefinition.BRIGHT_CONCRETE, route(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualRenderState.java",
                "FeatureSwitches.BRIGHT_CONCRETE.getBooleanValue()"));

        assertEquals(Set.copyOf(FeatureDefinition.VALUES), routes.keySet());
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            Route route = routes.get(definition);
            String source = source(route.path());
            assertTrue(source.contains(route.marker()),
                    () -> definition + " lost route marker " + route.marker());
        }
    }

    @Test
    void sharedRuntimesStillKeepTheirUserTogglesIndependent() throws IOException {
        String analyzers = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java");
        assertTrue(analyzers.contains("FeatureSwitches.LAVA_HIGHLIGHT.getBooleanValue()"));
        assertTrue(analyzers.contains("FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue()"));
        assertTrue(analyzers.contains("LAVA_MASK"));
        assertTrue(analyzers.contains("HIDDEN_MASK"));

        String infrastructure = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/InfrastructureRangeFeature.java");
        assertTrue(infrastructure.contains("config.beaconRangeEnabled"));
        assertTrue(infrastructure.contains("config.lightningRodRangeEnabled"));
        assertTrue(infrastructure.contains("BEACON_MASK"));
        assertTrue(infrastructure.contains("LIGHTNING_ROD_MASK"));

        String worksite = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java");
        assertTrue(worksite.contains("for (FeatureSwitch toggle : FeatureSwitches.VALUES)"));
        assertTrue(worksite.contains("toggle.getBooleanValue()"));
        assertTrue(worksite.contains("toggle.definition().inspectionCategory()"));
        assertEquals(BlockInspectionCategory.TECHNICAL_TRACE,
                FeatureDefinition.FINE_THREAD_TRACE.inspectionCategory());
        assertEquals(BlockInspectionCategory.NETHER_PALETTE,
                FeatureDefinition.NETHER_PALETTE.inspectionCategory());
        assertFalse(FeatureDefinition.HIDDEN_SURFACE_TRACE.inspectionCategory() == null);
    }

    @Test
    void modelBackedFeaturesHaveBothToggleStateAndModelEmissionPaths() throws IOException {
        String state = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualRenderState.java");
        for (String toggle : new String[]{
                "MATERIAL_HIGHLIGHTS",
                "GLASS_INSPECTION",
                "KELP_HIGHLIGHT",
                "BRIGHT_CONCRETE"}) {
            assertTrue(state.contains("FeatureSwitches." + toggle + ".getBooleanValue()"), toggle);
        }

        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java");
        assertTrue(model.contains("KIND_ORE"));
        assertTrue(model.contains("KIND_GLASS"));
        assertTrue(model.contains("KIND_KELP"));
        assertTrue(model.contains("KIND_BRIGHT_CONCRETE"));
        assertTrue(model.contains("shouldRender(renderState)"));
        assertTrue(model.contains("FullbrightOverlayLighting.apply(quad)"));
    }

    @Test
    void mixinBackedFeaturesRemainAvailabilityGated() throws IOException {
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/mixin/FeatureAvailabilityMixinConfigPlugin.java");
        for (String feature : new String[]{
                "BUILDER_FOCUS_BLOCKS",
                "BUILDER_FOCUS_ENTITIES",
                "FIRE_VISIBILITY",
                "BRIGHT_CHEST"}) {
            assertTrue(plugin.contains("FeatureDefinition." + feature), feature);
        }
        for (String mixin : new String[]{
                "BuilderFocusBlockMixin",
                "BuilderFocusEntityMixin",
                "FireVisibilityMixin",
                "ChestVisibilityMixin"}) {
            assertTrue(plugin.contains(mixin), mixin);
        }
        assertTrue(plugin.contains("FeatureAvailabilityPolicy.isAvailable(feature)"));
    }

    private static Route route(String path, String marker) {
        return new Route(path, marker);
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }

    private record Route(String path, String marker) {}
}
