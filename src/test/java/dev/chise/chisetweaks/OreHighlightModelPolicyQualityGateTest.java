package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.VisualModelReloadThrottlePolicy;
import dev.chise.chisetweaks.core.vision.VisualModelSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic state/selection quality gate for model-backed Ore Highlights. */
final class OreHighlightModelPolicyQualityGateTest {
    @Test
    void modelSelectionSeparatesVanillaOresSpecialMaterialsAndHiddenTargets() {
        int oreMask = VisualModelSelectionPolicy.VANILLA_ORE_MODEL_TARGET_MASK;
        int specialMask = VisualModelSelectionPolicy.SPECIAL_MATERIAL_MODEL_TARGET_MASK;
        int materialMask = VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK;

        assertEquals(0, oreMask & specialMask);
        assertEquals(oreMask | specialMask, materialMask);
        assertEquals(VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK, materialMask);
        assertEquals(0, materialMask & VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK);
    }

    @Test
    void disabledMasterSwitchAlwaysProducesZeroActiveModelTargets() {
        assertEquals(0, VisualModelSelectionPolicy.activeMaterialModelMask(
                false, VisualTargetSelectionPolicy.ALL_TARGETS_MASK));
        assertEquals(0, VisualModelSelectionPolicy.activeMaterialModelMask(false, Integer.MAX_VALUE));
        assertEquals(0, VisualModelSelectionPolicy.activeMaterialModelMask(false, Integer.MIN_VALUE));
    }

    @Test
    void enabledMasterSwitchSanitizesAndKeepsOnlyMaterialTargets() {
        assertEquals(VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK,
                VisualModelSelectionPolicy.activeMaterialModelMask(
                        true, VisualTargetSelectionPolicy.ALL_TARGETS_MASK));
        assertEquals(VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK,
                VisualModelSelectionPolicy.activeMaterialModelMask(true, Integer.MAX_VALUE));
        assertEquals(0, VisualModelSelectionPolicy.activeMaterialModelMask(
                true, VisualTargetSelectionPolicy.HIDDEN_SURFACE_TARGETS_MASK));
        assertEquals(0, VisualModelSelectionPolicy.activeMaterialModelMask(true, 0));
    }

    @Test
    void oneTargetSelectionCannotAccidentallyEnableAnotherOreFamily() {
        int diamond = Target.MATERIAL_DIAMOND_ORE.bitMask();
        int redstone = Target.MATERIAL_REDSTONE_ORE.bitMask();

        assertTrue(VisualModelSelectionPolicy.useMaterialTarget(diamond, Target.MATERIAL_DIAMOND_ORE));
        assertFalse(VisualModelSelectionPolicy.useMaterialTarget(diamond, Target.MATERIAL_REDSTONE_ORE));
        assertFalse(VisualModelSelectionPolicy.useMaterialTarget(redstone, Target.MATERIAL_DIAMOND_ORE));
        assertTrue(VisualModelSelectionPolicy.useMaterialTarget(redstone, Target.MATERIAL_REDSTONE_ORE));
        assertFalse(VisualModelSelectionPolicy.useMaterialTarget(diamond, Target.HIDDEN_BLUE_ICE));
        assertFalse(VisualModelSelectionPolicy.useMaterialTarget(diamond, null));
    }

    @Test
    void allVanillaOrePredicateIgnoresSpecialMaterialsButRequiresEveryOreFamily() {
        int ores = VisualModelSelectionPolicy.VANILLA_ORE_MODEL_TARGET_MASK;
        int allMaterials = VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK;

        assertTrue(VisualModelSelectionPolicy.useAllVanillaOres(ores));
        assertTrue(VisualModelSelectionPolicy.useAllVanillaOres(allMaterials));
        assertFalse(VisualModelSelectionPolicy.useAllVanillaOres(
                ores & ~Target.MATERIAL_DIAMOND_ORE.bitMask()));
        assertFalse(VisualModelSelectionPolicy.useAllVanillaOres(
                VisualModelSelectionPolicy.SPECIAL_MATERIAL_MODEL_TARGET_MASK));
        assertFalse(VisualModelSelectionPolicy.useAllVanillaOres(0));
    }

    @Test
    void diamondCompatibilityHelperFollowsMasterAndTargetState() {
        int diamond = Target.MATERIAL_DIAMOND_ORE.bitMask();
        assertTrue(VisualModelSelectionPolicy.useDiamondOreModel(true, diamond));
        assertFalse(VisualModelSelectionPolicy.useDiamondOreModel(false, diamond));
        assertFalse(VisualModelSelectionPolicy.useDiamondOreModel(true, 0));
        assertFalse(VisualModelSelectionPolicy.useDiamondOreModel(
                true, Target.MATERIAL_GOLD_ORE.bitMask()));
    }

    @Test
    void reloadPolicyDebouncesUntilTheConfiguredQuietBoundary() {
        VisualModelReloadThrottlePolicy policy = new VisualModelReloadThrottlePolicy();
        assertEquals(4, VisualModelReloadThrottlePolicy.QUIET_TICKS);
        assertFalse(policy.shouldRequestReload(0, 1, false)); // first observation
        assertFalse(policy.shouldRequestReload(0, 1, false)); // stable tick 1
        assertFalse(policy.shouldRequestReload(0, 1, false)); // stable tick 2
        assertFalse(policy.shouldRequestReload(0, 1, false)); // stable tick 3
        assertTrue(policy.shouldRequestReload(0, 1, false));  // stable tick 4
    }

    @Test
    void reloadInFlightSuppressesDuplicateRequestsWithoutLosingStableState() {
        VisualModelReloadThrottlePolicy policy = stableMismatchPolicy();
        assertFalse(policy.shouldRequestReload(0, 1, true));
        assertFalse(policy.shouldRequestReload(0, 1, true));
        assertTrue(policy.shouldRequestReload(0, 1, false));
    }

    @Test
    void changingDesiredMaskRestartsTheDebounceWindow() {
        VisualModelReloadThrottlePolicy policy = new VisualModelReloadThrottlePolicy();
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertFalse(policy.shouldRequestReload(0, 2, false));
        assertFalse(policy.shouldRequestReload(0, 2, false));
        assertFalse(policy.shouldRequestReload(0, 2, false));
        assertFalse(policy.shouldRequestReload(0, 2, false));
        assertTrue(policy.shouldRequestReload(0, 2, false));
    }

    @Test
    void returningToAppliedMaskCancelsPendingReloadAndRequiresFreshDebounceLater() {
        VisualModelReloadThrottlePolicy policy = new VisualModelReloadThrottlePolicy();
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertFalse(policy.shouldRequestReload(0, 0, false));
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertTrue(policy.shouldRequestReload(0, 1, false));
    }

    @Test
    void failedReloadUsesExactBoundedBackoffBeforeRetry() {
        VisualModelReloadThrottlePolicy policy = stableMismatchPolicy();
        assertTrue(policy.shouldRequestReload(0, 1, false));
        policy.onReloadFailed();
        assertEquals(VisualModelReloadThrottlePolicy.FAILURE_BACKOFF_TICKS, policy.retryCooldownTicks());

        for (int remaining = VisualModelReloadThrottlePolicy.FAILURE_BACKOFF_TICKS - 1;
             remaining > 0;
             remaining--) {
            assertFalse(policy.shouldRequestReload(0, 1, false));
            assertEquals(remaining, policy.retryCooldownTicks());
        }

        assertTrue(policy.shouldRequestReload(0, 1, false));
        assertEquals(0, policy.retryCooldownTicks());
    }

    @Test
    void successfulReloadClearsBackoffAndRequiresFreshDesiredObservation() {
        VisualModelReloadThrottlePolicy policy = stableMismatchPolicy();
        assertTrue(policy.shouldRequestReload(0, 1, false));
        policy.onReloadFailed();
        assertTrue(policy.retryCooldownTicks() > 0);
        policy.onReloadSucceeded();
        assertEquals(0, policy.retryCooldownTicks());
        assertFalse(policy.shouldRequestReload(1, 2, false));
    }

    @Test
    void resetReturnsReloadPolicyToCleanStartupState() {
        VisualModelReloadThrottlePolicy policy = stableMismatchPolicy();
        policy.onReloadFailed();
        assertTrue(policy.retryCooldownTicks() > 0);
        policy.reset();
        assertEquals(0, policy.retryCooldownTicks());
        assertFalse(policy.shouldRequestReload(0, 1, false));
        assertFalse(policy.shouldRequestReload(0, 0, false));
    }

    private static VisualModelReloadThrottlePolicy stableMismatchPolicy() {
        VisualModelReloadThrottlePolicy policy = new VisualModelReloadThrottlePolicy();
        assertFalse(policy.shouldRequestReload(0, 1, false));
        for (int tick = 1; tick < VisualModelReloadThrottlePolicy.QUIET_TICKS; tick++) {
            assertFalse(policy.shouldRequestReload(0, 1, false));
        }
        return policy;
    }
}
