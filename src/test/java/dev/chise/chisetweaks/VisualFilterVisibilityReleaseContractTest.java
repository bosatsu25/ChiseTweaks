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
    void visualFilterAndLowFirePassTheReleaseGate() throws IOException {
        String policy = source("src/main/java/dev/chise/chisetweaks/core/policy/FeatureAvailabilityPolicy.java");
        String plugin = source("src/main/java/dev/chise/chisetweaks/mixin/FeatureAvailabilityMixinConfigPlugin.java");

        assertTrue(policy.contains("EnumSet.allOf(FeatureDefinition.class)"));
        assertTrue(plugin.contains("BuilderFocusBlockMixin"));
        assertTrue(plugin.contains("BuilderFocusBlockEntityMixin"));
        assertTrue(plugin.contains("BuilderFocusEntityMixin"));
        assertTrue(plugin.contains("FireVisibilityMixin"));
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
    void visibilityPacksAreIndependentAndUseTheDelayedTextureReloadPath() throws IOException {
        String controller = source(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java");
        String coordinator = source(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ResourceReloadCoordinator.java");
        String registrar = source(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackRegistrar.java");
        String visibilityPack = source(
                "src/main/java/dev/chise/chisetweaks/feature/resource/VisibilityPack.java");
        String chestSetting = source(
                "src/main/java/dev/chise/chisetweaks/config/ChestVisibilitySetting.java");
        String concreteSetting = source(
                "src/main/java/dev/chise/chisetweaks/config/WhiteConcreteVisibilitySetting.java");
        String catalog = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java");

        assertTrue(visibilityPack.contains("CHEST(\"chise_chest_visibility\", \"Bright Chest\")"));
        assertTrue(visibilityPack.contains("WHITE_CONCRETE(\"chise_white_concrete_visibility\", \"Bright Concrete\")"));
        assertTrue(registrar.contains("for (VisibilityPack pack : VisibilityPack.values())"));
        assertTrue(controller.contains("isEnabled(VisibilityPack pack)"));
        assertTrue(controller.contains("setEnabled(VisibilityPack pack, boolean enabled)"));
        assertTrue(controller.contains("getSelectedIds()"));
        assertTrue(controller.contains("getAvailableIds()"));
        assertTrue(controller.contains("repository.setSelected(selected)"));
        assertTrue(controller.contains("options.updateResourcePacks(repository)"));
        assertTrue(controller.contains("delayTextureReload().whenComplete"));
        assertFalse(controller.contains("reloadResourcePacks().whenComplete"));
        assertTrue(controller.contains("client.execute(() -> completeReload"));
        assertTrue(controller.contains("ResourceReloadCoordinator RELOADS"));
        assertTrue(controller.contains("RELOADS.markPending(selected)"));
        assertTrue(controller.contains("TERMINAL_RECOVERY"));
        assertTrue(controller.contains("RELOADS.cancel("));
        assertTrue(controller.contains("restoreSelection"));
        assertTrue(coordinator.contains("record Recovery"));
        assertTrue(coordinator.contains("terminalFailure"));
        assertTrue(coordinator.contains("Action.RELOAD"));
        assertTrue(coordinator.contains("Action.RESTORE"));
        assertTrue(chestSetting.contains("VisibilityPack.CHEST"));
        assertTrue(chestSetting.contains("\"Bright Chest\""));
        assertTrue(concreteSetting.contains("VisibilityPack.WHITE_CONCRETE"));
        assertTrue(concreteSetting.contains("\"Bright Concrete\""));
        assertTrue(catalog.contains("\"chestVisibility\""));
        assertTrue(catalog.contains("\"whiteConcreteVisibility\""));
        assertTrue(catalog.contains("\"Bright Chest\""));
        assertTrue(catalog.contains("\"Bright Concrete\""));
        assertFalse(catalog.contains("Chest Visibility / チェスト視認性"));
        assertFalse(catalog.contains("White Concrete Visibility / 白色コンクリート視認性"));
        assertFalse(catalog.contains("bundled white-concrete"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
