package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualModelSelectionPolicyTest {
    @Test
    void parentFeatureMustBeEnabled() {
        assertEquals(0, VisualModelSelectionPolicy.activeMaterialModelMask(
                false,
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK));
    }

    @Test
    void materialMaskContainsExactlyTheThirteenMaterialFamilies() {
        assertEquals(13, Integer.bitCount(VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK));
        assertTrue(VisualModelSelectionPolicy.useMaterialTarget(
                VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK,
                Target.MATERIAL_DIAMOND_ORE));
        assertTrue(VisualModelSelectionPolicy.useMaterialTarget(
                VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK,
                Target.MATERIAL_OBSIDIAN));
        assertTrue(VisualModelSelectionPolicy.useMaterialTarget(
                VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK,
                Target.MATERIAL_CRYING_OBSIDIAN));
        assertTrue(VisualModelSelectionPolicy.useMaterialTarget(
                VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK,
                Target.MATERIAL_NETHER_GOLD_ORE));
        assertTrue(VisualModelSelectionPolicy.useMaterialTarget(
                VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK,
                Target.MATERIAL_NETHER_QUARTZ_ORE));
        assertFalse(VisualModelSelectionPolicy.useMaterialTarget(
                VisualModelSelectionPolicy.MATERIAL_MODEL_TARGET_MASK,
                Target.HIDDEN_POWDER_SNOW));
    }

    @Test
    void disabledMaterialTargetIsRemovedFromModelMask() {
        int configured = VisualTargetSelectionPolicy.withEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                Target.MATERIAL_NETHER_QUARTZ_ORE,
                false);
        int active = VisualModelSelectionPolicy.activeMaterialModelMask(true, configured);

        assertFalse(VisualModelSelectionPolicy.useMaterialTarget(active, Target.MATERIAL_NETHER_QUARTZ_ORE));
        assertTrue(VisualModelSelectionPolicy.useMaterialTarget(active, Target.MATERIAL_DIAMOND_ORE));
    }

    @Test
    void diamondCompatibilityHelperUsesTheSharedMaterialPolicy() {
        assertTrue(VisualModelSelectionPolicy.useDiamondOreModel(
                true,
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK));

        int withoutDiamond = VisualTargetSelectionPolicy.withEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                Target.MATERIAL_DIAMOND_ORE,
                false);
        assertFalse(VisualModelSelectionPolicy.useDiamondOreModel(true, withoutDiamond));
    }
}
