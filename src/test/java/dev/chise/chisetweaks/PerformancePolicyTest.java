package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.VisualModelReloadThrottlePolicy;
import dev.chise.chisetweaks.core.performance.WorksiteOverlayDetailPolicy;
import dev.chise.chisetweaks.core.performance.WorksiteScanThrottlePolicy;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PerformancePolicyTest {
    @Test
    void activeAndIdleScanIntervalsStayBounded() {
        assertEquals(5, WorksiteScanThrottlePolicy.activeIntervalTicks(Integer.MIN_VALUE));
        assertEquals(10, WorksiteScanThrottlePolicy.activeIntervalTicks(10));
        assertEquals(100, WorksiteScanThrottlePolicy.activeIntervalTicks(Integer.MAX_VALUE));

        assertEquals(20, WorksiteScanThrottlePolicy.idleIntervalTicks(1));
        assertEquals(40, WorksiteScanThrottlePolicy.idleIntervalTicks(10));
        assertEquals(100, WorksiteScanThrottlePolicy.idleIntervalTicks(30));
        assertEquals(100, WorksiteScanThrottlePolicy.idleIntervalTicks(Integer.MAX_VALUE));
    }

    @Test
    void scanThrottleRefreshesImmediatelyForStateChangesAndSlowsWhenIdle() {
        assertTrue(WorksiteScanThrottlePolicy.shouldScan(0, 10, true, false));
        assertTrue(WorksiteScanThrottlePolicy.shouldScan(-100, 10, true, true));

        assertFalse(WorksiteScanThrottlePolicy.shouldScan(9, 10, false, true));
        assertTrue(WorksiteScanThrottlePolicy.shouldScan(10, 10, false, true));

        assertFalse(WorksiteScanThrottlePolicy.shouldScan(39, 10, false, false));
        assertTrue(WorksiteScanThrottlePolicy.shouldScan(40, 10, false, false));
        assertFalse(WorksiteScanThrottlePolicy.shouldScan(-1, 10, false, false));
    }

    @Test
    void loadedChunkProbeBudgetIsConstantBoundedForTheSupportedRadius() {
        assertEquals(1, WorksiteVisibilityBudgetPolicy.maximumLoadedChunkProbesFor(1));
        assertEquals(4, WorksiteVisibilityBudgetPolicy.maximumLoadedChunkProbesFor(8));
        assertEquals(4, WorksiteVisibilityBudgetPolicy.maximumLoadedChunkProbesFor(Integer.MAX_VALUE));
        assertEquals(4, WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES);
        assertTrue(WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES
                < WorksiteVisibilityBudgetPolicy.maximumBlocksFor(8, 5));
    }

    @Test
    void overlayDetailKeepsNearTargetsRichAndDistantTargetsCompact() {
        assertEquals(WorksiteOverlayDetailPolicy.Detail.FULL,
                WorksiteOverlayDetailPolicy.detailFor(0.0));
        assertEquals(WorksiteOverlayDetailPolicy.Detail.FULL,
                WorksiteOverlayDetailPolicy.detailFor(49.0));
        assertEquals(WorksiteOverlayDetailPolicy.Detail.COMPACT,
                WorksiteOverlayDetailPolicy.detailFor(Math.nextUp(49.0)));
        assertEquals(WorksiteOverlayDetailPolicy.Detail.COMPACT,
                WorksiteOverlayDetailPolicy.detailFor(-0.01));
        assertEquals(WorksiteOverlayDetailPolicy.Detail.COMPACT,
                WorksiteOverlayDetailPolicy.detailFor(Double.NaN));
        assertEquals(WorksiteOverlayDetailPolicy.Detail.COMPACT,
                WorksiteOverlayDetailPolicy.detailFor(Double.POSITIVE_INFINITY));
    }

    @Test
    void modelReloadThrottleDebouncesRapidMaskChanges() {
        VisualModelReloadThrottlePolicy policy = new VisualModelReloadThrottlePolicy();

        assertFalse(policy.shouldRequestReload(1, 2, false));
        assertFalse(policy.shouldRequestReload(1, 2, false));
        assertFalse(policy.shouldRequestReload(1, 2, false));
        assertFalse(policy.shouldRequestReload(1, 2, false));
        assertTrue(policy.shouldRequestReload(1, 2, false));

        assertFalse(policy.shouldRequestReload(1, 3, false));
        assertFalse(policy.shouldRequestReload(1, 3, false));
        assertFalse(policy.shouldRequestReload(1, 3, false));
        assertFalse(policy.shouldRequestReload(1, 3, false));
        assertTrue(policy.shouldRequestReload(1, 3, false));
    }

    @Test
    void modelReloadThrottleHonorsInFlightAndFailureBackoff() {
        VisualModelReloadThrottlePolicy policy = new VisualModelReloadThrottlePolicy();
        primeReload(policy, 4, 5);
        assertFalse(policy.shouldRequestReload(4, 5, true));
        assertTrue(policy.shouldRequestReload(4, 5, false));

        policy.onReloadFailed();
        assertEquals(VisualModelReloadThrottlePolicy.FAILURE_BACKOFF_TICKS,
                policy.retryCooldownTicks());
        assertFalse(policy.shouldRequestReload(4, 5, false));
        assertEquals(VisualModelReloadThrottlePolicy.FAILURE_BACKOFF_TICKS - 1,
                policy.retryCooldownTicks());

        for (int i = 0; i < VisualModelReloadThrottlePolicy.FAILURE_BACKOFF_TICKS - 2; i++) {
            assertFalse(policy.shouldRequestReload(4, 5, false));
        }
        assertEquals(1, policy.retryCooldownTicks());
        assertTrue(policy.shouldRequestReload(4, 5, false));
        assertEquals(0, policy.retryCooldownTicks());
    }

    @Test
    void modelReloadThrottleClearsStableStateWhenAppliedOrReset() {
        VisualModelReloadThrottlePolicy policy = new VisualModelReloadThrottlePolicy();
        primeReload(policy, 7, 9);
        assertTrue(policy.shouldRequestReload(7, 9, false));

        assertFalse(policy.shouldRequestReload(9, 9, false));
        assertFalse(policy.shouldRequestReload(7, 9, false));

        policy.onReloadFailed();
        assertTrue(policy.retryCooldownTicks() > 0);
        policy.onReloadSucceeded();
        assertEquals(0, policy.retryCooldownTicks());
        assertFalse(policy.shouldRequestReload(7, 9, false));

        policy.onReloadFailed();
        policy.reset();
        assertEquals(0, policy.retryCooldownTicks());
        assertFalse(policy.shouldRequestReload(7, 9, false));
    }

    @Test
    void visualTargetGroupsAreDisjointCompleteAndPreserveUnrelatedBits() {
        int placement = VisualTargetGroupPolicy.maskFor(VisualTargetGroupPolicy.Group.PLACEMENT);
        int material = VisualTargetGroupPolicy.maskFor(VisualTargetGroupPolicy.Group.MATERIAL);
        int hidden = VisualTargetGroupPolicy.maskFor(VisualTargetGroupPolicy.Group.HIDDEN);

        assertEquals(0, placement & material);
        assertEquals(0, placement & hidden);
        assertEquals(0, material & hidden);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                placement | material | hidden);
        assertEquals(0, VisualTargetGroupPolicy.maskFor(null));
        assertFalse(VisualTargetGroupPolicy.allEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK, null));

        int all = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        int noPlacement = VisualTargetGroupPolicy.withAll(
                all, VisualTargetGroupPolicy.Group.PLACEMENT, false);
        assertFalse(VisualTargetGroupPolicy.allEnabled(
                noPlacement, VisualTargetGroupPolicy.Group.PLACEMENT));
        assertTrue(VisualTargetGroupPolicy.allEnabled(
                noPlacement, VisualTargetGroupPolicy.Group.MATERIAL));
        assertTrue(VisualTargetGroupPolicy.allEnabled(
                noPlacement, VisualTargetGroupPolicy.Group.HIDDEN));

        int restored = VisualTargetGroupPolicy.withAll(
                noPlacement, VisualTargetGroupPolicy.Group.PLACEMENT, true);
        assertEquals(all, restored);
        assertEquals(VisualTargetSelectionPolicy.sanitizeMask(all),
                VisualTargetGroupPolicy.withAll(all, null, false));
    }

    private static void primeReload(
            VisualModelReloadThrottlePolicy policy,
            int applied,
            int desired) {
        assertFalse(policy.shouldRequestReload(applied, desired, false));
        for (int i = 0; i < VisualModelReloadThrottlePolicy.QUIET_TICKS - 1; i++) {
            assertFalse(policy.shouldRequestReload(applied, desired, false));
        }
    }
}
