package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.PumpkinScaffoldPolicy;
import dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy;
import dev.chise.chisetweaks.core.security.FailureIsolationPolicy;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class PreJava25BoundaryTest {
    @Test
    void worksiteRadiusAndResultBoundariesAreStableAtMinusOneExactAndPlusOne() {
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(0));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(1));
        assertEquals(2, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(2));
        assertEquals(7, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(7));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(8));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(9));

        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampHudResults(0));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampHudResults(1));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHudResults(8));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHudResults(9));

        assertEquals(23, WorksiteVisibilityBudgetPolicy.clampOverlayResults(23));
        assertEquals(24, WorksiteVisibilityBudgetPolicy.clampOverlayResults(24));
        assertEquals(24, WorksiteVisibilityBudgetPolicy.clampOverlayResults(25));
    }

    @Test
    void pumpkinRangeBoundariesAreStableAtMinusOneExactAndPlusOne() {
        assertEquals(1, PumpkinScaffoldPolicy.clampPlacementRange(0));
        assertEquals(1, PumpkinScaffoldPolicy.clampPlacementRange(1));
        assertEquals(2, PumpkinScaffoldPolicy.clampPlacementRange(2));
        assertEquals(4, PumpkinScaffoldPolicy.clampPlacementRange(4));
        assertEquals(5, PumpkinScaffoldPolicy.clampPlacementRange(5));
        assertEquals(5, PumpkinScaffoldPolicy.clampPlacementRange(6));
    }

    @Test
    void exclusiveWorksiteSelectionNeverRetainsMoreThanOneMode() {
        var modes = WorksiteVisibilitySelectionPolicy.Mode.values();
        Set<WorksiteVisibilitySelectionPolicy.Mode> state = EnumSet.noneOf(
                WorksiteVisibilitySelectionPolicy.Mode.class);

        for (WorksiteVisibilitySelectionPolicy.Mode mode : modes) {
            state = WorksiteVisibilitySelectionPolicy.afterToggle(state, mode, true, true);
            assertEquals(Set.of(mode), state);
        }
    }

    @Test
    void nonExclusiveWorksiteSelectionPreservesIndependentModes() {
        Set<WorksiteVisibilitySelectionPolicy.Mode> state = Set.of();
        state = WorksiteVisibilitySelectionPolicy.afterToggle(
                state, WorksiteVisibilitySelectionPolicy.Mode.GLASS, true, false);
        state = WorksiteVisibilitySelectionPolicy.afterToggle(
                state, WorksiteVisibilitySelectionPolicy.Mode.NETHER_PALETTE, true, false);
        assertEquals(Set.of(
                WorksiteVisibilitySelectionPolicy.Mode.GLASS,
                WorksiteVisibilitySelectionPolicy.Mode.NETHER_PALETTE), state);

        state = WorksiteVisibilitySelectionPolicy.afterToggle(
                state, WorksiteVisibilitySelectionPolicy.Mode.GLASS, false, false);
        assertEquals(Set.of(WorksiteVisibilitySelectionPolicy.Mode.NETHER_PALETTE), state);
    }

    @Test
    void pumpkinScaffoldEligibilityFailsClosedForUnsafeClientStates() {
        assertTrue(PumpkinScaffoldPolicy.canHandleClick(
                true, true, true, true, false, false, true));
        assertFalse(PumpkinScaffoldPolicy.canHandleClick(
                false, true, true, true, false, false, true));
        assertFalse(PumpkinScaffoldPolicy.canHandleClick(
                true, false, true, true, false, false, true));
        assertFalse(PumpkinScaffoldPolicy.canHandleClick(
                true, true, false, true, false, false, true));
        assertFalse(PumpkinScaffoldPolicy.canHandleClick(
                true, true, true, false, false, false, true));
        assertFalse(PumpkinScaffoldPolicy.canHandleClick(
                true, true, true, true, true, false, true));
        assertFalse(PumpkinScaffoldPolicy.canHandleClick(
                true, true, true, true, false, true, true));
        assertFalse(PumpkinScaffoldPolicy.canHandleClick(
                true, true, true, true, false, false, false));
    }

    @Test
    void pumpkinAirPlacementRequiresLoadedChunkAndAirTarget() {
        assertTrue(PumpkinScaffoldPolicy.canAttemptAirPlacement(true, true));
        assertFalse(PumpkinScaffoldPolicy.canAttemptAirPlacement(false, true));
        assertFalse(PumpkinScaffoldPolicy.canAttemptAirPlacement(true, false));
        assertFalse(PumpkinScaffoldPolicy.canAttemptAirPlacement(false, false));
    }

    @Test
    void failureIsolationQuarantinesFirstRecoverableFailureButNotFatalJvmErrors() {
        assertEquals(1, FailureIsolationPolicy.MAX_RECOVERABLE_FAILURES);
        assertFalse(FailureIsolationPolicy.shouldQuarantine(0));
        assertTrue(FailureIsolationPolicy.shouldQuarantine(1));
        assertTrue(FailureIsolationPolicy.shouldQuarantine(2));
        assertTrue(FailureIsolationPolicy.isRecoverable(new IllegalArgumentException("test")));
        assertTrue(FailureIsolationPolicy.isRecoverable(new LinkageError("test")));
        assertFalse(FailureIsolationPolicy.isRecoverable(new OutOfMemoryError("test")));
        assertFalse(FailureIsolationPolicy.isRecoverable(new StackOverflowError("test")));
    }
}
