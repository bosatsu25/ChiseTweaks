package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.exists;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
                "private static boolean syncing",
                "AncientDebrisAnalyzerPolicy");
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
    void sharedAnalyzersRetainGenericThroughWallInfrastructureWithoutRetiredScanner() throws IOException {
        String analyzers = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");

        assertContainsAll(analyzers,
                "ThroughWallMarkerRenderer.Style.LAVA_SOURCE",
                "new NearestPositionBuffer(",
                "getChunkNow(");
        assertContainsAll(renderer, "enum Style", "LAVA_SOURCE");
        assertContainsNone(renderer, "ANCIENT_DEBRIS", "AncientDebrisAnalyzerPolicy");
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/core/policy/AncientDebrisAnalyzerPolicy.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaSourceSnapshot.java"));
        assertContainsNone(analyzers, "getChunk(chunkX, chunkZ, true)");
    }

    @Test
    void settingsPresentationUsesSixTabsAndRetainedProductNames() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String rows = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsRows.java");
        String inspectorRows = read("src/main/java/dev/chise/chisetweaks/gui/InspectorSettingsRows.java");
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String definition = read("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java");
        String sceneFilter = read("src/main/java/dev/chise/chisetweaks/gui/ChiseListEditorScreen.java");
        String oreCompat = read("src/main/java/dev/chise/chisetweaks/gui/ChiseListEditorScreen.java");
        String english = read("src/main/resources/assets/chisetweaks/lang/en_us.json");
        String japanese = read("src/main/resources/assets/chisetweaks/lang/ja_jp.json");

        assertContainsAll(controller,
                "ChiseTweaksSettingRowDefinition",
                "HIGHLIGHT",
                "FILTER",
                "INSPECTOR",
                "ANALYZER",
                "VISIBILITY");
        assertContainsNone(controller,
                "withAirPlacement",
                "FeatureSwitches.AIR_PLACEMENT",
                "ANCIENT_DEBRIS_ANALYZER",
                "boolean japanese",
                "japanese ?",
                "HIGHLIGHT_DETAILS",
                "VISUAL_FILTER_DETAILS",
                "LAVA_DETAILS");
        assertContainsAll(rows,
                "addCompatibilityRows(",
                "configOptionKey(",
                "addTargets(");
        assertContainsAll(inspectorRows,
                "addPlacementRows(",
                "addSchematicPlacementRows(",
                "addPatternConsistencyRows(",
                "addInteractionHistoryRows(");
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java"));
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/gui/CompatibilitySettingsRows.java"));
        assertContainsAll(rows,
                "Component.translatable(",
                "FeatureSwitch config",
                "config.definition().englishName()",
                ".name().toLowerCase(java.util.Locale.ROOT)",
                "FeatureSwitches.BRIGHT_CHEST",
                "FeatureSwitches.BRIGHT_CONCRETE");
        assertContainsAll(controller,
                "return ChiseTweaksSettingsRows.rows(",
                "return InspectorSettingsRows.rows(",
                "SettingPersistenceCoordinator.production()");
        assertContainsNone(rows,
                "FeatureDefinition.AIR_PLACEMENT",
                "FeatureDefinition.ANCIENT_DEBRIS_ANALYZER",
                "FeatureDefinition.BRIGHT_CHEST.englishName()",
                "FeatureDefinition.BRIGHT_CONCRETE.englishName()");
        assertContainsNone(controller,
                "addHighlightRows(",
                "addInspectorRows(",
                "addCompatibilityRows(");
        assertContainsAll(definition,
                "\"Ore Highlights\"",
                "\"Nether Highlight\"",
                "\"Fine Line Highlight\"",
                "\"Hidden Block Analyzer\"",
                "\"Glass Highlight\"",
                "\"Kelp Highlight\"",
                "\"Block Filter\"",
                "\"Entity Filter\"",
                "\"Lava Analyzer\"",
                "\"Low Fire\"",
                "\"Bright Chest\"",
                "\"Bright Concrete\"");
        assertContainsNone(definition,
                "\"Air Placement\"",
                "\"Ancient Debris Analyzer\"");
        assertContainsAll(screen,
                "ChiseTweaksMetadata.MOD_NAME",
                "screen.chisetweaks.settings.apply_changes",
                "screen.chisetweaks.settings.reset_all");
        assertContainsNone(screen,
                "ChiseTweaksMetadata.MOD_VERSION",
                "Resource reload:",
                "ChiseTweaksHelpScreen");
        assertContainsNone(sceneFilter, "boolean japanese");
        assertContainsNone(oreCompat, "boolean japanese");

        assertContainsAll(english,
                "\"screen.chisetweaks.settings.apply_changes\": \"Apply settings\"",
                "\"screen.chisetweaks.settings.tab.integrations\": \"Integrations\"",
                "\"config.name.brightchest\": \"Bright Chest\"",
                "\"config.name.brightconcrete\": \"Bright Concrete\"",
                "direct lighting-only Bright features");
        assertContainsNone(english,
                "config.option.localworksitevisibilityexclusivemode",
                "Single Visibility Mode",
                "built-in Resource Pack");

        assertContainsAll(japanese,
                "\"screen.chisetweaks.settings.apply_changes\": \"設定を適用\"",
                "\"screen.chisetweaks.settings.tab.integrations\": \"連携\"",
                "\"config.name.brightchest\": \"Bright Chest\"",
                "\"config.name.brightconcrete\": \"Bright Concrete\"",
                "Bright切替のためのResource Pack再読み込みは行いません");
        assertContainsNone(japanese,
                "config.option.localworksitevisibilityexclusivemode",
                "視認ガイド単独表示",
                "保持した11機能",
                "built-in Resource Pack");
    }
}
