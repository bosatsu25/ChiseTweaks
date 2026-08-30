package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.MasaIntegrationSettings;
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
    void sixTabsExposeTheCurrentReleasedInformationArchitecture() {
        var controller = new ChiseTweaksSettingsController();

        assertEquals("Highlight", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.HIGHLIGHT));
        assertEquals("Filter", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.FILTER));
        assertEquals("Inspector", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.INSPECTOR));
        assertEquals("Analyzer", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.ANALYZER));
        assertEquals("Visibility", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.VISIBILITY));
        assertEquals("Integrations", controller.surfaceTitle(ChiseTweaksSettingsController.Surface.INTEGRATIONS));

        assertTrue(ids(controller.rows(ChiseTweaksSettingsController.Surface.HIGHLIGHT)).containsAll(
                List.of("materials", "nether", "thread", "hidden", "glass", "kelp")));
        assertTrue(ids(controller.rows(ChiseTweaksSettingsController.Surface.FILTER)).containsAll(
                List.of("focusBlocks", "focusEntities", "editBlockFilter", "editEntityFilter")));

        List<String> inspector = ids(controller.rows(ChiseTweaksSettingsController.Surface.INSPECTOR));
        assertTrue(inspector.containsAll(List.of(
                "inspector.title", "inspector.noTarget", "placement.title", "placement.none",
                "pattern.title", "pattern.select", "pattern.inactive")));
        assertFalse(inspector.contains("airPlacement"));

        List<String> analyzer = ids(controller.rows(ChiseTweaksSettingsController.Surface.ANALYZER));
        assertTrue(analyzer.containsAll(List.of(
                "header.analyzer", "lava", "lavaRange", "lavaVerticalRange", "lavaInterval", "lavaMaxOverlays")));
        assertFalse(analyzer.stream().anyMatch(id -> id.toLowerCase().contains("ancientdebris")));

        assertEquals(List.of(
                        "header.visibility",
                        "fireVisibility",
                        "fireVisibilitySize",
                        "chestVisibility",
                        "whiteConcreteVisibility"),
                ids(controller.rows(ChiseTweaksSettingsController.Surface.VISIBILITY)));
        assertTrue(controller.rows(ChiseTweaksSettingsController.Surface.INSPECTOR).stream()
                .anyMatch(row -> row.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO));

        List<String> integrations = ids(controller.rows(ChiseTweaksSettingsController.Surface.INTEGRATIONS));
        assertTrue(integrations.containsAll(List.of(
                "header.integrations", "masaJapaneseUiMode",
                "litematicaPickRedirect", "tweakerooToolSwitchGuard",
                "tweakerooPersistentGammaOverride", "tweakermoreAutoPickGuard",
                "tweakermoreMaterialListRefresh", "syncmaticaRemoveDisabled",
                "syncmaticaRemoveRequireShift")));
    }

    @Test
    void readmeNamesAreTheCanonicalUiNames() {
        var controller = new ChiseTweaksSettingsController();
        var highlight = controller.rows(ChiseTweaksSettingsController.Surface.HIGHLIGHT);
        var visual = controller.rows(ChiseTweaksSettingsController.Surface.FILTER);
        var analyzer = controller.rows(ChiseTweaksSettingsController.Surface.ANALYZER);
        var visibility = controller.rows(ChiseTweaksSettingsController.Surface.VISIBILITY);
        var integrations = controller.rows(ChiseTweaksSettingsController.Surface.INTEGRATIONS);

        assertEquals("Ore Highlights", row(highlight, "materials").name());
        assertEquals("Nether Highlight", row(highlight, "nether").name());
        assertEquals("Fine Line Highlight", row(highlight, "thread").name());
        assertEquals("Hidden Block Highlight", row(highlight, "hidden").name());
        assertEquals("Glass Highlight", row(highlight, "glass").name());
        assertEquals("Kelp Highlight", row(highlight, "kelp").name());
        assertEquals("Block Filter", row(visual, "focusBlocks").name());
        assertEquals("Entity Filter", row(visual, "focusEntities").name());
        assertEquals("Lava Analyzer", row(analyzer, "lava").name());
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
        assertSame(FeatureSwitches.LAVA_HIGHLIGHT, row(analyzer, "lava").booleanConfig());
        assertSame(FeatureSwitches.FIRE_VISIBILITY, row(visibility, "fireVisibility").booleanConfig());
        assertSame(LocalFeatureSettings.FIRE_VISIBILITY_SIZE,
                row(visibility, "fireVisibilitySize").integerConfig());
        assertSame(FeatureSwitches.BRIGHT_CHEST, row(visibility, "chestVisibility").booleanConfig());
        assertSame(FeatureSwitches.BRIGHT_CONCRETE,
                row(visibility, "whiteConcreteVisibility").booleanConfig());
        assertSame(MasaIntegrationSettings.LITEMATICA_PICK_REDIRECT,
                row(integrations, "litematicaPickRedirect").booleanConfig());
        assertSame(MasaIntegrationSettings.TWEAKEROO_TOOL_SWITCH_GUARD,
                row(integrations, "tweakerooToolSwitchGuard").booleanConfig());
        assertSame(MasaIntegrationSettings.TWEAKERMORE_AUTO_PICK_GUARD,
                row(integrations, "tweakermoreAutoPickGuard").booleanConfig());
        assertSame(MasaIntegrationSettings.SYNCMATICA_REMOVE_DISABLED,
                row(integrations, "syncmaticaRemoveDisabled").booleanConfig());
        assertSame(MasaIntegrationSettings.JAPANESE_UI_MODE,
                row(integrations, "masaJapaneseUiMode").integerConfig());
    }

    @Test
    void integrationsTabOwnsTheThreeUserEditableGuardLists() {
        var rows = new ChiseTweaksSettingsController().rows(
                ChiseTweaksSettingsController.Surface.INTEGRATIONS);
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_LITEMATICA_PICK_REDIRECT,
                row(rows, "editLitematicaPickRedirect").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKERMORE_AUTO_PICK_GUARD,
                row(rows, "editTweakerMoreAutoPickGuard").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.EDIT_TWEAKEROO_TOOL_SWITCH_GUARD,
                row(rows, "editTweakerooToolSwitchGuard").action());
        assertEquals(ChiseTweaksSettingRowDefinition.Action.OPEN_MASA_GUIDE,
                row(rows, "openMasaGuide").action());
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
    void analyzerTabOwnsOnlyTheRetainedLavaAnalyzerSettings() {
        var rows = new ChiseTweaksSettingsController().rows(ChiseTweaksSettingsController.Surface.ANALYZER);
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                row(rows, "lavaRange").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS,
                row(rows, "lavaVerticalRange").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                row(rows, "lavaInterval").integerConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS,
                row(rows, "lavaMaxOverlays").integerConfig());
        assertFalse(rows.stream().anyMatch(candidate -> candidate.id().toLowerCase().contains("ancientdebris")));
    }

    @Test
    void rowIdsAreUniqueAndRetiredUtilityOrFeatureRowsDoNotReturn() {
        var controller = new ChiseTweaksSettingsController();
        Set<String> removedTokens = Set.of(
                "diagnostic", "resourceReload", "copySnapshot", "exportSnapshot",
                "airPlacement", "ancientDebrisAnalyzer");
        for (ChiseTweaksSettingsController.Surface surface : ChiseTweaksSettingsController.Surface.values()) {
            Set<String> unique = new HashSet<>();
            for (ChiseTweaksSettingRowDefinition row : controller.rows(surface)) {
                assertTrue(unique.add(row.id()), "duplicate row id: " + row.id());
                String normalized = row.id().toLowerCase();
                for (String removed : removedTokens) {
                    assertFalse(normalized.contains(removed.toLowerCase()),
                            "retired row leaked into settings: " + row.id());
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
