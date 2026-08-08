package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.security.FailureIsolationPolicy;
import dev.chise.chisetweaks.core.security.StartupPhasePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RuntimePolicyTest {
    @Test
    void worksiteBudgetsClampEveryExternalSetting() {
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(Integer.MIN_VALUE));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(Integer.MAX_VALUE));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampVerticalRadius(Integer.MIN_VALUE));
        assertEquals(5, WorksiteVisibilityBudgetPolicy.clampVerticalRadius(Integer.MAX_VALUE));
        assertEquals(5, WorksiteVisibilityBudgetPolicy.clampIntervalTicks(Integer.MIN_VALUE));
        assertEquals(100, WorksiteVisibilityBudgetPolicy.clampIntervalTicks(Integer.MAX_VALUE));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampHudResults(Integer.MIN_VALUE));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHudResults(Integer.MAX_VALUE));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampOverlayResults(Integer.MIN_VALUE));
        assertEquals(24, WorksiteVisibilityBudgetPolicy.clampOverlayResults(Integer.MAX_VALUE));
    }

    @Test
    void maximumBlockBudgetUsesClampedRadii() {
        assertEquals(27, WorksiteVisibilityBudgetPolicy.maximumBlocksFor(1, 1));
        assertEquals(17 * 17 * 11, WorksiteVisibilityBudgetPolicy.maximumBlocksFor(999, 999));
    }

    @Test
    void startupFailureCountIsBoundedAgainstInvalidConfiguration() {
        assertEquals(0, StartupPhasePolicy.boundedFailureCount(Integer.MIN_VALUE));
        assertEquals(0, StartupPhasePolicy.boundedFailureCount(-1));
        assertEquals(0, StartupPhasePolicy.boundedFailureCount(0));
        assertEquals(1, StartupPhasePolicy.boundedFailureCount(1));
        assertEquals(StartupPhasePolicy.MAX_RECORDED_FAILURES,
                StartupPhasePolicy.boundedFailureCount(Integer.MAX_VALUE));
    }

    @Test
    void failureIsolationDoesNotTreatJvmFatalErrorsAsRecoverable() {
        assertTrue(FailureIsolationPolicy.isRecoverable(new IllegalStateException("test")));
        assertTrue(FailureIsolationPolicy.isRecoverable(new LinkageError("test")));
        assertFalse(FailureIsolationPolicy.isRecoverable(new OutOfMemoryError("test")));
        assertFalse(FailureIsolationPolicy.shouldQuarantine(0));
        assertTrue(FailureIsolationPolicy.shouldQuarantine(1));
    }
}
