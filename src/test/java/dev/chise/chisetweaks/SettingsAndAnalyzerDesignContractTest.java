package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.exists;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the design boundaries introduced by the settings/rendering cleanup. */
final class SettingsAndAnalyzerDesignContractTest {
    @Test
    void localSettingsUseConfigAsSingleInMemorySourceOfTruth() throws IOException {
        String settings = read("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targets = read("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");

        assertContainsAll(settings,
                "() -> config().worksiteVisibilityHorizontalRadius",
                "value -> config().worksiteVisibilityHorizontalRadius = value",
                "() -> config().lavaAnalyzerIntervalTicks",
                "value -> config().lavaAnalyzerIntervalTicks = value");
        assertContainsNone(settings,
                "syncFromStorage",
                "private static boolean syncing");
        assertContainsAll(targets,
                "LocalFeatureConfig.getInstance().visualTargetMask",
                "VisualTargetSelectionPolicy.withEnabled(");
        assertContainsNone(targets,
                "syncFromConfig",
                "private static boolean syncing");
    }

    @Test
    void configLoadingDoesNotReachIntoRenderingState() throws IOException {
        String config = read("src/main/java/dev/chise/chisetweaks/config/FeatureConfig.java");
        String bindings = read("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");

        assertContainsNone(config,
                "feature.rendering",
                "BuilderFocusVisibility");
        assertContainsAll(bindings, "BuilderFocusVisibility.applyConfig()");
    }

    @Test
    void analyzerFeaturesShareOnlyGenericRetentionAndRenderingInfrastructure() throws IOException {
        String lava = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String debris = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");

        assertContainsAll(lava,
                "ThroughWallMarkerRenderer.Style.LAVA_SOURCE",
                "new NearestPositionBuffer(");
        assertContainsAll(debris,
                "ThroughWallMarkerRenderer.Style.ANCIENT_DEBRIS",
                "new NearestPositionBuffer(");
        assertContainsAll(renderer, "enum Style");
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisThroughWallRenderer.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaSourceSnapshot.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisSnapshot.java"));
    }

    @Test
    void analyzerDiscoveryStaysLoadedChunkOnlyAndDebrisBootstrapIsIncremental() throws IOException {
        String lava = read("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String debris = read("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");

        assertContainsAll(lava, "getChunkNow(");
        assertContainsAll(debris,
                "getChunkNow(",
                "scheduleLoadedChunkBootstrap(",
                "processPendingLoadedChunks(",
                "AncientDebrisAnalyzerPolicy.MAX_BOOTSTRAP_CHUNKS_PER_TICK");
        assertContainsNone(debris,
                "private void bootstrapLoadedChunks(",
                "getChunk(chunkX, chunkZ, true)");
        assertContainsNone(lava, "getChunk(chunkX, chunkZ, true)");
    }

    @Test
    void settingsPresentationUsesFiveTabsReadmeNamesAndMinecraftLocalizedEditors() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String catalog = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java");
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String definition = read("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java");
        String sceneFilter = read("src/main/java/dev/chise/chisetweaks/gui/ChiseSceneFilterEditorScreen.java");
        String oreCompat = read("src/main/java/dev/chise/chisetweaks/gui/ChiseOreCompatibilityScreen.java");
        String english = read("src/main/resources/assets/chisetweaks/lang/en_us.json");
        String japanese = read("src/main/resources/assets/chisetweaks/lang/ja_jp.json");

        assertContainsAll(controller,
                "ChiseTweaksSettingsCatalog",
                "HIGHLIGHT",
                "VISUAL_FILTER",
                "ANALYZER",
                "VISIBILITY",
                "HELP");
        assertContainsNone(controller,
                "boolean japanese",
                "japanese ?",
                "HIGHLIGHT_DETAILS",
                "VISUAL_FILTER_DETAILS",
                "LAVA_DETAILS");
        assertContainsAll(catalog,
                "Component.translatable(",
                "definition.englishName()",
                "\"Highlight\"",
                "\"Visual Filter\"",
                "\"Analyzer\"",
                "\"Visibility\"",
                "\"使い方\"",
                "\"Bright Chest\"",
                "\"Bright Concrete\"");
        assertContainsAll(definition,
                "\"Ore Highlights\"",
                "\"Nether Highlight\"",
                "\"Fine Line Highlight\"",
                "\"Hidden Block Highlight\"",
                "\"Glass Highlight\"",
                "\"Kelp Highlight\"",
                "\"Block Filter\"",
                "\"Entity Filter\"",
                "\"Lava Analyzer\"",
                "\"Ancient Debris Analyzer\"",
                "\"Low Fire\"");
        assertContainsAll(screen,
                "ChiseTweaksMetadata.MOD_NAME",
                "\"設定を適用\"",
                "\"設定をリセット\"");
        assertContainsNone(screen,
                "ChiseTweaksMetadata.MOD_VERSION",
                "Resource reload:",
                "ChiseTweaksHelpScreen");
        assertContainsNone(sceneFilter, "boolean japanese");
        assertContainsNone(oreCompat, "boolean japanese");
        assertTrue(english.contains("\"screen.chisetweaks.scene_filter.title\""));
        assertTrue(japanese.contains("\"screen.chisetweaks.ore_compat.title\""));
    }
}
