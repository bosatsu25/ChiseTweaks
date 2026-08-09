package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class VisualTargetSelectionPolicyTest {
    @Test
    void allTargetsUseUniqueBitsAndStartEnabled() {
        Set<Integer> bits = new HashSet<>();
        int combined = 0;
        for (Target target : Target.values()) {
            assertTrue(bits.add(target.bitMask()), target.name());
            combined |= target.bitMask();
            assertTrue(VisualTargetSelectionPolicy.isEnabled(
                    VisualTargetSelectionPolicy.ALL_TARGETS_MASK, target));
        }
        assertEquals(28, Target.values().length);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, combined);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                VisualTargetSelectionPolicy.sanitizeMask(-1));
        assertEquals(0, VisualTargetSelectionPolicy.sanitizeMask(0));
    }

    @Test
    void appendedNetherBitsPreserveAllLegacyBitAssignments() {
        assertEquals(1 << 21, Target.HIDDEN_BLUE_ICE.bitMask());
        assertEquals(1 << 22, Target.HIDDEN_DEAD_CORAL.bitMask());
        assertEquals(1 << 23, Target.HIDDEN_POWDER_SNOW.bitMask());
        assertEquals(1 << 24, Target.HIDDEN_SCULK_CATALYST.bitMask());
        assertEquals(1 << 25, Target.MATERIAL_CRYING_OBSIDIAN.bitMask());
        assertEquals(1 << 26, Target.MATERIAL_NETHER_GOLD_ORE.bitMask());
        assertEquals(1 << 27, Target.MATERIAL_NETHER_QUARTZ_ORE.bitMask());
    }

    @Test
    void legacyMasksGainNewTargetsExactlyOnce() {
        int legacyMask = (1 << 25) - 1;
        int migrated = VisualTargetSelectionPolicy.migrateMask(
                legacyMask,
                VisualTargetSelectionPolicy.LEGACY_SCHEMA_VERSION);
        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK, migrated);

        int explicitlyDisabled = VisualTargetSelectionPolicy.withEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                Target.MATERIAL_NETHER_QUARTZ_ORE,
                false);
        assertEquals(explicitlyDisabled, VisualTargetSelectionPolicy.migrateMask(
                explicitlyDisabled,
                VisualTargetSelectionPolicy.CURRENT_SCHEMA_VERSION));
    }

    @Test
    void individualBitsCanBeDisabledAndRestoredWithoutChangingOthers() {
        for (Target target : Target.values()) {
            int disabled = VisualTargetSelectionPolicy.withEnabled(
                    VisualTargetSelectionPolicy.ALL_TARGETS_MASK, target, false);
            assertFalse(VisualTargetSelectionPolicy.isEnabled(disabled, target), target.name());
            for (Target other : Target.values()) {
                if (other != target) {
                    assertTrue(VisualTargetSelectionPolicy.isEnabled(disabled, other),
                            target + " changed " + other);
                }
            }
            assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                    VisualTargetSelectionPolicy.withEnabled(disabled, target, true));
        }

        assertEquals(VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                VisualTargetSelectionPolicy.withEnabled(-1, null, false));
        assertFalse(VisualTargetSelectionPolicy.isEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK, null));
    }

    @Test
    void placementFamiliesFollowTheirGroupedSwitches() {
        assertFamily(Target.PLACEMENT_ANVIL, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:anvil");
        assertFamily(Target.PLACEMENT_BEEHIVE, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:beehive");
        assertFamily(Target.PLACEMENT_CAMPFIRE, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:campfire", "minecraft:soul_campfire");
        assertFamily(Target.PLACEMENT_GLAZED_TERRACOTTA, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:blue_glazed_terracotta");
        assertFamily(Target.PLACEMENT_GRINDSTONE, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:grindstone");
        assertFamily(Target.PLACEMENT_FENCE_GATE, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:oak_fence_gate");
        assertFamily(Target.PLACEMENT_FROGLIGHT, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:ochre_froglight");
        assertFamily(Target.PLACEMENT_SLAB, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:stone_slab");
        assertFamily(Target.PLACEMENT_STAIRS, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:oak_stairs");
        assertFamily(Target.PLACEMENT_TRAPDOOR, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:oak_trapdoor");
        assertFamily(Target.PLACEMENT_LOG_WOOD, BlockInspectionCategory.PLACEMENT_GUIDE,
                "minecraft:oak_log",
                "minecraft:oak_wood",
                "minecraft:crimson_stem",
                "minecraft:crimson_hyphae",
                "minecraft:bamboo_block",
                "minecraft:stripped_bamboo_block");
    }

    @Test
    void materialOrePairsAndNetherResourcesFollowTheirSwitches() {
        assertFamily(Target.MATERIAL_OBSIDIAN, BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                "minecraft:obsidian");
        assertFamily(Target.MATERIAL_CRYING_OBSIDIAN, BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                "minecraft:crying_obsidian");
        assertFamily(Target.MATERIAL_ANCIENT_DEBRIS, BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                "minecraft:ancient_debris");
        assertFamily(Target.MATERIAL_NETHER_GOLD_ORE, BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                "minecraft:nether_gold_ore");
        assertFamily(Target.MATERIAL_NETHER_QUARTZ_ORE, BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                "minecraft:nether_quartz_ore");
        assertOrePair(Target.MATERIAL_DIAMOND_ORE, "diamond");
        assertOrePair(Target.MATERIAL_GOLD_ORE, "gold");
        assertOrePair(Target.MATERIAL_EMERALD_ORE, "emerald");
        assertOrePair(Target.MATERIAL_COAL_ORE, "coal");
        assertOrePair(Target.MATERIAL_IRON_ORE, "iron");
        assertOrePair(Target.MATERIAL_COPPER_ORE, "copper");
        assertOrePair(Target.MATERIAL_LAPIS_ORE, "lapis");
        assertOrePair(Target.MATERIAL_REDSTONE_ORE, "redstone");
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                "minecraft:netherrack",
                BlockInspectionCategory.MATERIAL_HIGHLIGHT));
    }

    @Test
    void hiddenSurfaceFamiliesFollowTheirOwnSwitches() {
        assertFamily(Target.HIDDEN_BLUE_ICE, BlockInspectionCategory.HIDDEN_SURFACE,
                "minecraft:blue_ice");
        assertFamily(Target.HIDDEN_POWDER_SNOW, BlockInspectionCategory.HIDDEN_SURFACE,
                "minecraft:powder_snow");
        assertFamily(Target.HIDDEN_SCULK_CATALYST, BlockInspectionCategory.HIDDEN_SURFACE,
                "minecraft:sculk_catalyst");
        assertFamily(Target.HIDDEN_DEAD_CORAL, BlockInspectionCategory.HIDDEN_SURFACE,
                "minecraft:dead_brain_coral",
                "minecraft:dead_brain_coral_block",
                "minecraft:dead_brain_coral_fan",
                "minecraft:dead_brain_coral_wall_fan");
    }

    @Test
    void singletonCategoriesStayGroupControlledAndInvalidInputsAreRejected() {
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                0, "minecraft:tripwire", BlockInspectionCategory.TECHNICAL_TRACE));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                0, "minecraft:red_stained_glass", BlockInspectionCategory.GLASS_INSPECTION));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                0, "minecraft:netherrack", BlockInspectionCategory.NETHER_PALETTE));

        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                "  MINECRAFT:ANVIL  ",
                BlockInspectionCategory.PLACEMENT_GUIDE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                "minecraft:stone",
                BlockInspectionCategory.PLACEMENT_GUIDE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                "minecraft:stone",
                BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                "minecraft:dead_brain_coral_wall",
                BlockInspectionCategory.HIDDEN_SURFACE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                null,
                BlockInspectionCategory.PLACEMENT_GUIDE));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                "minecraft:anvil",
                null));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                "minecraft:anvil",
                BlockInspectionCategory.NONE));
    }

    private static void assertOrePair(Target target, String ore) {
        assertFamily(target, BlockInspectionCategory.MATERIAL_HIGHLIGHT,
                "minecraft:" + ore + "_ore",
                "minecraft:deepslate_" + ore + "_ore");
    }

    private static void assertFamily(
            Target target,
            BlockInspectionCategory category,
            String... ids) {
        int disabledMask = VisualTargetSelectionPolicy.withEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK, target, false);
        for (String id : ids) {
            assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                    VisualTargetSelectionPolicy.ALL_TARGETS_MASK, id, category), id);
            assertFalse(VisualTargetSelectionPolicy.matchesEnabled(disabledMask, id, category), id);
        }
    }
}
