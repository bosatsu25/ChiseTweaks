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
    void repeatedControllersUseTheSameStableHighlightStructure() {
        var first = new ChiseTweaksSettingsController();
        var second = new ChiseTweaksSettingsController();
        assertEquals(ids(first.rows()), ids(second.rows()));
        assertEquals(kinds(first.rows()), kinds(second.rows()));
        assertRowContracts(first.rows());
        assertRowContracts(second.rows());
    }

    @Test
    void fiveTabsExposeTheCurrentReleasedInformationArchitecture() {
        var controller = new ChiseTweaksSettingsController();

        assertEquals("Highlight", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.HIGHLIGHT));
        assertEquals("Filter", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.FILTER));
        assertEquals("Inspector", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.INSPECTOR));
        assertEquals("Analyzer", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.ANALYZER));
        assertEquals("Visibility", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.VISIBILITY));

        assertTrue(ids(controller.rows(ChiseTweaksSettingsController.Surface.HIGHLIGHT)).containsAll(
                List.of("materials", "nether", "thread", "hidden", "glass", "kelp")));
        assertTrue(ids(controller.rows(ChiseTweaksSettingsController.Surface.FILTER)).containsAll(
                List.of("focusBlocks", "focusEntities", "editBlockFilter", "editEntityFilter")));
        assertEquals(List.of(
                        "inspector.title", "inspector.noTarget", "placement.title", "placement.none"),
                ids(controller.rows(ChiseTweaksSettingsController.Surface.INSPECTOR)));
        assertTrue(ids(controller.rows(ChiseTweaksSettingsController.Surface.ANALYZER)).containsAll(
                List.of("lava", "ancientDebrisAnalyzer", "lavaRange", "ancientDebrisRange")));
        assertEquals(List.of("header.visibility", "fireVisibility", "chestVisibility", "whiteConcreteVisibility"),
                ids(controller.rows(ChiseTweaksSettingsController.Surface.VISIBILITY)));
        assertTrue(controller.rows(ChiseTweaksSettingsController.Surface.INSPECTOR).stream()
                .anyMatch(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO));
    }

    @Test
    void readmeNamesAreTheCanonicalUiNames() {
        var controller = new ChiseTweaksSettingsController();
        var highlight = controller.rows(ChiseTweaksSettingsController.Surface.HIGHLIGHT);
        var visual = controller.rows(ChiseTweaksSettingsController.Surface.FILTER);
        var analyzer = controller.rows(ChiseTweaksSettingsController.Surface.ANALYZER);
        var visibility = controller.rows(ChiseTweaksSettingsController.Surface.VISIBILITY);

        assertEquals("Ore Highlights", row(highlight, "materials").name());
        assertEquals("Nether Highlight", row(highlight, "nether").name());
        assertEquals("Fine Line Highlight", row(highlight, "thread").name());
        assertEquals("Hidden Block Highlight", row(highlight, "hidden").name());
        assertEquals("Glass Highlight", row(highlight, "glass").name());
        assertEquals("Kelp Highlight", row(highlight, "kelp").name());
        assertEquals("Block Filter", row(visual, "focusBlocks").name());
        assertEquals("Entity Filter", row(visual, "focusEntities").name());
        assertEquals("Lava Analyzer", row(analyzer, "lava").name());
        assertEquals("Ancient Debris Analyzer", row(analyzer, "ancientDebrisAnalyzer").name());
        assertEquals("Low Fire", row(visibility, "fireVisibility").name());
        assertEquals("Bright Chest", row(visibility, "chestVisibility").name());
        assertEquals("Bright Concrete", row(visibility, "whiteConcreteVisibility").name());
    }

    @Test
    void rowsRemainBoundToExpectedSettings() {
        var controller = new ChiseTweaksSettingsController();
        var highlight = controller.rows(ChiseTweaksSettingsController.Surface.HIGHLIGHT);
        var visual = controller.rows(ChiseTweaksSettingsController.Surface.FILTER);
        var analyzer = controller.rows(ChiseTweaksSettingsController.Surface.ANALYZER);
        var visibility = controller.rows(ChiseTweaksSettingsController.Surface.VISIBILITY);

        assertSame(FeatureSwitches.MATERIAL_HIGHLIGHTS, row(highlight, "materials").booleanConfig());
        assertSame(FeatureSwitches.NETHER_PALETTE, row(highlight, "nether").booleanConfig());
        assertSame(FeatureSwitches.FINE_THREAD_TRACE, row(highlight, "thread").booleanConfig());
        assertSame(FeatureSwitches.HIDDEN_SURFACE_TRACE, row(highlight, "hidden").booleanConfig());
        assertSame(FeatureSwitches.GLASS_INSPECTION, row(highlight, "glass").booleanConfig());
        assertSame(FeatureSwitches.KELP_HIGHLIGHT, row(highlight, "kelp").booleanConfig());
        assertSame(FeatureSwitches.BUILDER_FOCUS_BLOCKS, row(visual, "focusBlocks").booleanConfig());
        assertSame(FeatureSwitches.BUILDER_FOCUS_ENTITIES, row(visual, "focusEntities").booleanConfig());
        assertSame(BuilderFocusConfig.REFRESH_RENDERER, row(visual, "refreshRenderer").booleanConfig());
        assertSame(LocalFeatureSwitches.LAVA_HIGHLIGHT, row(analyzer, "lava").booleanConfig());
        assertSame(LocalFeatureSwitches.ANCIENT_DEBRIS_ANALYZER,
                row(analyzer, "ancientDebrisAnalyzer").booleanConfig());
        assertSame(LocalFeatureSwitches.FIRE_VISIBILITY, row(visibility, "fireVisibility").booleanConfig());
        assertSame(ChestVisibilitySetting.INSTANCE, row(visibility, "chestVisibility").booleanConfig());
        assertSame(WhiteConcreteVisibilitySetting.INSTANCE,
                row(visibility, "whiteConcreteVisibility").booleanConfig());
    }

    @Test
    void visualFilterTabOwnsBothTargetEditors() {
        var rows = new ChiseTweaksSettingsController().rows(ChiseTweaksSettingsController.Surface.FILTER);
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER,
                row(rows, "editBlockFilter").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER,
                row(rows, "editEntityFilter").action());
    }

    @Test
    void highlightTabKeepsAllModesIndependentAndOwnsTheirDetails() {
        var rows = new ChiseTweaksSettingsController().rows(ChiseTweaksSettingsController.Surface.HIGHLIGHT);
        assertSame(LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY,
                row(rows, "highlightWorldOverlay").booleanConfig());
        assertFalse(rows.stream().anyMatch(candidate -> "highlightExclusiveMode".equals(candidate.id())));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetMaterial")));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetTechnical")));
        assertTrue(rows.stream().anyMatch(candidate -> candidate.id().startsWith("visualTargetHidden")));
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT,
                row(rows, "moddedOreTargets").action());
    }

    @Test
    void analyzerTabKeepsLavaAndAncientDebrisSettingsSeparate() {
        var rows = new ChiseTweaksSettingsController().rows(ChiseTweaksSettingsController.Surface.ANALYZER);
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
    void rowIdsAreUniqueAndDiagnosticUtilityRowsDoNotReturn() {
        var controller = new ChiseTweaksSettingsController();
        Set<String> removedTokens = Set.of("diagnostic", "resourceReload", "copySnapshot", "exportSnapshot");
        for (ChiseTweaksSettingsController.Surface surface : ChiseTweaksSettingsController.Surface.values()) {
            Set<String> unique = new HashSet<>();
            for (ChiseTweaksSettingRowDefinition row : controller.rows(surface)) {
                assertTrue(unique.add(row.id()), "duplicate row id: " + row.id());
                String normalized = row.id().toLowerCase();
                for (String removed : removedTokens) {
                    assertFalse(normalized.contains(removed.toLowerCase()),
                            "removed utility leaked into settings row: " + row.id());
                }
            }
            assertRowContracts(controller.rows(surface));
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
                case HEADER, INFO -> {
                    assertNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    assertNull(row.action());
                    assertTrue(row.actionLabel().isEmpty());
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
