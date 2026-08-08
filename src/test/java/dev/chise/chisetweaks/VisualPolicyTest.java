package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.policy.PumpkinScaffoldPolicy;
import dev.chise.chisetweaks.core.policy.WorksiteVisibilitySelectionPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class VisualPolicyTest {
    @Test void pumpkinScaffoldRangeIsStrictlyBounded() {
        assertEquals(1, PumpkinScaffoldPolicy.clampPlacementRange(Integer.MIN_VALUE));
        assertEquals(1, PumpkinScaffoldPolicy.clampPlacementRange(1));
        assertEquals(4, PumpkinScaffoldPolicy.clampPlacementRange(4));
        assertEquals(5, PumpkinScaffoldPolicy.clampPlacementRange(5));
        assertEquals(5, PumpkinScaffoldPolicy.clampPlacementRange(Integer.MAX_VALUE));
    }

    @Test void worksiteModesContainTheSixRequestedScanModes() {
        assertEquals(6, WorksiteVisibilitySelectionPolicy.Mode.values().length);
        assertNotNull(WorksiteVisibilitySelectionPolicy.Mode.PLACEMENT_GUIDE);
    }

    @Test void placementGuideCoversDecorationFamiliesWithoutTakingGlassOwnership() {
        assertTrue(BlockInspectionPolicy.matches("minecraft:anvil", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:beehive", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:soul_campfire", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:grindstone", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:oak_fence_gate", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:ochre_froglight", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:blue_glazed_terracotta", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:stone_slab", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:quartz_stairs", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:iron_trapdoor", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:stripped_oak_log", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:warped_hyphae", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:bamboo_block", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertFalse(BlockInspectionPolicy.matches("minecraft:white_stained_glass", BlockInspectionCategory.PLACEMENT_GUIDE));
        assertTrue(BlockInspectionPolicy.matches("minecraft:white_stained_glass", BlockInspectionCategory.GLASS_INSPECTION));
    }

    @Test void placementGuideReportsRelevantBlockState() {
        var result = BlockInspectionPolicy.inspect(
                "minecraft:oak_stairs",
                Map.of("facing", "east", "half", "top", "shape", "inner_left", "waterlogged", "false"),
                BlockInspectionCategory.PLACEMENT_GUIDE);
        assertEquals(BlockInspectionCategory.PLACEMENT_GUIDE, result.category());
        assertEquals(java.util.List.of("facing=east", "half=top", "shape=inner_left"), result.details());
    }
}
