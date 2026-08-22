package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

 
public final class OreHighlightRuntimePolicy {
    public enum Motion {
        STATIC,
        ANIMATED
    }

    private OreHighlightRuntimePolicy() {}

     
    public static boolean shouldRender(boolean masterEnabled, int targetMask, Target target) {
        return masterEnabled
                && target != null
                && (target.bitMask() & VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK) != 0
                && VisualTargetSelectionPolicy.isEnabled(targetMask, target);
    }

     
    public static Motion motion(boolean animationEnabled) {
        return animationEnabled ? Motion.ANIMATED : Motion.STATIC;
    }
}
