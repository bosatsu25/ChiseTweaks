package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualModelSelectionPolicyTest {
    @Test
    void parentFeatureMustBeEnabled() {
        assertFalse(VisualModelSelectionPolicy.useDiamondOreModel(
                false,
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK));
    }

    @Test
    void diamondTargetMustBeEnabled() {
        int mask = VisualTargetSelectionPolicy.withEnabled(
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                Target.MATERIAL_DIAMOND_ORE,
                false);

        assertFalse(VisualModelSelectionPolicy.useDiamondOreModel(true, mask));
    }

    @Test
    void enabledParentAndDiamondTargetUseModelPath() {
        assertTrue(VisualModelSelectionPolicy.useDiamondOreModel(
                true,
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK));
    }
}
