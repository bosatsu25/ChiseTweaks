package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

/**
 * Pure selection policy for visual-assistance features that are rendered through Minecraft's
 * block-model pipeline instead of the bounded world-line overlay.
 */
public final class VisualModelSelectionPolicy {
    private VisualModelSelectionPolicy() {}

    /**
     * Diamond ore is the first model-loading PoC. Both normal and deepslate variants share the
     * existing Material Highlights parent switch and the existing diamond target filter.
     */
    public static boolean useDiamondOreModel(boolean materialHighlightsEnabled, int visualTargetMask) {
        return materialHighlightsEnabled
                && VisualTargetSelectionPolicy.isEnabled(
                        visualTargetMask,
                        Target.MATERIAL_DIAMOND_ORE);
    }
}
