package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.WorksiteScanThrottlePolicy;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RetainedPolicyQualityGateTest {
    @Test
    void scanBudgetsClampEveryPublicRange() {
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(Integer.MIN_VALUE));
        assertEquals(4, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(4));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(Integer.MAX_VALUE));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampVerticalRadius(Integer.MIN_VALUE));
        assertEquals(3, WorksiteVisibilityBudgetPolicy.clampVerticalRadius(3));
        assertEquals(5, WorksiteVisibilityBudgetPolicy.clampVerticalRadius(Integer.MAX_VALUE));
        assertEquals(5, WorksiteVisibilityBudgetPolicy.clampIntervalTicks(Integer.MIN_VALUE));
        assertEquals(10, WorksiteVisibilityBudgetPolicy.clampIntervalTicks(10));
        assertEquals(100, WorksiteVisibilityBudgetPolicy.clampIntervalTicks(Integer.MAX_VALUE));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampOverlayResults(Integer.MIN_VALUE));
        assertEquals(12, WorksiteVisibilityBudgetPolicy.clampOverlayResults(12));
        assertEquals(24, WorksiteVisibilityBudgetPolicy.clampOverlayResults(Integer.MAX_VALUE));
        assertEquals(1, WorksiteVisibilityBudgetPolicy.clampLegacyResults(Integer.MIN_VALUE));
        assertEquals(6, WorksiteVisibilityBudgetPolicy.clampLegacyResults(6));
        assertEquals(8, WorksiteVisibilityBudgetPolicy.clampLegacyResults(Integer.MAX_VALUE));
    }

    @Test
    void scanBudgetCalculationsStayBoundedAtBothEnds() {
        assertEquals(27, WorksiteVisibilityBudgetPolicy.maximumBlocksFor(1, 1));
        assertEquals(27, WorksiteVisibilityBudgetPolicy.maximumBlocksFor(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertEquals(3179, WorksiteVisibilityBudgetPolicy.maximumBlocksFor(8, 5));
        assertEquals(3179, WorksiteVisibilityBudgetPolicy.maximumBlocksFor(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(4, WorksiteVisibilityBudgetPolicy.maximumLoadedChunkProbesFor(1));
        assertEquals(4, WorksiteVisibilityBudgetPolicy.maximumLoadedChunkProbesFor(8));
        assertEquals(4, WorksiteVisibilityBudgetPolicy.MAX_LOADED_CHUNK_PROBES);
        assertEquals(128, WorksiteVisibilityBudgetPolicy.MAX_SCAN_CANDIDATES);
        assertEquals(24, WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS);
        assertEquals(192, WorksiteVisibilityBudgetPolicy.MAX_LINE_OF_SIGHT_RAYS_PER_SCAN);
    }

    @Test
    void scanThrottleCoversUrgentMovingAndIdleStateTransitions() {
        assertEquals(5, WorksiteScanThrottlePolicy.activeIntervalTicks(Integer.MIN_VALUE));
        assertEquals(10, WorksiteScanThrottlePolicy.activeIntervalTicks(10));
        assertEquals(100, WorksiteScanThrottlePolicy.activeIntervalTicks(Integer.MAX_VALUE));
        assertEquals(20, WorksiteScanThrottlePolicy.idleIntervalTicks(5));
        assertEquals(40, WorksiteScanThrottlePolicy.idleIntervalTicks(10));
        assertEquals(100, WorksiteScanThrottlePolicy.idleIntervalTicks(100));

        assertTrue(WorksiteScanThrottlePolicy.shouldScan(0, 10, true, false));
        assertFalse(WorksiteScanThrottlePolicy.shouldScan(-1, 10, false, false));
        assertFalse(WorksiteScanThrottlePolicy.shouldScan(9, 10, false, true));
        assertTrue(WorksiteScanThrottlePolicy.shouldScan(10, 10, false, true));
        assertFalse(WorksiteScanThrottlePolicy.shouldScan(39, 10, false, false));
        assertTrue(WorksiteScanThrottlePolicy.shouldScan(40, 10, false, false));
    }

    @Test
    void worksiteModeTogglePreservesOrExcludesOtherModesAsRequested() {
        var fine = WorksiteVisibilitySelectionPolicy.Mode.FINE_THREAD;
        var nether = WorksiteVisibilitySelectionPolicy.Mode.NETHER_PALETTE;
        var hidden = WorksiteVisibilitySelectionPolicy.Mode.HIDDEN_SURFACE;
        assertEquals(Set.of(fine), WorksiteVisibilitySelectionPolicy.afterToggle(null, fine, true, false));
        assertEquals(Set.of(), WorksiteVisibilitySelectionPolicy.afterToggle(null, fine, false, false));
        assertEquals(Set.of(fine, nether), WorksiteVisibilitySelectionPolicy.afterToggle(
                Set.of(fine), nether, true, false));
        assertEquals(Set.of(nether), WorksiteVisibilitySelectionPolicy.afterToggle(
                Set.of(fine, nether), fine, false, false));
        assertEquals(Set.of(hidden), WorksiteVisibilitySelectionPolicy.afterToggle(
                Set.of(fine, nether), hidden, true, true));
    }

    @Test
    void worksiteExclusiveNormalizationCollapsesOnlyWhenNecessary() {
        var fine = WorksiteVisibilitySelectionPolicy.Mode.FINE_THREAD;
        var nether = WorksiteVisibilitySelectionPolicy.Mode.NETHER_PALETTE;
        assertEquals(Set.of(), WorksiteVisibilitySelectionPolicy.normalize(null, true));
        assertEquals(Set.of(fine), WorksiteVisibilitySelectionPolicy.normalize(Set.of(fine), true));
        assertEquals(Set.of(fine, nether), WorksiteVisibilitySelectionPolicy.normalize(Set.of(fine, nether), false));
        assertEquals(Set.of(fine), WorksiteVisibilitySelectionPolicy.normalize(Set.of(fine, nether), true));
    }

    @Test
    void configListsTrimDeduplicateRejectUnsafeEntriesAndCapSize() {
        assertEquals(List.of(), ConfigListPolicy.sanitize(null));
        assertEquals(List.of("minecraft:stone", "Minecraft:Stone"), ConfigListPolicy.sanitize(List.of(
                "  minecraft:stone  ", "minecraft:stone", "Minecraft:Stone")));
        String tooLong = "a".repeat(ConfigListPolicy.MAX_ENTRY_CHARS + 1);
        assertEquals(List.of("ok"), ConfigListPolicy.sanitize(java.util.Arrays.asList(
                null, "", "   ", tooLong, "bad\nvalue", "bad\u202Evalue", " ok ")));
        ArrayList<String> oversized = new ArrayList<>();
        for (int index = 0; index < ConfigListPolicy.MAX_ENTRIES + 20; index++) {
            oversized.add("minecraft:block_" + index);
        }
        List<String> capped = ConfigListPolicy.sanitize(oversized);
        assertEquals(ConfigListPolicy.MAX_ENTRIES, capped.size());
        assertEquals("minecraft:block_0", capped.getFirst());
        assertEquals("minecraft:block_511", capped.getLast());
    }

    @Test
    void lavaAnalyzerHighlightsOnlyEnabledExposedSources() {
        assertTrue(LavaVisionPalettePolicy.shouldHighlight(true, true, true));
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(false, true, true));
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(true, false, true));
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(true, true, false));
        assertFalse(LavaVisionPalettePolicy.shouldHighlight(false, false, false));
    }

    @Test
    void lavaAnalyzerDistancePaletteHasStableNearMidAndFarSemantics() {
        assertEquals(0xFF075B32, LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB);
        assertEquals(0xFF021A0E, LavaVisionPalettePolicy.FAR_OUTLINE_ARGB);
        assertEquals(0.026f, LavaVisionPalettePolicy.ANALYZER_EDGE_THICKNESS);
        assertEquals(LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(-100));
        assertEquals(LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(0));
        assertEquals(LavaVisionPalettePolicy.SOURCE_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(2));
        assertEquals(0xFF053B20, LavaVisionPalettePolicy.colorForDistance(5));
        assertEquals(LavaVisionPalettePolicy.FAR_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(8));
        assertEquals(LavaVisionPalettePolicy.FAR_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(100));
        assertEquals(LavaVisionPalettePolicy.FAR_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(Double.NaN));
        assertEquals(LavaVisionPalettePolicy.FAR_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(Double.POSITIVE_INFINITY));
        assertEquals(LavaVisionPalettePolicy.FAR_OUTLINE_ARGB,
                LavaVisionPalettePolicy.colorForDistance(Double.NEGATIVE_INFINITY));
    }

    @Test
    void retainedVisualTargetGroupsAreDisjointCompleteAndNullSafe() {
        int material = VisualTargetGroupPolicy.maskFor(VisualTargetGroupPolicy.Group.MATERIAL);
        int hidden = VisualTargetGroupPolicy.maskFor(VisualTargetGroupPolicy.Group.HIDDEN);
        assertEquals(0, material & hidden);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, material | hidden);
        assertEquals(0, VisualTargetGroupPolicy.maskFor(null));
        int all = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        assertTrue(VisualTargetGroupPolicy.allEnabled(all, VisualTargetGroupPolicy.Group.MATERIAL));
        assertTrue(VisualTargetGroupPolicy.allEnabled(all, VisualTargetGroupPolicy.Group.HIDDEN));
        assertFalse(VisualTargetGroupPolicy.allEnabled(all, null));
        int noMaterial = VisualTargetGroupPolicy.withAll(all, VisualTargetGroupPolicy.Group.MATERIAL, false);
        assertFalse(VisualTargetGroupPolicy.allEnabled(noMaterial, VisualTargetGroupPolicy.Group.MATERIAL));
        assertTrue(VisualTargetGroupPolicy.allEnabled(noMaterial, VisualTargetGroupPolicy.Group.HIDDEN));
        assertEquals(all, VisualTargetGroupPolicy.withAll(
                noMaterial, VisualTargetGroupPolicy.Group.MATERIAL, true));
        assertEquals(all, VisualTargetGroupPolicy.withAll(all, null, false));
    }
}
