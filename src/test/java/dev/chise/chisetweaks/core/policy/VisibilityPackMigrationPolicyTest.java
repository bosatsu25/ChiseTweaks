package dev.chise.chisetweaks.core.policy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void eitherSplitPackInOptionsWinsOverLegacyEvidence() {
        String options = "resourcePacks:[\"" + LEGACY + "\",\"" + CONCRETE + "\"]";
        var plan = VisibilityPackMigrationPolicy.plan(
                List.of("vanilla", LEGACY, CONCRETE), false, true,
                options, LEGACY, CHEST, CONCRETE);
        assertEquals(VisibilityPackMigrationPolicy.Source.SPLIT_PACK_STATE, plan.source());
        assertEquals(List.of("vanilla", CONCRETE), plan.selectedIds());
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
    void currentSelectionIsSanitizedWithoutChangingUnrelatedRelativeOrder() {
        List<String> noisy = new ArrayList<>();
        noisy.add(" vanilla ");
        noisy.add(null);
        noisy.add("user-pack");
        noisy.add("vanilla");
        noisy.add("   ");
        noisy.add(LEGACY);
        String options = "resourcePacks:[\"" + LEGACY + "\"]";
        var plan = VisibilityPackMigrationPolicy.plan(
                noisy, false, true, options, LEGACY, CHEST, CONCRETE);
        assertEquals(List.of("vanilla", "user-pack", CHEST, CONCRETE), plan.selectedIds());
    }

    @Test
    void pendingIntentRetriesExactTargetAfterReloadRollbackErasedLegacyEvidence() {
        List<String> fallback = List.of("vanilla", "user-pack");
        var original = VisibilityPackMigrationPolicy.plan(
                fallback,
                false,
                true,
                "resourcePacks:[\"vanilla\",\"" + LEGACY + "\"]",
                LEGACY,
                CHEST,
                CONCRETE);
        var pending = VisibilityPackMigrationPolicy.pendingIntent(original, fallback);

        var resumed = VisibilityPackMigrationPolicy.resumePending(
                pending, fallback, LEGACY, CHEST, CONCRETE);

        assertEquals(VisibilityPackMigrationPolicy.Source.LEGACY_ENABLED, resumed.source());
        assertTrue(resumed.selectionChanged());
        assertEquals(original.selectedIds(), resumed.selectedIds());
    }

    @Test
    void pendingIntentCompletesWhenTargetWasPersistedSuccessfully() {
        List<String> fallback = List.of("vanilla", LEGACY, "after");
        var original = VisibilityPackMigrationPolicy.plan(
                fallback,
                false,
                true,
                "resourcePacks:[\"" + LEGACY + "\"]",
                LEGACY,
                CHEST,
                CONCRETE);
        var pending = VisibilityPackMigrationPolicy.pendingIntent(original, fallback);

        var resumed = VisibilityPackMigrationPolicy.resumePending(
                pending, original.selectedIds(), LEGACY, CHEST, CONCRETE);

        assertFalse(resumed.selectionChanged());
        assertEquals(original.selectedIds(), resumed.selectedIds());
    }

    @Test
    void pendingIntentPreservesNewUnrelatedPacksWhileRestoringManagedPackPlacement() {
        List<String> fallback = List.of("vanilla", LEGACY, "after");
        var original = VisibilityPackMigrationPolicy.plan(
                fallback,
                false,
                true,
                "resourcePacks:[\"" + LEGACY + "\"]",
                LEGACY,
                CHEST,
                CONCRETE);
        var pending = VisibilityPackMigrationPolicy.pendingIntent(original, fallback);

        var resumed = VisibilityPackMigrationPolicy.resumePending(
                pending,
                List.of("vanilla", "new-user-pack", "after"),
                LEGACY,
                CHEST,
                CONCRETE);

        assertTrue(resumed.selectionChanged());
        assertEquals(
                List.of("vanilla", CHEST, CONCRETE, "new-user-pack", "after"),
                resumed.selectedIds());
    }

    @Test
    void completedPlanCannotBecomePendingIntent() {
        var complete = new VisibilityPackMigrationPolicy.Plan(
                VisibilityPackMigrationPolicy.Source.ALREADY_MIGRATED,
                List.of("vanilla"),
                false);
        assertThrows(IllegalArgumentException.class,
                () -> VisibilityPackMigrationPolicy.pendingIntent(complete, List.of("vanilla")));
    }

    @Test
    void packIdMentionOutsideResourcePackOptionDoesNotCountAsSelected() {
        String options = "lastServer:" + LEGACY + "\nresourcePacks:[\"vanilla\"]";
        assertFalse(VisibilityPackMigrationPolicy.optionPackSelected(options, LEGACY));
    }

    @Test
    void longerPackIdContainingTargetDoesNotCountAsExactSelection() {
        String options = "resourcePacks:[\"" + CHEST + "_backup\"]";
        assertFalse(VisibilityPackMigrationPolicy.optionPackSelected(options, CHEST));
    }

    @Test
    void exactQuotedPackIdIsDetectedAmongOtherEntries() {
        String options = "resourcePacks:[\"vanilla\",\"" + CHEST + "\",\"other\"]";
        assertTrue(VisibilityPackMigrationPolicy.optionPackSelected(options, CHEST));
    }

    @Test
    void invalidPackIdsAreRejectedAtThePolicyBoundary() {
        assertThrows(NullPointerException.class, () -> VisibilityPackMigrationPolicy.plan(
                List.of(), false, false, "", null, CHEST, CONCRETE));
        assertThrows(IllegalArgumentException.class, () -> VisibilityPackMigrationPolicy.plan(
                List.of(), false, false, "", LEGACY, "   ", CONCRETE));
    }
}
