package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChestVisibilitySetting;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.config.WhiteConcreteVisibilitySetting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsControllerTest {
    @Test
    void repeatedControllersUseTheSameStableMainStructure() {
        var first = new ChiseTweaksSettingsController();
        var second = new ChiseTweaksSettingsController();
        assertEquals(ids(first.rows()), ids(second.rows()));
        assertEquals(kinds(first.rows()), kinds(second.rows()));
        assertRowContracts(first.rows());
        assertRowContracts(second.rows());
    }

    @Test
    void mainSurfaceExposesAllReleasedVisualGroupsAndDiagnostics() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();
        assertEquals(List.of(
                "header.highlight", "materials", "nether", "thread", "hidden", "glass", "kelp",
                "header.visualFilter", "focusBlocks", "focusEntities",
                "header.analyzer", "lava", "ancientDebrisAnalyzer",
                "header.visibilityImprovement", "fireVisibility", "chestVisibility",
                "whiteConcreteVisibility", "header.diagnostics", "copyDiagnostics", "exportDiagnostics"), ids(rows));
        assertEquals(List.of(
                        "header.highlight", "header.visualFilter", "header.analyzer",
                        "header.visibilityImprovement", "header.diagnostics"),
                rows.stream()
                        .filter(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER)
                        .map(ChiseTweaksSettingRowDefinition::id)
                        .toList());
        assertFalse(rows.stream().anyMatch(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.INTEGER));
        assertRowContracts(rows);
    }

    @Test
    void mainRowsRemainBoundToExpectedSettings() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();
        assertSame(FeatureSwitches.MATERIAL_HIGHLIGHTS, row(rows, "materials").booleanConfig());
        assertSame(FeatureSwitches.NETHER_PALETTE, row(rows, "nether").booleanConfig());
        assertSame(FeatureSwitches.FINE_THREAD_TRACE, row(rows, "thread").booleanConfig());
        assertSame(FeatureSwitches.HIDDEN_SURFACE_TRACE, row(rows, "hidden").booleanConfig());
        assertSame(FeatureSwitches.GLASS_INSPECTION, row(rows, "glass").booleanConfig());
        assertSame(FeatureSwitches.KELP_HIGHLIGHT, row(rows, "kelp").booleanConfig());
        assertSame(FeatureSwitches.BUILDER_FOCUS_BLOCKS, row(rows, "focusBlocks").booleanConfig());
        assertSame(FeatureSwitches.BUILDER_FOCUS_ENTITIES, row(rows, "focusEntities").booleanConfig());
        assertSame(LocalFeatureSwitches.LAVA_HIGHLIGHT, row(rows, "lava").booleanConfig());
        assertSame(LocalFeatureSwitches.ANCIENT_DEBRIS_ANALYZER,
                row(rows, "ancientDebrisAnalyzer").booleanConfig());
        assertSame(LocalFeatureSwitches.FIRE_VISIBILITY, row(rows, "fireVisibility").booleanConfig());
        assertSame(ChestVisibilitySetting.INSTANCE, row(rows, "chestVisibility").booleanConfig());
        assertSame(WhiteConcreteVisibilitySetting.INSTANCE,
                row(rows, "whiteConcreteVisibility").booleanConfig());
        assertTrue(row(rows, "chestVisibility").booleanConfig().getDefaultBooleanValue());
        assertTrue(row(rows, "whiteConcreteVisibility").booleanConfig().getDefaultBooleanValue());
    }

    @Test
    void sectionAndDiagnosticActionsAreBoundToExpectedCommands() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS,
                row(rows, "header.highlight").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_VISUAL_FILTER_DETAILS,
                row(rows, "header.visualFilter").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS,
                row(rows, "header.analyzer").action());
        assertNull(row(rows, "header.visibilityImprovement").action());
        assertNull(row(rows, "header.diagnostics").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.COPY_DIAGNOSTICS,
                row(rows, "copyDiagnostics").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EXPORT_DIAGNOSTICS,
                row(rows, "exportDiagnostics").action());
    }

    @Test
    void visualFilterDetailSurfaceOwnsBothEditorsAndRendererRefreshSetting() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS);
        assertFalse(controller.surfaceTitle(
                ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS).isBlank());
        assertSame(BuilderFocusConfig.REFRESH_RENDERER, row(rows, "refreshRenderer").booleanConfig());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER,
                row(rows, "editBlockFilter").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER,
                row(rows, "editEntityFilter").action());
    }

    @Test
    void highlightDetailSurfaceNoLongerExposesMutualExclusion() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS);
        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY,
                row(rows, "highlightWorldOverlay").booleanConfig());
        assertFalse(rows.stream().anyMatch(candidate -> "highlightExclusiveMode".equals(candidate.id())));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetMaterial")));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetTechnical")));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetHidden")));
    }

    @Test
    void analyzerDetailSurfaceKeepsLavaAndAncientDebrisSettingsSeparate() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS);
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                row(rows, "lavaRange").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                row(rows, "lavaInterval").integerConfig());
        assertSame(LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_RANGE,
                row(rows, "ancientDebrisRange").integerConfig());
        assertSame(LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS,
                row(rows, "ancientDebrisMaxMarkers").integerConfig());
    }

    @Test
    void visibilitySectionContainsLowFireChestAndWhiteConcrete() {
        var controller = new ChiseTweaksSettingsController();
        ChiseTweaksSettingRowDefinition fire = row(controller.rows(), "fireVisibility");
        ChiseTweaksSettingRowDefinition chest = row(controller.rows(), "chestVisibility");
        ChiseTweaksSettingRowDefinition concrete = row(controller.rows(), "whiteConcreteVisibility");
        assertSame(LocalFeatureSwitches.FIRE_VISIBILITY, fire.booleanConfig());
        assertFalse(fire.booleanConfig().getDefaultBooleanValue());
        assertSame(ChestVisibilitySetting.INSTANCE, chest.booleanConfig());
        assertTrue(chest.booleanConfig().getDefaultBooleanValue());
        assertSame(WhiteConcreteVisibilitySetting.INSTANCE, concrete.booleanConfig());
        assertTrue(concrete.booleanConfig().getDefaultBooleanValue());
    }

    @Test
    void rowIdsAreUniqueAndRemovedFeaturesDoNotReturnOnAnySurface() {
        var controller = new ChiseTweaksSettingsController();
        Set<String> removedTokens = Set.of("pumpkin", "placement", "lavaSourceColor", "sodium", "bulk");
        for (ChiseTweaksSettingsController.Surface surface : ChiseTweaksSettingsController.Surface.values()) {
            Set<String> unique = new HashSet<>();
            for (ChiseTweaksSettingRowDefinition row : controller.rows(surface)) {
                assertTrue(unique.add(row.id()), "duplicate row id: " + row.id());
                String normalized = row.id().toLowerCase();
                for (String removed : removedTokens) {
                    assertFalse(normalized.contains(removed.toLowerCase()),
                            "removed feature leaked into settings row: " + row.id());
                }
            }
        }
    }

    private static ChiseTweaksSettingRowDefinition row(
            List<ChiseTweaksSettingRowDefinition> rows, String id) {
        return rows.stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing row: " + id));
    }

    private static void assertRowContracts(List<ChiseTweaksSettingRowDefinition> rows) {
        for (ChiseTweaksSettingRowDefinition row : rows) {
            assertNotNull(row.id());
            assertFalse(row.id().isBlank());
            assertNotNull(row.name());
            assertFalse(row.name().isBlank());
            assertNotNull(row.description());
            switch (row.kind()) {
                case HEADER -> {
                    assertNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    if (row.action() == null) assertTrue(row.actionLabel().isEmpty());
                    else assertFalse(row.actionLabel().isBlank());
                }
                case BOOLEAN -> {
                    assertNotNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    assertNull(row.action());
                    assertTrue(row.actionLabel().isEmpty());
                }
                case INTEGER -> {
                    assertNull(row.booleanConfig());
                    assertNotNull(row.integerConfig());
                    assertTrue(row.step() >= 1);
                    assertNull(row.action());
                    assertTrue(row.actionLabel().isEmpty());
                }
                case ACTION -> {
                    assertNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    assertNotNull(row.action());
                    assertFalse(row.actionLabel().isBlank());
                }
            }
        }
    }

    private static List<String> ids(List<ChiseTweaksSettingRowDefinition> rows) {
        return rows.stream().map(ChiseTweaksSettingRowDefinition::id).toList();
    }

    private static List<ChiseTweaksSettingRowDefinition.Kind> kinds(
            List<ChiseTweaksSettingRowDefinition> rows) {
        return rows.stream().map(ChiseTweaksSettingRowDefinition::kind).toList();
    }
}
