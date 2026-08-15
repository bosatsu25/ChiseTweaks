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
    void japaneseAndEnglishUseTheSameStableRowStructure() {
        var japanese = new ChiseTweaksSettingsController(true);
        var english = new ChiseTweaksSettingsController(false);
        List<ChiseTweaksSettingRowDefinition> japaneseRows = japanese.rows();
        List<ChiseTweaksSettingRowDefinition> englishRows = english.rows();

        assertEquals(ids(englishRows), ids(japaneseRows));
        assertEquals(kinds(englishRows), kinds(japaneseRows));
        assertFalse(japaneseRows.isEmpty());
        assertRowContracts(japaneseRows);
        assertRowContracts(englishRows);
    }

    @Test
    void settingsUseExactlyThreeUserFacingGroupsInStableOrder() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> headers = controller.rows().stream()
                .filter(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER)
                .toList();

        assertEquals(List.of(
                "header.highlight",
                "header.visualFilter",
                "header.visibilityImprovement"), ids(headers));
        assertEquals(List.of("ハイライト", "Visual Filter", "視認改善"),
                headers.stream().map(ChiseTweaksSettingRowDefinition::name).toList());
    }

    @Test
    void primaryFeatureNamesUseSimpleUserFacingHighlightAndFilterTerms() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        assertEquals("鉱石ハイライト", row(rows, "materials").name());
        assertEquals("ネザーハイライト", row(rows, "nether").name());
        assertEquals("細線ハイライト", row(rows, "thread").name());
        assertEquals("隠れブロックハイライト", row(rows, "hidden").name());
        assertEquals("ガラスハイライト", row(rows, "glass").name());
        assertEquals("ブロックフィルター", row(rows, "focusBlocks").name());
        assertEquals("エンティティフィルター", row(rows, "focusEntities").name());
        assertEquals("溶岩解析", row(rows, "lava").name());
        assertEquals("火炎表示を低くする", row(rows, "fireVisibility").name());
    }

    @Test
    void lavaAnalysisBelongsToVisibilityImprovementInsteadOfHighlight() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();
        int highlight = indexOf(rows, "header.highlight");
        int filter = indexOf(rows, "header.visualFilter");
        int visibility = indexOf(rows, "header.visibilityImprovement");
        int lava = indexOf(rows, "lava");

        assertTrue(highlight < filter);
        assertTrue(filter < visibility);
        assertTrue(visibility < lava);
    }

    @Test
    void highlightBulkStartsAsAllOnActionAndOnlyTouchesFiveHighlightFeatures() {
        List<ChiseBooleanSetting> highlights = highlightFeatures();
        boolean[] oldHighlightValues = highlights.stream()
                .mapToInt(value -> value.getBooleanValue() ? 1 : 0)
                .mapToObj(value -> value == 1)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        values -> {
                            boolean[] result = new boolean[values.size()];
                            for (int index = 0; index < values.size(); index++) result[index] = values.get(index);
                            return result;
                        }));
        boolean oldBlocks = FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue();
        boolean oldEntities = FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue();
        boolean oldLava = LocalFeatureSwitches.LAVA_HIGHLIGHT.getBooleanValue();
        boolean oldFire = LocalFeatureSwitches.FIRE_VISIBILITY.getBooleanValue();

        try {
            for (ChiseBooleanSetting highlight : highlights) highlight.setBooleanValueSilently(false);
            FeatureSwitches.BUILDER_FOCUS_BLOCKS.setBooleanValueSilently(true);
            FeatureSwitches.BUILDER_FOCUS_ENTITIES.setBooleanValueSilently(false);
            LocalFeatureSwitches.LAVA_HIGHLIGHT.setBooleanValueSilently(true);
            LocalFeatureSwitches.FIRE_VISIBILITY.setBooleanValueSilently(false);

            var controller = new ChiseTweaksSettingsController(true);
            assertTrue(controller.shouldTurnHighlightBulkOn());

            controller.toggleHighlightBulk();
            assertTrue(highlights.stream().allMatch(ChiseBooleanSetting::getBooleanValue));
            assertFalse(controller.shouldTurnHighlightBulkOn());
            assertTrue(FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue());
            assertFalse(FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue());
            assertTrue(LocalFeatureSwitches.LAVA_HIGHLIGHT.getBooleanValue());
            assertFalse(LocalFeatureSwitches.FIRE_VISIBILITY.getBooleanValue());

            highlights.getFirst().setBooleanValueSilently(false);
            assertTrue(controller.shouldTurnHighlightBulkOn());
            controller.toggleHighlightBulk();
            assertTrue(highlights.stream().allMatch(ChiseBooleanSetting::getBooleanValue));

            controller.toggleHighlightBulk();
            assertTrue(highlights.stream().noneMatch(ChiseBooleanSetting::getBooleanValue));
            assertTrue(controller.shouldTurnHighlightBulkOn());
        } finally {
            for (int index = 0; index < highlights.size(); index++) {
                highlights.get(index).setBooleanValueSilently(oldHighlightValues[index]);
            }
            FeatureSwitches.BUILDER_FOCUS_BLOCKS.setBooleanValueSilently(oldBlocks);
            FeatureSwitches.BUILDER_FOCUS_ENTITIES.setBooleanValueSilently(oldEntities);
            LocalFeatureSwitches.LAVA_HIGHLIGHT.setBooleanValueSilently(oldLava);
            LocalFeatureSwitches.FIRE_VISIBILITY.setBooleanValueSilently(oldFire);
        }
    }

    @Test
    void highlightDetailsKeepReducedMotionAndModdedOreEditorAvailable() {
        var controller = new ChiseTweaksSettingsController(true);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();

        ChiseTweaksSettingRowDefinition motion = row(rows, "oreMotion");
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, motion.kind());
        assertSame(LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION, motion.booleanConfig());
        assertFalse(motion.booleanConfig().getDefaultBooleanValue());

        ChiseTweaksSettingRowDefinition moddedOre = row(rows, "moddedOreTargets");
        assertEquals(ChiseTweaksSettingRowDefinition.Kind.ACTION, moddedOre.kind());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT, moddedOre.action());
    }

    @Test
    void fireVisibilityRemainsOptIn() {
        var controller = new ChiseTweaksSettingsController(true);
        ChiseTweaksSettingRowDefinition fire = row(controller.rows(), "fireVisibility");

        assertEquals(ChiseTweaksSettingRowDefinition.Kind.BOOLEAN, fire.kind());
        assertSame(LocalFeatureSwitches.FIRE_VISIBILITY, fire.booleanConfig());
        assertFalse(fire.booleanConfig().getDefaultBooleanValue());
        assertTrue(fire.description().contains("ワールド上の炎は変更しない"));
    }

    @Test
    void rowIdsAreUniqueAndRemovedFeaturesDoNotReturn() {
        var controller = new ChiseTweaksSettingsController(false);
        Set<String> removedTokens = Set.of("pumpkin", "placement", "lavaSourceColor", "sodium");
        Set<String> unique = new HashSet<>();
        for (ChiseTweaksSettingRowDefinition row : controller.rows()) {
            assertTrue(unique.add(row.id()), "duplicate row id: " + row.id());
            String normalized = row.id().toLowerCase();
            for (String removed : removedTokens) {
                assertFalse(normalized.contains(removed.toLowerCase()),
                        "removed feature leaked into settings row: " + row.id());
            }
        }
    }

    @Test
    void actionRowsRemainPairedWithTheirEditors() {
        var controller = new ChiseTweaksSettingsController(false);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rows();
        assertTrue(rows.stream().anyMatch(row ->
                row.action() == ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertTrue(rows.stream().anyMatch(row ->
                row.action() == ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER));
        assertTrue(rows.stream().anyMatch(row ->
                row.action() == ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT));
    }

    private static List<ChiseBooleanSetting> highlightFeatures() {
        return List.of(
                FeatureSwitches.MATERIAL_HIGHLIGHTS,
                FeatureSwitches.NETHER_PALETTE,
                FeatureSwitches.FINE_THREAD_TRACE,
                FeatureSwitches.HIDDEN_SURFACE_TRACE,
                FeatureSwitches.GLASS_INSPECTION);
    }

    private static int indexOf(List<ChiseTweaksSettingRowDefinition> rows, String id) {
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index).id().equals(id)) return index;
        }
        return -1;
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
            switch (row.kind()) {
                case HEADER -> {
                    assertEquals("", row.description());
                    assertNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    assertNull(row.action());
                }
                case BOOLEAN -> {
                    assertFalse(row.description().isBlank());
                    assertNotNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    assertNull(row.action());
                }
                case INTEGER -> {
                    assertFalse(row.description().isBlank());
                    assertNull(row.booleanConfig());
                    assertNotNull(row.integerConfig());
                    assertTrue(row.step() >= 1);
                    assertNull(row.action());
                }
                case ACTION -> {
                    assertFalse(row.description().isBlank());
                    assertNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    assertNotNull(row.action());
                    assertNotNull(row.actionLabel());
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
