package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.exists;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Guards the Tweaks-oriented settings/rendering boundaries. */
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
    }

    @Test
    void occludedHighlightsRetainBoundedSharedInfrastructure() throws IOException {
        String feature = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java");
        String renderer = read("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");

        assertContainsAll(feature,
                "ThroughWallMarkerRenderer.Style.LAVA_SOURCE",
                "ThroughWallMarkerRenderer.Style.HIDDEN_BLOCK",
                "new NearestPositionBuffer(",
                "getChunkNow(");
        assertContainsAll(renderer, "enum Style", "LAVA_SOURCE", "HIDDEN_BLOCK");
        assertContainsNone(renderer, "ANCIENT_DEBRIS", "AncientDebrisAnalyzerPolicy");
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java"));
        assertContainsNone(feature, "getChunk(chunkX, chunkZ, true)");
    }

    @Test
    void villagerVisualizationUsesKnownMinecraftJobSiteOnly() throws IOException {
        String villager = read("src/main/java/dev/chise/chisetweaks/feature/rendering/VillagerAnalyzerFeature.java");

        assertContainsAll(villager,
                "MemoryModuleType.JOB_SITE",
                "collectKnownLinks",
                "FeatureDefinition.VILLAGER_ANALYZER.englishName()");
        assertContainsNone(villager,
                "FALLBACK_WORKSTATION_RADIUS",
                "findNearestLoadedWorkstation",
                "VillagerWorkstationPolicy",
                "LoadedChunkWindow",
                "FALLBACK_COLOR");
    }

    @Test
    void settingsPresentationUsesTweaksProductComposition() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String productRows = read("src/main/java/dev/chise/chisetweaks/gui/TweaksProductSettingsRows.java");
        String assistRows = read("src/main/java/dev/chise/chisetweaks/gui/TweaksBuilderAssistRows.java");
        String grouping = read("src/main/java/dev/chise/chisetweaks/core/definition/TweaksProductGroupPolicy.java");
        String definition = read("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java");

        assertContainsAll(controller,
                "TweaksProductSettingsRows.rows",
                "TweaksBuilderAssistRows.rows",
                "resetTechnicalVisualizationDetails",
                "resetOccludedHighlightDetails",
                "SettingPersistenceCoordinator.production()");
        assertContainsAll(productRows,
                "Builder Highlights",
                "Scene Filter",
                "Builder Assist",
                "Technical",
                "Visual Tweaks",
                "FeatureSwitches.LAVA_HIGHLIGHT",
                "FeatureSwitches.HIDDEN_SURFACE_TRACE",
                "FeatureSwitches.VILLAGER_ANALYZER");
        assertContainsAll(assistRows,
                "inspector.filter",
                "inspector.matchedRule",
                "inspector.features",
                "inspector.renderMode");
        assertContainsAll(grouping,
                "TweaksProductGroup.BUILDER_HIGHLIGHTS",
                "TweaksProductGroup.TECHNICAL_VISUALIZATION",
                "TweaksProductGroup.VISUAL_TWEAKS",
                "TweaksProductGroup.SCENE_FILTER");
        assertContainsAll(definition,
                "\"Lava Source Highlight\"",
                "\"Hidden Material Highlight\"",
                "\"Villager Job Site Links\"");
        assertContainsNone(definition,
                "\"Lava Analyzer\"",
                "\"Hidden Block Analyzer\"",
                "\"Villager Analyzer\"");
    }

    @Test
    void builderAssistStillRetainsPlacementPatternSchematicAndHistoryCapabilities() throws IOException {
        String rows = read("src/main/java/dev/chise/chisetweaks/gui/InspectorSettingsRows.java");
        String productRows = read("src/main/java/dev/chise/chisetweaks/gui/TweaksBuilderAssistRows.java");

        assertContainsAll(rows,
                "addPlacementRows(",
                "addSchematicPlacementRows(",
                "addPatternConsistencyRows(",
                "addInteractionHistoryRows(");
        assertContainsAll(productRows,
                "InspectorSettingsRows.rows",
                "product.builderAssist");
    }
}
