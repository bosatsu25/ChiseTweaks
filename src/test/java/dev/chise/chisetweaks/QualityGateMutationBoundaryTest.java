package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.WorksiteCandidateRetentionPolicy;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import dev.chise.chisetweaks.core.policy.ModVersionPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QualityGateMutationBoundaryTest {
    @Test
    void configListsExerciseNullLengthControlAndCapacityBoundaries() {
        String maxLength = "x".repeat(ConfigListPolicy.MAX_ENTRY_CHARS);
        String tooLong = "x".repeat(ConfigListPolicy.MAX_ENTRY_CHARS + 1);
        List<String> raw = Arrays.asList(
                null, "", "   ", maxLength, tooLong, " duplicate ", "duplicate",
                "bad\u202Evalue", "bad\nvalue", "tail");

        assertEquals(List.of(maxLength, "duplicate", "tail"), ConfigListPolicy.sanitize(raw));
        assertEquals(List.of(), ConfigListPolicy.sanitize(null));

        ArrayList<String> many = new ArrayList<>();
        for (int i = 0; i < ConfigListPolicy.MAX_ENTRIES + 1; i++) many.add("entry-" + i);
        List<String> bounded = ConfigListPolicy.sanitize(many);
        assertEquals(ConfigListPolicy.MAX_ENTRIES, bounded.size());
        assertEquals("entry-0", bounded.getFirst());
        assertEquals("entry-511", bounded.getLast());
    }

    @Test
    void candidateRetentionDistinguishesExactQueueAndDistanceBoundaries() {
        assertFalse(WorksiteCandidateRetentionPolicy.shouldRetain(0, 0, 10, 1.0, 1, 9.0));
        assertTrue(WorksiteCandidateRetentionPolicy.shouldRetain(2, 3, 0, 100.0, 99, 0.0));
        assertFalse(WorksiteCandidateRetentionPolicy.shouldRetain(3, 3, 5, 4.0, 5, 4.0));
        assertTrue(WorksiteCandidateRetentionPolicy.shouldRetain(3, 3, 5, 3.999, 5, 4.0));
        assertFalse(WorksiteCandidateRetentionPolicy.isBetter(5, 4.0, 5, 4.0));
        assertTrue(WorksiteCandidateRetentionPolicy.isBetter(6, 100.0, 5, 0.0));
        assertFalse(WorksiteCandidateRetentionPolicy.isBetter(4, 0.0, 5, 100.0));
    }

    @Test
    void versionComparisonExercisesUnequalWidthsPrefixesSuffixesAndIntegerLimits() {
        assertFalse(ModVersionPolicy.meetsMinimum("1", "1.0.1"));
        assertTrue(ModVersionPolicy.meetsMinimum("1.0.1", "1"));
        assertTrue(ModVersionPolicy.meetsMinimum("1.0.0", "1"));
        assertTrue(ModVersionPolicy.meetsMinimum("v1", "1"));
        assertTrue(ModVersionPolicy.meetsMinimum("V1", "1"));
        assertFalse(ModVersionPolicy.meetsMinimum("v", "1"));
        assertFalse(ModVersionPolicy.meetsMinimum("+build", "1"));
        assertFalse(ModVersionPolicy.meetsMinimum("-rc1", "1"));

        assertTrue(ModVersionPolicy.compareRelease("1.0.1", "1") > 0);
        assertTrue(ModVersionPolicy.compareRelease("1", "1.0.1") < 0);
        assertEquals(0, ModVersionPolicy.compareRelease("1", "1.0.0"));
        assertEquals(0, ModVersionPolicy.compareRelease("2147483647", "2147483647"));
        assertFalse(ModVersionPolicy.meetsMinimum("2147483648", "2147483647"));
        assertFalse(ModVersionPolicy.matchesPinnedRelease("1..0", "1.0"));
        assertFalse(ModVersionPolicy.matchesPinnedRelease("1.0", "1..0"));
    }
}
