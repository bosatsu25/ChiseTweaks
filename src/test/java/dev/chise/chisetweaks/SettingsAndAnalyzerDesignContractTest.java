package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.chise.chisetweaks.SourceContractSupport.assertContainsAll;
import static dev.chise.chisetweaks.SourceContractSupport.assertContainsNone;
import static dev.chise.chisetweaks.SourceContractSupport.exists;
import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class SettingsAndAnalyzerDesignContractTest {
    @Test
    void localSettingsUseConfigAsSingleInMemorySourceOfTruth() throws IOException {
        String settings = read("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targets = read("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");

        assertContainsAll(settings,
                "() -> config().worksiteVisibilityHorizontalRadius",
                "value -> config().worksiteVisibilityHorizontalRadius = value",
                "() -> config().occludedHighlightIntervalTicks",
                "value -> config().occludedHighlightIntervalTicks = value");
        assertContainsNone(settings,
                "LAVA_ANALYZER_HORIZONTAL_RADIUS",
                "HIDDEN_ANALYZER_HORIZONTAL_RADIUS",
                "AncientDebrisAnalyzerPolicy");
        assertContainsAll(targets,
                "LocalFeatureConfig.getInstance().visualTargetMask",
                "VisualTargetSelectionPolicy.withEnabled(");
    }

    @Test
    void occludedHighlightsUseOneBoundedTraversalBudget() throws IOException {
        String feature = read(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/OccludedHighlightsFeature.java");
        String renderer = read(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");

        assertContainsAll(feature,
                "FeatureSwitches.LAVA_HIGHLIGHT.getBooleanValue()",
                "FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue()",
                "local.occludedHighlightHorizontalRadius",
                "local.occludedHighlightVerticalRadius",
                "local.occludedHighlightIntervalTicks",
                "local.occludedHighlightMaxOverlayResults",
                "LoadedChunkWindow loadedChunks",
                "ThroughWallMarkerRenderer.Style.LAVA_SOURCE",
                "ThroughWallMarkerRenderer.Style.HIDDEN_BLOCK");
        assertContainsNone(feature,
                "lavaAnalyzerHorizontalRadius",
                "hiddenAnalyzerHorizontalRadius",
                "int[] ticksUntilScan",
                "getChunk(chunkX, chunkZ, true)");
        assertContainsAll(renderer, "enum Style", "LAVA_SOURCE", "HIDDEN_BLOCK");
        assertFalse(exists(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallAnalyzerFeature.java"));
    }

    @Test
    void villagerLinksVisualizeKnownJobSiteMemoryWithoutFallbackGuessing() throws IOException {
        String links = read(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/VillagerJobSiteLinksFeature.java");

        assertContainsAll(links,
                "MemoryModuleType.JOB_SITE",
                "memory.isEmpty()",
                "memory.get().pos()");
        assertContainsNone(links,
                "VillagerWorkstationPolicy",
                "findNearestLoadedWorkstation",
                "LoadedChunkWindow",
                "getBlockState(");
        assertFalse(exists(
                "src/main/java/dev/chise/chisetweaks/core/policy/VillagerWorkstationPolicy.java"));
        assertFalse(exists(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/VillagerAnalyzerFeature.java"));
    }

    @Test
    void settingsPresentationUsesSevenTweaksGroups() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String rows = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsRows.java");
        String assist = read("src/main/java/dev/chise/chisetweaks/gui/BuilderAssistRows.java");
        String workflow = read("src/main/java/dev/chise/chisetweaks/gui/WorkflowRows.java");
        String definition = read("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java");

        assertContainsAll(controller,
                "VISUAL",
                "BUILDER_HIGHLIGHTS",
                "TECHNICAL_VISUALIZATION",
                "SCENE_FILTER",
                "BUILDER_ASSIST",
                "WORKFLOW",
                "INTEGRATIONS");
        assertContainsNone(controller, "ANALYZER", "INSPECTOR", "HIGHLIGHT,", "VISIBILITY");
        assertContainsAll(rows,
                "addVisualRows(",
                "addBuilderHighlightRows(",
                "addTechnicalVisualizationRows(",
                "addSceneFilterRows(",
                "addIntegrationRows(");
        assertContainsAll(assist,
                "addPlacementRows(",
                "addSchematicPlacementRows(",
                "addPatternConsistencyRows(");
        assertContainsAll(workflow, "InteractionHistory.snapshot()");
        assertFalse(exists("src/main/java/dev/chise/chisetweaks/gui/InspectorSettingsRows.java"));

        assertContainsAll(definition,
                "\"Fine Line / Tripwire\"",
                "\"Occluded Blocks\"",
                "\"Lava Source\"",
                "\"Villager Job Site Links\"");
        assertContainsNone(definition,
                "\"Hidden Block Analyzer\"",
                "\"Lava Analyzer\"",
                "\"Villager Analyzer\"");
    }
}
