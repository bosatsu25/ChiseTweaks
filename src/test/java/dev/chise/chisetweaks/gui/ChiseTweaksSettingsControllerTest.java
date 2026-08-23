package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
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
    void mainSurfaceKeepsSemanticGroupsAndAnalyzerTogglesCompact() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        assertEquals(List.of(
                "header.highlight",
                "materials",
                "nether",
                "thread",
                "hidden",
                "glass",
                "kelp",
                "header.visualFilter",
                "focusBlocks",
                "focusEntities",
                "header.analyzer",
                "lava",
                "ancientDebrisAnalyzer",
                "header.visibilityImprovement",
                "fireVisibility"), ids(rows));

        assertEquals(List.of(
                        "header.highlight",
                        "header.visualFilter",
                        "header.analyzer",
                        "header.visibilityImprovement"),
                rows.stream()
                        .filter(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER)
                        .map(ChiseTweaksSettingRowDefinition::id)
                        .toList());
        assertFalse(rows.stream().anyMatch(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.INTEGER));
        assertRowContracts(rows);
    }

    @Test
    void mainRowsRemainBoundToTheExpectedFeatureSettings() {
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
    }

    @Test
    void mainSectionSettingsActionsAreConsolidatedAtHeaders() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        ChiseTweaksSettingRowDefinition highlightHeader = row(rows, "header.highlight");
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS, highlightHeader.action());
        assertFalse(highlightHeader.actionLabel().isBlank());

        ChiseTweaksSettingRowDefinition visualFilterHeader = row(rows, "header.visualFilter");
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_VISUAL_FILTER_DETAILS,
                visualFilterHeader.action());
        assertFalse(visualFilterHeader.actionLabel().isBlank());

        ChiseTweaksSettingRowDefinition analyzerHeader = row(rows, "header.analyzer");
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS, analyzerHeader.action());
        assertFalse(analyzerHeader.actionLabel().isBlank());

        ChiseTweaksSettingRowDefinition visibilityHeader = row(rows, "header.visibilityImprovement");
        assertNull(visibilityHeader.action());
        assertEquals("", visibilityHeader.actionLabel());

        for (String id : List.of("focusBlocks", "focusEntities")) {
            ChiseTweaksSettingRowDefinition filter = row(rows, id);
            assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, filter.kind());
            assertNull(filter.action());
            assertTrue(filter.actionLabel().isEmpty());
        }
        assertFalse(rows.stream().anyMatch(row -> row.id().toLowerCase().contains("bulk")));
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
        assertEquals(2, rows.stream()
                .filter(candidate -> candidate.kind() == ChiseTweaksSettingRowDefinition.Kind.ACTION)
                .count());
    }

    @Test
    void visualFilterDetailResetRestoresBuilderFocusDefaults() {
        boolean originalRefresh = BuilderFocusConfig.REFRESH_RENDERER.getBooleanValue();
        try {
            BuilderFocusConfig.REFRESH_RENDERER.setBooleanValueSilently(false);
            var controller = new ChiseTweaksSettingsController();
            assertTrue(controller.reset(ChiseTweaksSettingsController.Surface.VISUAL_FILTER_DETAILS));
            assertEquals(BuilderFocusConfig.REFRESH_RENDERER.getDefaultBooleanValue(),
                    BuilderFocusConfig.REFRESH_RENDERER.getBooleanValue());
        } finally {
            BuilderFocusConfig.REFRESH_RENDERER.setBooleanValueSilently(originalRefresh);
        }
    }

    @Test
    void ancientDebrisAnalyzerIsOptInAndUsesIndependentBoundedSettings() {
        var controller = new ChiseTweaksSettingsController();
        ChiseTweaksSettingRowDefinition analyzer = row(controller.rows(), "ancientDebrisAnalyzer");
        List<ChiseTweaksSettingRowDefinition> details = controller.rows(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS);

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, analyzer.kind());
        assertSame(LocalFeatureSwitches.ANCIENT_DEBRIS_ANALYZER, analyzer.booleanConfig());
        assertFalse(analyzer.booleanConfig().getDefaultBooleanValue());
        assertSame(LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_RANGE,
                row(details, "ancientDebrisRange").integerConfig());
        assertEquals(64, row(details, "ancientDebrisRange").integerConfig().getDefaultIntegerValue());
        assertSame(LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS,
                row(details, "ancientDebrisMaxMarkers").integerConfig());
        assertEquals(64, row(details, "ancientDebrisMaxMarkers").integerConfig().getDefaultIntegerValue());
    }

    @Test
    void highlightDetailSurfaceKeepsPreviouslyReachableAdvancedSettings() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS);

        assertSame(LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION, row(rows, "oreMotion").booleanConfig());
        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS,
                row(rows, "highlightRange").integerConfig());
        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS,
                row(rows, "highlightVerticalRange").integerConfig());
        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL,
                row(rows, "highlightInterval").integerConfig());
        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS,
                row(rows, "highlightMaxOverlays").integerConfig());
        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY,
                row(rows, "highlightWorldOverlay").booleanConfig());
        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_EXCLUSIVE_MODE,
                row(rows, "highlightExclusiveMode").booleanConfig());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT,
                row(rows, "moddedOreTargets").action());

        assertTrue(rows.stream().anyMatch(row -> row.id().startsWith("visualTargetMaterial")));
        assertTrue(rows.stream().anyMatch(row -> row.id().startsWith("visualTargetTechnical")));
        assertTrue(rows.stream().anyMatch(row -> row.id().startsWith("visualTargetHidden")));
    }

    @Test
    void analyzerDetailSurfaceKeepsDynamicLavaAndStaticDebrisSettingsSeparate() {
        var controller = new ChiseTweaksSettingsController();
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS);

        assertFalse(controller.surfaceTitle(ChiseTweaksSettingsController.Surface.LAVA_DETAILS).isBlank());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                row(rows, "lavaRange").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS,
                row(rows, "lavaVerticalRange").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                row(rows, "lavaInterval").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS,
                row(rows, "lavaMaxOverlays").integerConfig());
        assertSame(LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_RANGE,
                row(rows, "ancientDebrisRange").integerConfig());
        assertSame(LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS,
                row(rows, "ancientDebrisMaxMarkers").integerConfig());

        assertFalse(rows.stream().anyMatch(row ->
                row.integerConfig() == LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS));
        assertFalse(rows.stream().anyMatch(row ->
                row.integerConfig() == LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL));
    }

    @Test
    void lavaSourceHighlightIsOptInAndSharesTheAnalyzerSettingsSurface() {
        var controller = new ChiseTweaksSettingsController();
        ChiseTweaksSettingRowDefinition lava = row(controller.rows(), "lava");
        ChiseTweaksSettingRowDefinition header = row(controller.rows(), "header.analyzer");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, lava.kind());
        assertSame(LocalFeatureSwitches.LAVA_HIGHLIGHT, lava.booleanConfig());
        assertFalse(lava.booleanConfig().getDefaultBooleanValue());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS, header.action());
    }

    @Test
    void fireVisibilityRemainsOptInAndOutsideAnalyzerSettings() {
        var controller = new ChiseTweaksSettingsController();
        ChiseTweaksSettingRowDefinition fire = row(controller.rows(), "fireVisibility");
        ChiseTweaksSettingRowDefinition visibility = row(controller.rows(), "header.visibilityImprovement");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, fire.kind());
        assertSame(LocalFeatureSwitches.FIRE_VISIBILITY, fire.booleanConfig());
        assertFalse(fire.booleanConfig().getDefaultBooleanValue());
        assertFalse(fire.booleanConfig().getBooleanValue());
        assertNull(visibility.action());
    }

    @Test
    void kelpHighlightIsOptInAndUsesTheSharedHighlightSurface() {
        var controller = new ChiseTweaksSettingsController();
        ChiseTweaksSettingRowDefinition kelp = row(controller.rows(), "kelp");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, kelp.kind());
        assertSame(FeatureSwitches.KELP_HIGHLIGHT, kelp.booleanConfig());
        assertFalse(kelp.booleanConfig().getDefaultBooleanValue());
        assertFalse(kelp.booleanConfig().getBooleanValue());
    }

    @Test
    void glassHighlightIsOptInAndUsesTheSharedHighlightSurface() {
        var controller = new ChiseTweaksSettingsController();
        ChiseTweaksSettingRowDefinition glass = row(controller.rows(), "glass");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, glass.kind());
        assertSame(FeatureSwitches.GLASS_INSPECTION, glass.booleanConfig());
        assertFalse(glass.booleanConfig().getDefaultBooleanValue());
        assertFalse(glass.booleanConfig().getBooleanValue());
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
            List<ChiseTweaksSettingRowDefinition> rows,
            String id) {
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
