package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.WorksiteCandidateRetentionPolicy;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WorksiteCandidateRetentionPolicyTest {
    @Test
    void candidate127And128AreAcceptedWhileCapacityRemains() {
        int max = WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES;
        assertEquals(128, max);
        assertTrue(WorksiteCandidateRetentionPolicy.shouldRetain(
                126, max, 10, 100.0, 999, 1.0));
        assertTrue(WorksiteCandidateRetentionPolicy.shouldRetain(
                127, max, 10, 100.0, 999, 1.0));
    }

    @Test
    void candidate129ReplacesOnlyAWeakerEntry() {
        int max = WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES;
        assertTrue(WorksiteCandidateRetentionPolicy.shouldRetain(
                128, max, 81, 100.0, 80, 1.0));
        assertFalse(WorksiteCandidateRetentionPolicy.shouldRetain(
                128, max, 79, 1.0, 80, 100.0));
    }

    @Test
    void equalPriorityUsesNearestDistanceAsTieBreaker() {
        assertTrue(WorksiteCandidateRetentionPolicy.isBetter(50, 24.99, 50, 25.0));
        assertFalse(WorksiteCandidateRetentionPolicy.isBetter(50, 25.0, 50, 25.0));
        assertFalse(WorksiteCandidateRetentionPolicy.isBetter(50, 25.01, 50, 25.0));
    }

    @Test
    void invalidZeroCapacityNeverRetains() {
        assertFalse(WorksiteCandidateRetentionPolicy.shouldRetain(
                0, 0, Integer.MAX_VALUE, 0.0, Integer.MIN_VALUE, Double.POSITIVE_INFINITY));
    }
}
