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
        String policy = source("src/main/java/dev/chise/chisetweaks/core/policy/PreReleaseFeaturePolicy.java");
        String plugin = source("src/main/java/dev/chise/chisetweaks/mixin/PreReleaseMixinConfigPlugin.java");

        assertTrue(policy.contains("FeatureDefinition.BUILDER_FOCUS_BLOCKS"));
        assertTrue(policy.contains("FeatureDefinition.BUILDER_FOCUS_ENTITIES"));
        assertTrue(policy.contains("FeatureDefinition.FIRE_VISIBILITY"));
        assertTrue(plugin.contains("BuilderFocusBlockMixin"));
        assertTrue(plugin.contains("BuilderFocusEntityMixin"));
        assertTrue(plugin.contains("FireVisibilityMixin"));
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
    void chestVisibilityCoalescesReloadsAndRecoversAsynchronousFailures() throws IOException {
        String controller = source(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java");
        String registrar = source(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackRegistrar.java");
        String catalog = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java");

        assertTrue(registrar.contains("Identifier.fromNamespaceAndPath"));
        assertTrue(registrar.contains("repositoryPackId()"));
        assertTrue(controller.contains("getSelectedIds()"));
        assertTrue(controller.contains("getAvailableIds()"));
        assertTrue(controller.contains("repository.setSelected(selected)"));
        assertTrue(controller.contains("options.updateResourcePacks(repository)"));
        assertTrue(controller.contains("reloadResourcePacks().whenComplete"));
        assertTrue(controller.contains("client.execute(() -> completeReload"));
        assertTrue(controller.contains("reloadInFlight"));
        assertTrue(controller.contains("reloadPending"));
        assertTrue(controller.contains("restoreSelection"));
        assertTrue(catalog.contains("\"chestVisibility\""));
        assertTrue(catalog.contains("Chest Visibility / チェスト視認性"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
