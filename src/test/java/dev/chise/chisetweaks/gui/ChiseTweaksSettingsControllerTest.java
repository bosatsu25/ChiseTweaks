package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.ChiseBooleanSetting;
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
    void japaneseAndEnglishUseTheSameStableMainStructure() {
        var japanese = new ChiseTweaksSettingsController(true);
        var english = new ChiseTweaksSettingsController(false);
        List<ChiseTweaksSettingRowDefinition> japaneseRows = japanese.rows();
        List<ChiseTweaksSettingRowDefinition> englishRows = english.rows();

        assertEquals(ids(englishRows), ids(japaneseRows));
        assertEquals(kinds(englishRows), kinds(japaneseRows));
        assertRowContracts(japaneseRows);
        assertRowContracts(englishRows);
    }

    @Test
    void mainSurfaceKeepsThreeCompactGroupsAndAnalyzerToggles() {
        var controller = new ChiseTweaksSettingsController(true);
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
                "header.visibilityImprovement",
                "lava",
                "ancientDebrisAnalyzer",
                "fireVisibility"), ids(rows));

        assertEquals(List.of("ハイライト", "Visual Filter", "視認改善"),
                rows.stream()
                        .filter(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER)
                        .map(ChiseTweaksSettingRowDefinition::name)
                        .toList());
        assertFalse(rows.stream().anyMatch(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.INTEGER));
        assertRowContracts(rows);
    }

    @Test
    void mainFeatureNamesUseTheFinalUserFacingTerminology() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        assertEquals("鉱石ハイライト", row(rows, "materials").name());
        assertEquals("ネザーハイライト", row(rows, "nether").name());
        assertEquals("細線ハイライト", row(rows, "thread").name());
        assertEquals("隠れブロックハイライト", row(rows, "hidden").name());
        assertEquals("ガラスハイライト", row(rows, "glass").name());
        assertEquals("昆布ハイライト", row(rows, "kelp").name());
        assertEquals("ブロックフィルター", row(rows, "focusBlocks").name());
        assertEquals("エンティティフィルター", row(rows, "focusEntities").name());
        assertEquals("溶岩源ハイライト", row(rows, "lava").name());
        assertEquals("古代の残骸アナライザー", row(rows, "ancientDebrisAnalyzer").name());
        assertEquals("火炎表示を低くする", row(rows, "fireVisibility").name());
    }

    @Test
    void settingsActionsAreAttachedToGroupHeadersInsteadOfCrowdingAnalyzerRows() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        ChiseTweaksSettingRowDefinition highlightHeader = row(rows, "header.highlight");
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS, highlightHeader.action());
        assertEquals("設定", highlightHeader.actionLabel());

        ChiseTweaksSettingRowDefinition visibilityHeader = row(rows, "header.visibilityImprovement");
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS, visibilityHeader.action());
        assertEquals("設定", visibilityHeader.actionLabel());

        ChiseTweaksSettingRowDefinition blocks = row(rows, "focusBlocks");
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN_ACTION, blocks.kind());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER, blocks.action());

        ChiseTweaksSettingRowDefinition entities = row(rows, "focusEntities");
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN_ACTION, entities.kind());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER, entities.action());

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, row(rows, "lava").kind());
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, row(rows, "ancientDebrisAnalyzer").kind());
    }

    @Test
    void ancientDebrisAnalyzerIsOptInAndUsesIndependentBoundedSettings() {
        var controller = new ChiseTweaksSettingsController(true);
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
        var controller = new ChiseTweaksSettingsController(true);
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
        assertTrue(rows.stream().anyMatch(row -> row.id().startsWith("visualTargetHidden")));
    }

    @Test
    void analyzerDetailSurfaceKeepsDynamicLavaAndStaticDebrisSettingsSeparate() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS);

        assertEquals("アナライザー設定", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.LAVA_DETAILS));
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
    void prereleaseBulkEnablesOnlyReleasedHighlights() {
        List<ChiseBooleanSetting> highlights = highlightFeatures();
        try {
            for (ChiseBooleanSetting highlight : highlights) highlight.setBooleanValueSilently(false);
            var controller = new ChiseTweaksSettingsController(true);

            controller.toggleHighlightBulk();

            assertTrue(FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue());
            assertTrue(FeatureSwitches.KELP_HIGHLIGHT.getBooleanValue());
            assertTrue(FeatureSwitches.GLASS_INSPECTION.getBooleanValue());
            assertFalse(FeatureSwitches.NETHER_PALETTE.getBooleanValue());
            assertFalse(FeatureSwitches.FINE_THREAD_TRACE.getBooleanValue());
            assertFalse(FeatureSwitches.HIDDEN_SURFACE_TRACE.getBooleanValue());
            assertFalse(controller.shouldTurnHighlightBulkOn());
        } finally {
            for (ChiseBooleanSetting highlight : highlights) highlight.setBooleanValueSilently(false);
        }
    }

    @Test
    void lavaSourceHighlightIsOptInAndSharesTheAnalyzerSettingsSurface() {
        var controller = new ChiseTweaksSettingsController(true);
        ChiseTweaksSettingRowDefinition lava = row(controller.rows(), "lava");
        ChiseTweaksSettingRowDefinition header = row(controller.rows(), "header.visibilityImprovement");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, lava.kind());
        assertSame(LocalFeatureSwitches.LAVA_HIGHLIGHT, lava.booleanConfig());
        assertFalse(lava.booleanConfig().getDefaultBooleanValue());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS, header.action());
    }

    @Test
    void fireVisibilityRemainsOptIn() {
        var controller = new ChiseTweaksSettingsController(true);
        ChiseTweaksSettingRowDefinition fire = row(controller.rows(), "fireVisibility");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, fire.kind());
        assertSame(LocalFeatureSwitches.FIRE_VISIBILITY, fire.booleanConfig());
        assertFalse(fire.booleanConfig().getDefaultBooleanValue());
        assertFalse(fire.booleanConfig().getBooleanValue());
    }

    @Test
    void kelpHighlightIsOptInAndUsesTheSharedHighlightSurface() {
        var controller = new ChiseTweaksSettingsController(true);
        ChiseTweaksSettingRowDefinition kelp = row(controller.rows(), "kelp");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, kelp.kind());
        assertSame(FeatureSwitches.KELP_HIGHLIGHT, kelp.booleanConfig());
        assertFalse(kelp.booleanConfig().getDefaultBooleanValue());
        assertFalse(kelp.booleanConfig().getBooleanValue());
    }

    @Test
    void glassHighlightIsOptInAndUsesTheSharedHighlightSurface() {
        var controller = new ChiseTweaksSettingsController(true);
        ChiseTweaksSettingRowDefinition glass = row(controller.rows(), "glass");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, glass.kind());
        assertSame(FeatureSwitches.GLASS_INSPECTION, glass.booleanConfig());
        assertFalse(glass.booleanConfig().getDefaultBooleanValue());
        assertFalse(glass.booleanConfig().getBooleanValue());
    }

    @Test
    void rowIdsAreUniqueAndRemovedFeaturesDoNotReturnOnAnySurface() {
        var controller = new ChiseTweaksSettingsController(false);
        Set<String> removedTokens = Set.of("pumpkin", "placement", "lavaSourceColor", "sodium");
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

    private static List<ChiseBooleanSetting> highlightFeatures() {
        return List.of(
                FeatureSwitches.MATERIAL_HIGHLIGHTS,
                FeatureSwitches.NETHER_PALETTE,
                FeatureSwitches.FINE_THREAD_TRACE,
                FeatureSwitches.HIDDEN_SURFACE_TRACE,
                FeatureSwitches.GLASS_INSPECTION,
                FeatureSwitches.KELP_HIGHLIGHT);
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
                }
                case BOOLEAN_ACTION -> {
                    assertNotNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    assertNotNull(row.action());
                    assertFalse(row.actionLabel().isBlank());
                }
                case INTEGER -> {
                    assertNull(row.booleanConfig());
                    assertNotNull(row.integerConfig());
                    assertTrue(row.step() >= 1);
                    assertNull(row.action());
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
