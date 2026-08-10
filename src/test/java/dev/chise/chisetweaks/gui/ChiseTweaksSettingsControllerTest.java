package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingsControllerTest {
    @Test
    void japaneseAndEnglishUseTheSameStableRowStructure() {
        var japanese = new ChiseTweaksSettingsController(true);
        var english = new ChiseTweaksSettingsController(false);

        for (ChiseTweaksUiSection section : List.of(
                ChiseTweaksUiSection.RESOURCES,
                ChiseTweaksUiSection.VISIBILITY)) {
            List<ChiseTweaksSettingRowDefinition> japaneseRows = japanese.rowsFor(section);
            List<ChiseTweaksSettingRowDefinition> englishRows = english.rowsFor(section);

            assertEquals(ids(englishRows), ids(japaneseRows), section.name());
            assertEquals(kinds(englishRows), kinds(japaneseRows), section.name());
            assertFalse(japaneseRows.isEmpty(), section.name());
            assertRowContracts(japaneseRows);
            assertRowContracts(englishRows);
        }
    }

    @Test
    void helpIsNavigationOnlyAndDoesNotCreateSettingsRows() {
        var controller = new ChiseTweaksSettingsController(true);

        assertTrue(controller.rowsFor(ChiseTweaksUiSection.HELP).isEmpty());
        assertTrue(controller.rowsFor(null).isEmpty());
    }

    @Test
    void rowIdsAreUniqueWithinEachSectionAndRemovedFeaturesDoNotReturn() {
        var controller = new ChiseTweaksSettingsController(false);
        Set<String> removedTokens = Set.of(
                "pumpkin",
                "placement",
                "lavaSourceColor",
                "sodium");

        for (ChiseTweaksUiSection section : List.of(
                ChiseTweaksUiSection.RESOURCES,
                ChiseTweaksUiSection.VISIBILITY)) {
            List<ChiseTweaksSettingRowDefinition> rows = controller.rowsFor(section);
            Set<String> unique = new HashSet<>();
            for (ChiseTweaksSettingRowDefinition row : rows) {
                assertTrue(unique.add(row.id()), "duplicate row id: " + row.id());
                String normalized = row.id().toLowerCase();
                for (String removed : removedTokens) {
                    assertFalse(normalized.contains(removed.toLowerCase()),
                            "removed feature leaked into settings row: " + row.id());
                }
            }
        }
    }

    @Test
    void actionRowsRemainPairedWithBothSceneFilterTargets() {
        var controller = new ChiseTweaksSettingsController(false);
        List<ChiseTweaksSettingRowDefinition> rows = controller.rowsFor(ChiseTweaksUiSection.VISIBILITY);

        assertTrue(rows.stream().anyMatch(row ->
                row.action() == ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER));
        assertTrue(rows.stream().anyMatch(row ->
                row.action() == ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER));
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
