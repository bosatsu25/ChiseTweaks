package dev.chise.chisetweaks;

import dev.chise.chisetweaks.core.vision.VisualModelSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Focused regression oracle for material-vs-hidden target mutation boundaries. */
final class OreHighlightMutationClosureTest {
    @Test
    void hiddenTargetIsRejectedEvenWhenItsBitIsPresentInTheProvidedMask() {
        int hiddenOnlyMask = Target.HIDDEN_BLUE_ICE.bitMask();
        assertFalse(VisualModelSelectionPolicy.useMaterialTarget(
                hiddenOnlyMask, Target.HIDDEN_BLUE_ICE));
    }
}
