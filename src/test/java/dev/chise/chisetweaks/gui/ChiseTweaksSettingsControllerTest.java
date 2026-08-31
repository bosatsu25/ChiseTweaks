package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.SettingPersistence;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsControllerTest {
    @Test
    void rowIdsRejectWhitespaceAndUnsupportedCharacters() {
        assertThrows(IllegalArgumentException.class,
                () -> ChiseTweaksSettingRowDefinition.header(" bad ", "Bad"));
        assertThrows(IllegalArgumentException.class,
                () -> ChiseTweaksSettingRowDefinition.header("bad/id", "Bad"));
    }

    @Test
    void sixStableSurfacesExposeTweaksProductGroups() {
        assertEquals("Builder Highlights", TweaksProductSettingsRows.title(ChiseTweaksSettingsController.Surface.HIGHLIGHT));
        assertEquals("Scene Filter", TweaksProductSettingsRows.title(ChiseTweaksSettingsController.Surface.FILTER));
        assertEquals("Builder Assist", TweaksProductSettingsRows.title(ChiseTweaksSettingsController.Surface.INSPECTOR));
        assertEquals("Technical", TweaksProductSettingsRows.title(ChiseTweaksSettingsController.Surface.ANALYZER));
        assertEquals("Visual Tweaks", TweaksProductSettingsRows.title(ChiseTweaksSettingsController.Surface.VISIBILITY));
        assertEquals("Integrations", TweaksProductSettingsRows.title(ChiseTweaksSettingsController.Surface.INTEGRATIONS));

        List<String> highlights = ids(TweaksProductSettingsRows.rows(ChiseTweaksSettingsController.Surface.HIGHLIGHT));
        assertTrue(highlights.containsAll(List.of(
                "materials", "nether", "glass", "kelp", "lava", "hidden",
                "lavaRange", "hiddenRange", "visualTargetHiddenBlueIce")));
        assertFalse(highlights.contains("thread"));
        assertFalse(highlights.contains("villagerAnalyzer"));

        List<String> technical = ids(TweaksProductSettingsRows.rows(ChiseTweaksSettingsController.Surface.ANALYZER));
        assertTrue(technical.containsAll(List.of(
                "thread", "fineThreadColor", "fineThreadOpacity",
                "beaconRange", "lightningRodRange", "villagerAnalyzer",
                "visualTargetTechnicalTripwire", "visualTargetTechnicalTripwireHook")));
        assertFalse(technical.contains("lava"));
        assertFalse(technical.contains("hidden"));

        List<String> visual = ids(TweaksProductSettingsRows.rows(ChiseTweaksSettingsController.Surface.VISIBILITY));
        assertTrue(visual.containsAll(List.of(
                "fireVisibility", "fireVisibilitySize",
                "handheldSize", "handheldBlockScale", "handheldItemScale", "handheldToolScale",
                "chestVisibility", "whiteConcreteVisibility")));
        assertFalse(visual.contains("beaconRange"));
        assertFalse(visual.contains("lightningRodRange"));
    }

    @Test
    void featureBindingsMoveGroupsWithoutChangingPersistenceBindings() {
        var highlights = TweaksProductSettingsRows.rows(ChiseTweaksSettingsController.Surface.HIGHLIGHT);
        var technical = TweaksProductSettingsRows.rows(ChiseTweaksSettingsController.Surface.ANALYZER);
        var visual = TweaksProductSettingsRows.rows(ChiseTweaksSettingsController.Surface.VISIBILITY);

        assertSame(FeatureSwitches.MATERIAL_HIGHLIGHTS, row(highlights, "materials").booleanConfig());
        assertSame(FeatureSwitches.LAVA_HIGHLIGHT, row(highlights, "lava").booleanConfig());
        assertSame(FeatureSwitches.HIDDEN_SURFACE_TRACE, row(highlights, "hidden").booleanConfig());
        assertSame(LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                row(highlights, "lavaRange").integerConfig());
        assertSame(LocalFeatureSettings.HIDDEN_ANALYZER_HORIZONTAL_RADIUS,
                row(highlights, "hiddenRange").integerConfig());

        assertSame(FeatureSwitches.FINE_THREAD_TRACE, row(technical, "thread").booleanConfig());
        assertSame(FeatureSwitches.BEACON_RANGE, row(technical, "beaconRange").booleanConfig());
        assertSame(FeatureSwitches.LIGHTNING_ROD_RANGE, row(technical, "lightningRodRange").booleanConfig());
        assertSame(FeatureSwitches.VILLAGER_ANALYZER, row(technical, "villagerAnalyzer").booleanConfig());

        assertSame(FeatureSwitches.FIRE_VISIBILITY, row(visual, "fireVisibility").booleanConfig());
        assertSame(FeatureSwitches.HANDHELD_SIZE, row(visual, "handheldSize").booleanConfig());
        assertSame(FeatureSwitches.BRIGHT_CHEST, row(visual, "chestVisibility").booleanConfig());
        assertSame(FeatureSwitches.BRIGHT_CONCRETE, row(visual, "whiteConcreteVisibility").booleanConfig());
    }

    @Test
    void builderAssistHidesDeveloperDiagnosticsButRetainsBuilderTools() {
        List<String> ids = TweaksBuilderAssistRows.rows(CrosshairInspector.Snapshot.noTarget(), false)
                .stream().map(ChiseTweaksSettingRowDefinition::id).toList();

        assertTrue(ids.containsAll(List.of(
                "product.builderAssist",
                "inspector.noTarget",
                "placement.title",
                "pattern.title",
                "interactionHistory.title")));
        assertFalse(ids.contains("inspector.filter"));
        assertFalse(ids.contains("inspector.matchedRule"));
        assertFalse(ids.contains("inspector.features"));
        assertFalse(ids.contains("inspector.renderMode"));
    }

    @Test
    void rowsRemainUniqueAndStructurallyValid() {
        for (ChiseTweaksSettingsController.Surface surface : ChiseTweaksSettingsController.Surface.values()) {
            List<ChiseTweaksSettingRowDefinition> rows = surface == ChiseTweaksSettingsController.Surface.INSPECTOR
                    ? TweaksBuilderAssistRows.rows(CrosshairInspector.Snapshot.noTarget(), false)
                    : TweaksProductSettingsRows.rows(surface);
            Set<String> unique = new HashSet<>();
            for (ChiseTweaksSettingRowDefinition row : rows) {
                assertTrue(unique.add(row.id()), "duplicate row id: " + row.id());
            }
            assertRowContracts(rows);
        }
    }

    @Test
    void pendingPersistenceStateIsOwnedAndFilteredByController() {
        var controller = new ChiseTweaksSettingsController();
        assertFalse(controller.hasPendingChanges());
        controller.markDirty(SettingPersistence.EXTERNAL);
        assertFalse(controller.hasPendingChanges());

        controller.markDirty(SettingPersistence.FEATURE_CONFIG);
        controller.markDirty(Set.of(
                SettingPersistence.LOCAL_CONFIG,
                SettingPersistence.INTEGRATION_CONFIG,
                SettingPersistence.EXTERNAL));

        assertEquals(Set.of(
                        SettingPersistence.FEATURE_CONFIG,
                        SettingPersistence.LOCAL_CONFIG,
                        SettingPersistence.INTEGRATION_CONFIG),
                controller.pendingDomainsForDiagnostics());
    }

    private static ChiseTweaksSettingRowDefinition row(
            List<ChiseTweaksSettingRowDefinition> rows, String id) {
        return rows.stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing row: " + id));
    }

    private static List<String> ids(List<ChiseTweaksSettingRowDefinition> rows) {
        return rows.stream().map(ChiseTweaksSettingRowDefinition::id).toList();
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
                }
                case BOOLEAN -> {
                    assertNotNull(row.booleanConfig());
                    assertNull(row.integerConfig());
                    assertNull(row.action());
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
}
