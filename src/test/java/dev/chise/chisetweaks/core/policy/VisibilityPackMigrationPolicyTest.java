package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisibilityPackMigrationPolicyTest {
    private static final String LEGACY = "chisetweaks:chise_texture";
    private static final String CHEST = "chisetweaks:chise_chest_visibility";
    private static final String CONCRETE = "chisetweaks:chise_white_concrete_visibility";

    @Test
    void markerMakesMigrationIdempotent() {
        List<String> current = List.of("vanilla", CHEST);
        var plan = VisibilityPackMigrationPolicy.plan(
                current, true, true, "resourcePacks:[\"vanilla\"]", LEGACY, CHEST, CONCRETE);
        assertEquals(VisibilityPackMigrationPolicy.Source.ALREADY_MIGRATED, plan.source());
        assertEquals(current, plan.selectedIds());
        assertFalse(plan.selectionChanged());
    }

    @Test
    void existingSplitPackStateIsPreservedWithoutReenablingDisabledPack() {
        List<String> current = List.of("vanilla", CHEST, "user-pack");
        String options = "resourcePacks:[\"vanilla\",\"" + CHEST + "\",\"user-pack\"]";
        var plan = VisibilityPackMigrationPolicy.plan(
                current, false, true, options, LEGACY, CHEST, CONCRETE);
        assertEquals(VisibilityPackMigrationPolicy.Source.SPLIT_PACK_STATE, plan.source());
        assertEquals(current, plan.selectedIds());
        assertFalse(plan.selectionChanged());
    }

    @Test
    void legacyEnabledReplacesLegacyAtItsPositionAndKeepsUnrelatedOrder() {
        List<String> current = List.of("vanilla", "first", LEGACY, "last");
        String options = "resourcePacks:[\"vanilla\",\"" + LEGACY + "\"]";
        var plan = VisibilityPackMigrationPolicy.plan(
                current, false, true, options, LEGACY, CHEST, CONCRETE);
        assertEquals(VisibilityPackMigrationPolicy.Source.LEGACY_ENABLED, plan.source());
        assertEquals(List.of("vanilla", "first", CHEST, CONCRETE, "last"), plan.selectedIds());
        assertTrue(plan.selectionChanged());
    }

    @Test
    void legacyEnabledAddsSplitPacksWhenUnavailableLegacyIdAlreadyDroppedFromRepository() {
        List<String> current = List.of("vanilla", "user-pack");
        String options = "resourcePacks:[\"vanilla\",\"" + LEGACY + "\"]";
        var plan = VisibilityPackMigrationPolicy.plan(
                current, false, true, options, LEGACY, CHEST, CONCRETE);
        assertEquals(List.of("vanilla", "user-pack", CHEST, CONCRETE), plan.selectedIds());
    }

    @Test
    void legacyDisabledKeepsBothSplitPacksDisabled() {
        List<String> current = List.of("vanilla", CHEST, "user-pack", CONCRETE);
        var plan = VisibilityPackMigrationPolicy.plan(
                current, false, true, "resourcePacks:[\"vanilla\",\"user-pack\"]", LEGACY, CHEST, CONCRETE);
        assertEquals(VisibilityPackMigrationPolicy.Source.LEGACY_DISABLED, plan.source());
        assertEquals(List.of("vanilla", "user-pack"), plan.selectedIds());
        assertTrue(plan.selectionChanged());
    }

    @Test
    void brandNewInstallKeepsDefaultSplitSelection() {
        List<String> current = List.of("vanilla", CHEST, CONCRETE);
        var plan = VisibilityPackMigrationPolicy.plan(
                current, false, false, "resourcePacks:[\"vanilla\"]", LEGACY, CHEST, CONCRETE);
        assertEquals(VisibilityPackMigrationPolicy.Source.NEW_INSTALL, plan.source());
        assertEquals(current, plan.selectedIds());
        assertFalse(plan.selectionChanged());
    }

    @Test
    void packIdMentionOutsideResourcePackOptionDoesNotCountAsSelected() {
        String options = "lastServer:" + LEGACY + "\nresourcePacks:[\"vanilla\"]";
        assertFalse(VisibilityPackMigrationPolicy.optionPackSelected(options, LEGACY));
    }
}
