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
    void worksiteHighlightsHaveNoRuntimeMutualExclusionBinding() throws IOException {
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        String catalog = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java");

        assertFalse(bindings.contains("bindExclusiveWorksiteMode"));
        assertFalse(bindings.contains("applyWorksiteModesAtomically"));
        assertFalse(bindings.contains("worksiteVisibilityExclusiveMode"));
        assertFalse(catalog.contains("\"highlightExclusiveMode\""));
    }

    @Test
    void chestVisibilityUsesMinecraftResourcePackStateAndReloads() throws IOException {
        String controller = source(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackController.java");
        String registrar = source(
                "src/main/java/dev/chise/chisetweaks/feature/resource/ChiseTexturePackRegistrar.java");
        String catalog = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java");

        assertTrue(registrar.contains("Identifier.fromNamespaceAndPath"));
        assertTrue(registrar.contains("repositoryPackId()"));
        assertTrue(controller.contains("getSelectedIds()"));
        assertTrue(controller.contains("getAvailableIds()"));
        assertTrue(controller.contains("setSelected(selected)"));
        assertTrue(controller.contains("options.updateResourcePacks(repository)"));
        assertTrue(controller.contains("reloadResourcePacks()"));
        assertTrue(catalog.contains("\"chestVisibility\""));
        assertTrue(catalog.contains("Chest Visibility / チェスト視認性"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
