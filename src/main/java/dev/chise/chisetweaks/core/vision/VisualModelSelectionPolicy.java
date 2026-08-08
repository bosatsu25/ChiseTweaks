package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

/**
 * Pure selection policy for visual-assistance features rendered through Minecraft's block-model
 * pipeline instead of the bounded world-line overlay.
 */
public final class VisualModelSelectionPolicy {
    public static final int MATERIAL_MODEL_TARGET_MASK =
            Target.MATERIAL_OBSIDIAN.bitMask()
                    | Target.MATERIAL_ANCIENT_DEBRIS.bitMask()
                    | Target.MATERIAL_DIAMOND_ORE.bitMask()
                    | Target.MATERIAL_GOLD_ORE.bitMask()
                    | Target.MATERIAL_EMERALD_ORE.bitMask()
                    | Target.MATERIAL_COAL_ORE.bitMask()
                    | Target.MATERIAL_IRON_ORE.bitMask()
                    | Target.MATERIAL_COPPER_ORE.bitMask()
                    | Target.MATERIAL_LAPIS_ORE.bitMask()
                    | Target.MATERIAL_REDSTONE_ORE.bitMask();

    private VisualModelSelectionPolicy() {}

    /** Returns the model-backed material targets that are active for the current config. */
    public static int activeMaterialModelMask(boolean materialHighlightsEnabled, int visualTargetMask) {
        if (!materialHighlightsEnabled) return 0;
        return VisualTargetSelectionPolicy.sanitizeMask(visualTargetMask) & MATERIAL_MODEL_TARGET_MASK;
    }

    /** Returns whether one material family is enabled inside an already-normalized active mask. */
    public static boolean useMaterialTarget(int activeMaterialMask, Target target) {
        return isMaterialTarget(target)
                && VisualTargetSelectionPolicy.isEnabled(activeMaterialMask, target);
    }

    /** Kept as a small compatibility helper for tests/callers that only care about diamond. */
    public static boolean useDiamondOreModel(boolean materialHighlightsEnabled, int visualTargetMask) {
        return useMaterialTarget(
                activeMaterialModelMask(materialHighlightsEnabled, visualTargetMask),
                Target.MATERIAL_DIAMOND_ORE);
    }

    private static boolean isMaterialTarget(Target target) {
        return target != null && (target.bitMask() & MATERIAL_MODEL_TARGET_MASK) != 0;
    }
}
