package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Architecture guard for layered modded Ore Highlight compatibility. */
final class OreHighlightCompatibilityArchitectureContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void automaticDetectionUsesConventionalTagsBeforeConservativeFallback() throws IOException {
        String resolver = source("src/main/java/dev/chise/chisetweaks/core/vision/OreHighlightResolver.java");
        int explicit = resolver.indexOf("styleForBlockId");
        int apiTag = resolver.indexOf("styleForApiTag");
        int conventional = resolver.indexOf("conventionalStyle(state)");
        int fallback = resolver.indexOf("ModdedOreIdPolicy.looksLikeOre");

        assertTrue(explicit >= 0);
        assertTrue(explicit < apiTag);
        assertTrue(apiTag < conventional);
        assertTrue(conventional < fallback);
        assertTrue(resolver.contains("ConventionalBlockTags.ORES"));
        assertTrue(resolver.contains("ConventionalBlockTags.NETHERITE_SCRAP_ORES"));
        assertTrue(resolver.contains("public static long revision()"));
        assertFalse(resolver.contains("BlockPos.betweenClosed"));
    }

    @Test
    void resourceAndLocalCompatibilityAreStrictBoundedOfflineAndTransactional() throws IOException {
        String loader = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightResourceCompatibilityLoader.java");
        String config = source(
                "src/main/java/dev/chise/chisetweaks/config/OreHighlightCompatibilityConfig.java");
        String registry = source(
                "src/main/java/dev/chise/chisetweaks/core/vision/OreHighlightExternalRegistry.java");

        assertTrue(loader.contains("chisetweaks/ore_compat"));
        assertTrue(loader.contains("MAX_RESOURCE_ENTRIES = 512"));
        assertTrue(loader.contains("StrictJsonSecurityPolicy.validateObjectDocument"));
        assertTrue(config.contains("MAX_ENTRIES = 256"));
        assertTrue(config.contains("SecureConfigStorage.writeUtf8Atomic"));
        assertTrue(config.contains("if (save && !saveEntries(nextEntries)) return false;"));
        assertTrue(registry.indexOf("configBlocks.get") < registry.indexOf("resourceBlocks.get"));
        assertFalse(loader.contains("HttpClient"));
        assertFalse(loader.contains("URL("));
    }

    @Test
    void onlyResolvedTargetsAreWrappedAndRuntimeEmissionDoesNotResolveOreIdentity() throws IOException {
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        String model = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOreHighlightModel.java");

        assertTrue(plugin.contains("PreparableModelLoadingPlugin.register"));
        assertTrue(plugin.contains("ModelModifier.WRAP_PHASE"));
        assertTrue(plugin.contains("OreHighlightResolver.resolve(state)"));
        assertTrue(plugin.contains("if (resolved == null || resolved.style() == null) return model;"));
        assertFalse(plugin.contains("if (!\"minecraft\".equals(namespace))"));
        assertFalse(plugin.contains("new FullbrightOreHighlightModel(model)"));
        assertFalse(plugin.contains("OVERRIDE_PHASE"));

        assertTrue(model.contains("super.emitQuads(emitter, level, pos, state, random, cullTest);"));
        assertTrue(model.contains("if (!highlightEnabled()) return;"));
        assertFalse(model.contains("OreHighlightResolver.resolve"));
        assertFalse(model.contains("OreHighlightResolver.revision()"));
        assertFalse(model.contains("dynamicModded"));
        assertFalse(model.contains("ConcurrentHashMap"));
        assertFalse(model.contains("IrisApi"));
    }

    @Test
    void editorAndPublicApiMoveRareTargetChangesToColdModelReloadPath() throws IOException {
        String screen = source(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseOreCompatibilityScreen.java");
        String configScreen = source(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String api = source(
                "src/main/java/dev/chise/chisetweaks/api/ore/OreHighlightApi.java");
        String reload = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/OreHighlightModelReload.java");

        assertTrue(screen.contains("OreHighlightCompatibilityConfig.put"));
        assertTrue(screen.contains("OreHighlightCompatibilityConfig.clear()"));
        assertTrue(screen.contains("OreHighlightModelReload.request()"));
        assertTrue(screen.contains("BuiltInRegistries.BLOCK"));
        assertTrue(configScreen.contains("case EDIT_ORE_COMPAT"));
        assertTrue(configScreen.contains("new ChiseOreCompatibilityScreen"));
        assertTrue(api.contains("registerBlock"));
        assertTrue(api.contains("registerTag"));
        assertTrue(api.contains("OreHighlightModelReload.request()"));
        assertTrue(reload.contains("ChiseVisualModelPlugin.isModelPipelineReady()"));
        assertTrue(reload.contains("client.execute"));
        assertTrue(reload.contains("client.reloadResourcePacks()"));
        assertFalse(api.contains("ClientPlayNetworking"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
