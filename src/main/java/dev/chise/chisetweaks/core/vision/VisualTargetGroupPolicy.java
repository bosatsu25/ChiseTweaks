package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

public final class VisualTargetGroupPolicy {
    public enum Group {
        MATERIAL,
        HIDDEN
    }

    public static final int MATERIAL_MASK = VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK;

    public static final int HIDDEN_MASK =
            Target.HIDDEN_BLUE_ICE.bitMask()
                    | Target.HIDDEN_DEAD_CORAL.bitMask()
                    | Target.HIDDEN_POWDER_SNOW.bitMask()
                    | Target.HIDDEN_SCULK_CATALYST.bitMask();

    private VisualTargetGroupPolicy() {}

    public static int maskFor(Group group) {
        if (group == null) return 0;
        return switch (group) {
            case MATERIAL -> MATERIAL_MASK;
            case HIDDEN -> HIDDEN_MASK;
        };
    }

    public static boolean allEnabled(int mask, Group group) {
        int groupMask = maskFor(group);
        return groupMask != 0
                && (VisualTargetSelectionPolicy.sanitizeMask(mask) & groupMask) == groupMask;
    }

    public static int withAll(int mask, Group group, boolean enabled) {
        int sanitized = VisualTargetSelectionPolicy.sanitizeMask(mask);
        int groupMask = maskFor(group);
        if (groupMask == 0) return sanitized;
        return enabled ? sanitized | groupMask : sanitized & ~groupMask;
    }
}
