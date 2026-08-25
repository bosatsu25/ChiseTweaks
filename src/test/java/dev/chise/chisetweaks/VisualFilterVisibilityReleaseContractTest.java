package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression contract for the Visual Filter, independent highlights and visibility helpers. */
final class VisualFilterVisibilityReleaseContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void visualFilterLowFireAndBrightChestPassTheReleaseGate() throws IOException {
        String policy = source("src/main/java/dev/chise/chisetweaks/core/policy/FeatureAvailabilityPolicy.java");
        String plugin = source("src/main/java/dev/chise/chisetweaks/mixin/FeatureAvailabilityMixinConfigPlugin.java");

        assertTrue(policy.contains("EnumSet.allOf(FeatureDefinition.class)"));
        assertTrue(plugin.contains("BuilderFocusBlockMixin"));
        assertTrue(plugin.contains("BlockEntityVisualStateMixin"));
        assertTrue(plugin.contains("BuilderFocusEntityMixin"));
        assertTrue(plugin.contains("FireVisibilityMixin"));
        assertTrue(plugin.contains("FeatureDefinition.BRIGHT_CHEST"));
        assertTrue(plugin.contains("FeatureAvailabilityPolicy.isAvailable(feature)"));
    }

    @Test
    void worksiteHighlightsHaveNoRuntimeOrConfigMutualExclusionResidue() throws IOException {
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        String catalog = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java");
        String localConfig = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureConfig.java");
        String localSettings = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String featureDefinition = source("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java");

        assertFalse(bindings.contains("bindExclusiveWorksiteMode"));
        assertFalse(bindings.contains("applyWorksiteModesAtomically"));
        assertFalse(bindings.contains("worksiteVisibilityExclusiveMode"));
        assertFalse(catalog.contains("\"highlightExclusiveMode\""));
        assertFalse(localConfig.contains("worksiteVisibilityExclusiveMode"));
        assertFalse(localConfig.contains("worksiteVisibilityMaxResults"));
        assertFalse(localSettings.contains("WORKSITE_VISIBILITY_EXCLUSIVE_MODE"));
        assertFalse(localSettings.contains("setWorksiteVisibilityModeChangedCallback"));
        assertFalse(featureDefinition.contains("WorksiteVisibilitySelectionPolicy"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/core/policy/WorksiteVisibilitySelectionPolicy.java")));
    }

    @Test
    void brightVisibilityFeaturesAreIndependentWithoutResourcePackSelectionOrCustomPngs() throws IOException {
        String switches = source("src/main/java/dev/chise/chisetweaks/config/FeatureSwitches.java");
        String blockEntity = source(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/BlockEntityVisualStateMixin.java");
        String visualPlugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java");
        String catalog = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java");

        assertTrue(switches.contains("BRIGHT_CHEST = local("));
        assertTrue(switches.contains("BRIGHT_CONCRETE = local("));
        assertTrue(switches.contains("\"brightChest\","));
        assertTrue(switches.contains("\"brightConcrete\","));
        assertTrue(blockEntity.contains("FeatureSwitches.BRIGHT_CHEST.getBooleanValue()"));
        assertTrue(blockEntity.contains("chest.lightCoords = LightCoordsUtil.FULL_BRIGHT"));
        assertFalse(blockEntity.contains("customSprite"));
        assertTrue(visualPlugin.contains("FullbrightOverlayModel.brightConcrete(model)"));
        assertTrue(visualPlugin.contains("\"white_concrete\""));
        int bright = model.indexOf("private void emitBrightConcrete");
        int overlay = model.indexOf("private void emitExtraModel");
        assertTrue(bright >= 0 && overlay > bright);
        String brightBody = model.substring(bright, overlay);
        assertTrue(brightBody.contains("FullbrightOverlayLighting.apply(quad)"));
        assertTrue(brightBody.contains("super.emitQuads(emitter, level, pos, state, random, cullTest)"));
        assertFalse(brightBody.contains("FullbrightOverlayEmission.emit"));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/feature/resource")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/resourcepacks/chise_chest_visibility")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/resourcepacks/chise_white_concrete_visibility")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/chisetweaks/textures/entity/chest/normal.png")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/chisetweaks/textures/block/visual/bright_white_concrete.png")));
        assertTrue(catalog.contains("FeatureDefinition.BRIGHT_CHEST.englishName()"));
        assertTrue(catalog.contains("FeatureDefinition.BRIGHT_CONCRETE.englishName()"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
