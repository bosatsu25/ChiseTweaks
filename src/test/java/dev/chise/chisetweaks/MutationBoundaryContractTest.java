package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MutationBoundaryContractTest {
    @Test
    void configEntryLengthAcceptsExactlyTheLimitAndRejectsTheNextCharacter() {
        String exact = "a".repeat(ConfigListPolicy.MAX_ENTRY_CHARS);
        String over = exact + "b";
        assertEquals(List.of(exact), ConfigListPolicy.sanitize(List.of(exact)));
        assertEquals(List.of(), ConfigListPolicy.sanitize(List.of(over)));
    }

    @Test
    void everySpecialMaterialBranchCanReturnFalseWhenItsSelectionBitIsOff() {
        int all = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        int noObsidian = VisualTargetSelectionPolicy.withEnabled(all, Target.MATERIAL_OBSIDIAN, false);
        int noCrying = VisualTargetSelectionPolicy.withEnabled(all, Target.MATERIAL_CRYING_OBSIDIAN, false);
        int noDiamond = VisualTargetSelectionPolicy.withEnabled(all, Target.MATERIAL_DIAMOND_ORE, false);

        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                noObsidian, "minecraft:obsidian", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                noCrying, "minecraft:crying_obsidian", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                noDiamond, "minecraft:diamond_ore", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
        assertTrue(VisualTargetSelectionPolicy.matchesEnabled(
                all, "minecraft:obsidian", BlockInspectionCategory.MATERIAL_HIGHLIGHT));
    }

    @Test
    void everyHiddenMaterialBranchCanReturnFalseWhenItsSelectionBitIsOff() {
        int all = VisualTargetSelectionPolicy.ALL_TARGETS_MASK;
        assertHiddenDisabled(all, Target.HIDDEN_BLUE_ICE, "minecraft:blue_ice");
        assertHiddenDisabled(all, Target.HIDDEN_POWDER_SNOW, "minecraft:powder_snow");
        assertHiddenDisabled(all, Target.HIDDEN_SCULK_CATALYST, "minecraft:sculk_catalyst");
        assertHiddenDisabled(all, Target.HIDDEN_DEAD_CORAL, "minecraft:dead_brain_coral_block");
        assertHiddenDisabled(all, Target.HIDDEN_DEAD_CORAL, "minecraft:dead_brain_coral");
        assertHiddenDisabled(all, Target.HIDDEN_DEAD_CORAL, "minecraft:dead_brain_coral_fan");
        assertHiddenDisabled(all, Target.HIDDEN_DEAD_CORAL, "minecraft:dead_brain_coral_wall_fan");
    }

    private static void assertHiddenDisabled(int all, Target target, String blockId) {
        int disabled = VisualTargetSelectionPolicy.withEnabled(all, target, false);
        assertFalse(VisualTargetSelectionPolicy.matchesEnabled(
                disabled, blockId, BlockInspectionCategory.HIDDEN_SURFACE), blockId);
    }
}
