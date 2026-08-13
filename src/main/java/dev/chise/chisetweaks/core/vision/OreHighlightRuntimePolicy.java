package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

/** Pure runtime policy for Ore Highlight visibility and reduced-motion style selection. */
public final class OreHighlightRuntimePolicy {
    public enum Motion {
        STATIC,
        ANIMATED
    }

    private OreHighlightRuntimePolicy() {}

    /** Returns true only when the master switch and the requested material target are both enabled. */
    public static boolean shouldRender(boolean masterEnabled, int targetMask, Target target) {
        return masterEnabled
                && target != null
                && (target.bitMask() & VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK) != 0
                && VisualTargetSelectionPolicy.isEnabled(targetMask, target);
    }

    /** Static is the reduced-motion default; animation is always explicit opt-in. */
    public static Motion motion(boolean animationEnabled) {
        return animationEnabled ? Motion.ANIMATED : Motion.STATIC;
    }
}
