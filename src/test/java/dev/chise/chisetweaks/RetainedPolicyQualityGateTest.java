package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.BuilderEntityVisibilityPolicy;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void worksiteModeTogglePreservesOrExcludesOtherModesAsRequested() {
        var fine = WorksiteVisibilitySelectionPolicy.Mode.FINE_THREAD;
        var glass = WorksiteVisibilitySelectionPolicy.Mode.GLASS;
        var hidden = WorksiteVisibilitySelectionPolicy.Mode.HIDDEN_SURFACE;

        assertEquals(Set.of(fine), WorksiteVisibilitySelectionPolicy.afterToggle(null, fine, true, false));
        assertEquals(Set.of(), WorksiteVisibilitySelectionPolicy.afterToggle(null, fine, false, false));
        assertEquals(Set.of(fine, glass), WorksiteVisibilitySelectionPolicy.afterToggle(
                Set.of(fine), glass, true, false));
        assertEquals(Set.of(glass), WorksiteVisibilitySelectionPolicy.afterToggle(
                Set.of(fine, glass), fine, false, false));
        assertEquals(Set.of(hidden), WorksiteVisibilitySelectionPolicy.afterToggle(
                Set.of(fine, glass), hidden, true, true));
    }

    @Test
    void worksiteExclusiveNormalizationCollapsesOnlyWhenNecessary() {
        var fine = WorksiteVisibilitySelectionPolicy.Mode.FINE_THREAD;
        var glass = WorksiteVisibilitySelectionPolicy.Mode.GLASS;

        assertEquals(Set.of(), WorksiteVisibilitySelectionPolicy.normalize(null, true));
        assertEquals(Set.of(fine), WorksiteVisibilitySelectionPolicy.normalize(Set.of(fine), true));
        assertEquals(Set.of(fine, glass), WorksiteVisibilitySelectionPolicy.normalize(Set.of(fine, glass), false));
        assertEquals(Set.of(fine), WorksiteVisibilitySelectionPolicy.normalize(Set.of(fine, glass), true));
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
    void entityVisibilityPolicyKeepsLocalPlayerSafeAndImplementsExplicitLists() {
        var none = BuilderEntityVisibilityPolicy.Mode.NONE;
        var blacklist = BuilderEntityVisibilityPolicy.Mode.BLACKLIST;
        var whitelist = BuilderEntityVisibilityPolicy.Mode.WHITELIST;

        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW_DISABLED,
                BuilderEntityVisibilityPolicy.evaluate(input(false, false, "minecraft:zombie", blacklist,
                        Set.of("minecraft:zombie"), Set.of())));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW_LOCAL_PLAYER,
                BuilderEntityVisibilityPolicy.evaluate(input(true, true, "minecraft:player", blacklist,
                        Set.of("minecraft:player"), Set.of())));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW,
                BuilderEntityVisibilityPolicy.evaluate(input(true, false, "", blacklist, Set.of(), Set.of())));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW,
                BuilderEntityVisibilityPolicy.evaluate(input(true, false, "minecraft:zombie", none,
                        Set.of("minecraft:zombie"), Set.of())));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.HIDE,
                BuilderEntityVisibilityPolicy.evaluate(input(true, false, "minecraft:zombie", blacklist,
                        Set.of("minecraft:zombie"), Set.of())));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW,
                BuilderEntityVisibilityPolicy.evaluate(input(true, false, "minecraft:cow", blacklist,
                        Set.of("minecraft:zombie"), Set.of())));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.SHOW,
                BuilderEntityVisibilityPolicy.evaluate(input(true, false, "minecraft:cow", whitelist,
                        Set.of(), Set.of("minecraft:cow"))));
        assertEquals(BuilderEntityVisibilityPolicy.Decision.HIDE,
                BuilderEntityVisibilityPolicy.evaluate(input(true, false, "minecraft:zombie", whitelist,
                        Set.of(), Set.of("minecraft:cow"))));

        assertTrue(BuilderEntityVisibilityPolicy.filtersPlayers(blacklist,
                Set.of("minecraft:player"), Set.of()));
        assertFalse(BuilderEntityVisibilityPolicy.filtersPlayers(blacklist, Set.of(), Set.of()));
        assertFalse(BuilderEntityVisibilityPolicy.filtersPlayers(whitelist,
                Set.of(), Set.of("minecraft:player")));
        assertTrue(BuilderEntityVisibilityPolicy.filtersPlayers(whitelist, Set.of(), Set.of()));
    }

    @Test
    void entityVisibilityInputRejectsInvalidContainersAndNormalizesNullId() {
        assertThrows(NullPointerException.class, () -> BuilderEntityVisibilityPolicy.evaluate(null));
        assertThrows(NullPointerException.class, () -> new BuilderEntityVisibilityPolicy.Input(
                true, false, "minecraft:cow", null, Set.of(), Set.of()));
        assertThrows(NullPointerException.class, () -> new BuilderEntityVisibilityPolicy.Input(
                true, false, "minecraft:cow", BuilderEntityVisibilityPolicy.Mode.NONE, null, Set.of()));
        assertThrows(NullPointerException.class, () -> new BuilderEntityVisibilityPolicy.Input(
                true, false, "minecraft:cow", BuilderEntityVisibilityPolicy.Mode.NONE, Set.of(), null));
        assertEquals("", new BuilderEntityVisibilityPolicy.Input(
                true, false, null, BuilderEntityVisibilityPolicy.Mode.NONE, Set.of(), Set.of()).entityId());
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

    private static BuilderEntityVisibilityPolicy.Input input(
            boolean enabled,
            boolean localPlayer,
            String entityId,
            BuilderEntityVisibilityPolicy.Mode mode,
            Set<String> blacklist,
            Set<String> whitelist) {
        return new BuilderEntityVisibilityPolicy.Input(
                enabled, localPlayer, entityId, mode, blacklist, whitelist);
    }
}
