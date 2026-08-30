package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.MasaIntegrationSettings;
import dev.chise.chisetweaks.config.SettingPersistence;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsControllerTest {
    @Test
    void sevenProductGroupsReplaceLegacyAnalyzerAndInspectorSurfaces() {
        assertEquals(List.of(
                ChiseTweaksSettingsController.Surface.VISUAL,
                ChiseTweaksSettingsController.Surface.BUILDER_HIGHLIGHTS,
                ChiseTweaksSettingsController.Surface.TECHNICAL_VISUALIZATION,
                ChiseTweaksSettingsController.Surface.SCENE_FILTER,
                ChiseTweaksSettingsController.Surface.BUILDER_ASSIST,
                ChiseTweaksSettingsController.Surface.WORKFLOW,
                ChiseTweaksSettingsController.Surface.INTEGRATIONS),
                List.of(ChiseTweaksSettingsController.Surface.values()));
    }

    @Test
    void visualGroupOwnsFirstPersonAndBrightTweaks() {
        var rows = rows(ChiseTweaksSettingsController.Surface.VISUAL);
        assertSame(FeatureSwitches.FIRE_VISIBILITY, row(rows, "fireVisibility").booleanConfig());
        assertSame(LocalFeatureSettings.FIRE_VISIBILITY_SIZE, row(rows, "fireVisibilitySize").integerConfig());
        assertSame(FeatureSwitches.HANDHELD_SIZE, row(rows, "handheldSize").booleanConfig());
        assertSame(LocalFeatureSettings.HANDHELD_BLOCK_SCALE, row(rows, "handheldBlockScale").integerConfig());
        assertSame(FeatureSwitches.BRIGHT_CHEST, row(rows, "chestVisibility").booleanConfig());
        assertSame(FeatureSwitches.BRIGHT_CONCRETE, row(rows, "whiteConcreteVisibility").booleanConfig());
    }

    @Test
    void builderHighlightsOwnVisibleAndOccludedTargetsWithOneOccludedBudget() {
        var rows = rows(ChiseTweaksSettingsController.Surface.BUILDER_HIGHLIGHTS);
        assertSame(FeatureSwitches.MATERIAL_HIGHLIGHTS, row(rows, "materials").booleanConfig());
        assertSame(FeatureSwitches.NETHER_PALETTE, row(rows, "nether").booleanConfig());
        assertSame(FeatureSwitches.GLASS_INSPECTION, row(rows, "glass").booleanConfig());
        assertSame(FeatureSwitches.KELP_HIGHLIGHT, row(rows, "kelp").booleanConfig());
        assertSame(FeatureSwitches.LAVA_HIGHLIGHT, row(rows, "lava").booleanConfig());
        assertSame(FeatureSwitches.HIDDEN_SURFACE_TRACE, row(rows, "hidden").booleanConfig());
        assertSame(LocalFeatureSettings.OCCLUDED_HIGHLIGHT_HORIZONTAL_RADIUS,
                row(rows, "occludedRange").integerConfig());
        assertSame(LocalFeatureSettings.OCCLUDED_HIGHLIGHT_VERTICAL_RADIUS,
                row(rows, "occludedVerticalRange").integerConfig());
        assertSame(LocalFeatureSettings.OCCLUDED_HIGHLIGHT_INTERVAL,
                row(rows, "occludedInterval").integerConfig());
        assertSame(LocalFeatureSettings.OCCLUDED_HIGHLIGHT_MAX_OVERLAYS,
                row(rows, "occludedMaxOverlays").integerConfig());
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetMaterial")));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetHidden")));
    }

    @Test
    void technicalVisualizationOwnsTracesRangesAndVillagerLinks() {
        var rows = rows(ChiseTweaksSettingsController.Surface.TECHNICAL_VISUALIZATION);
        assertSame(FeatureSwitches.FINE_THREAD_TRACE, row(rows, "thread").booleanConfig());
        assertSame(FeatureSwitches.BEACON_RANGE, row(rows, "beaconRange").booleanConfig());
        assertSame(FeatureSwitches.LIGHTNING_ROD_RANGE, row(rows, "lightningRodRange").booleanConfig());
        assertSame(FeatureSwitches.VILLAGER_ANALYZER, row(rows, "villagerLinks").booleanConfig());
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetTechnical")));
    }

    @Test
    void sceneFilterOwnsBothFiltersAndEditors() {
        var rows = rows(ChiseTweaksSettingsController.Surface.SCENE_FILTER);
        assertSame(FeatureSwitches.BUILDER_FOCUS_BLOCKS, row(rows, "focusBlocks").booleanConfig());
        assertSame(FeatureSwitches.BUILDER_FOCUS_ENTITIES, row(rows, "focusEntities").booleanConfig());
        assertSame(BuilderFocusConfig.REFRESH_RENDERER, row(rows, "refreshRenderer").booleanConfig());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER, row(rows, "editBlockFilter").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER, row(rows, "editEntityFilter").action());
    }

    @Test
    void builderAssistDropsDeveloperDiagnosticsButKeepsPlacementAndPattern() {
        var rows = rows(ChiseTweaksSettingsController.Surface.BUILDER_ASSIST);
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().equals("builderAssist.blockInfo")));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().equals("placement.title")));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().equals("pattern.title")));
        assertFalse(rows.stream().anyMatch(candidate -> candidate.id().equals("inspector.filter")));
        assertFalse(rows.stream().anyMatch(candidate -> candidate.id().equals("inspector.features")));
        assertFalse(rows.stream().anyMatch(candidate -> candidate.id().equals("inspector.renderMode")));
    }

    @Test
    void workflowOwnsInteractionHistory() {
        var rows = rows(ChiseTweaksSettingsController.Surface.WORKFLOW);
        assertSame(LocalFeatureSettings.INTERACTION_HISTORY,
                row(rows, "interactionHistory").booleanConfig());
    }

    @Test
    void integrationsOwnLitematicaPlacementAssistAndExistingGuards() {
        var rows = rows(ChiseTweaksSettingsController.Surface.INTEGRATIONS);
        assertSame(LocalFeatureSettings.SCHEMATIC_PLACEMENT_INSPECTOR,
                row(rows, "schematicPlacementInspector").booleanConfig());
        assertSame(MasaIntegrationSettings.LITEMATICA_PICK_REDIRECT,
                row(rows, "litematicaPickRedirect").booleanConfig());
        assertSame(MasaIntegrationSettings.TWEAKEROO_TOOL_SWITCH_GUARD,
                row(rows, "tweakerooToolSwitchGuard").booleanConfig());
        assertSame(MasaIntegrationSettings.TWEAKERMORE_AUTO_PICK_GUARD,
                row(rows, "tweakermoreAutoPickGuard").booleanConfig());
    }

    @Test
    void rowIdsRemainUniqueAcrossEverySurface() {
        var controller = new ChiseTweaksSettingsController();
        for (var surface : ChiseTweaksSettingsController.Surface.values()) {
            Set<String> unique = new HashSet<>();
            for (var candidate : controller.rows(surface)) {
                assertTrue(unique.add(candidate.id()), "duplicate row id: " + candidate.id());
            }
        }
    }

    @Test
    void pendingPersistenceStateRemainsControllerOwned() {
        var controller = new ChiseTweaksSettingsController();
        assertFalse(controller.hasPendingChanges());
        controller.markDirty(SettingPersistence.EXTERNAL);
        assertFalse(controller.hasPendingChanges());
        controller.markDirty(Set.of(SettingPersistence.FEATURE_CONFIG, SettingPersistence.LOCAL_CONFIG));
        assertTrue(controller.hasPendingChanges());
        assertEquals(Set.of(SettingPersistence.FEATURE_CONFIG, SettingPersistence.LOCAL_CONFIG),
                controller.pendingDomainsForDiagnostics());
    }

    private static List<ChiseTweaksSettingRowDefinition> rows(
            ChiseTweaksSettingsController.Surface surface) {
        return new ChiseTweaksSettingsController().rows(surface);
    }

    private static ChiseTweaksSettingRowDefinition row(
            List<ChiseTweaksSettingRowDefinition> rows, String id) {
        return rows.stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing row: " + id));
    }
}
