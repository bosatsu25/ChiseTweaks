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
    void mainSurfaceContainsOnlyThreeGroupsAndTenPrimaryFeatures() {
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
                "fireVisibility"), ids(rows));

        assertEquals(List.of("ハイライト", "Visual Filter", "視認改善"),
                rows.stream()
                        .filter(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER)
                        .map(ChiseTweaksSettingRowDefinition::name)
                        .toList());

        assertEquals(10, rows.stream()
                .filter(row -> row.kind() != ChiseTweaksSettingRowDefinition.Kind.HEADER)
                .count());
        assertTrue(rows.stream()
                .filter(row -> row.kind() != ChiseTweaksSettingRowDefinition.Kind.HEADER)
                .allMatch(row -> row.description().isEmpty()));
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
        assertEquals("溶岩解析", row(rows, "lava").name());
        assertEquals("火炎表示を低くする", row(rows, "fireVisibility").name());
    }

    @Test
    void settingsActionsAreAttachedToTheRowsTheyConfigure() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        ChiseTweaksSettingRowDefinition highlightHeader = row(rows, "header.highlight");
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS, highlightHeader.action());
        assertEquals("設定", highlightHeader.actionLabel());

        ChiseTweaksSettingRowDefinition blocks = row(rows, "focusBlocks");
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN_ACTION, blocks.kind());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER, blocks.action());

        ChiseTweaksSettingRowDefinition entities = row(rows, "focusEntities");
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN_ACTION, entities.kind());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER, entities.action());

        ChiseTweaksSettingRowDefinition lava = row(rows, "lava");
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN_ACTION, lava.kind());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS, lava.action());
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
    void lavaDetailSurfaceUsesDedicatedSettingsInsteadOfSharedHighlightBudget() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows(
                ChiseTweaksSettingsController.Surface.LAVA_DETAILS);

        assertSame(LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                row(rows, "lavaRange").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS,
                row(rows, "lavaVerticalRange").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                row(rows, "lavaInterval").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS,
                row(rows, "lavaMaxOverlays").integerConfig());

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
            assertTrue(controller.shouldTurnHighlightBulkOn());
        } finally {
            for (ChiseBooleanSetting highlight : highlights) highlight.setBooleanValueSilently(false);
        }
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
