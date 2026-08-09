package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

/** Group-level target mask operations used by compact bulk UI actions. */
public final class VisualTargetGroupPolicy {
    public enum Group {
        PLACEMENT,
        MATERIAL,
        HIDDEN
    }

    public static final int PLACEMENT_MASK =
            Target.PLACEMENT_ANVIL.bitMask()
                    | Target.PLACEMENT_BEEHIVE.bitMask()
                    | Target.PLACEMENT_CAMPFIRE.bitMask()
                    | Target.PLACEMENT_GLAZED_TERRACOTTA.bitMask()
                    | Target.PLACEMENT_GRINDSTONE.bitMask()
                    | Target.PLACEMENT_FENCE_GATE.bitMask()
                    | Target.PLACEMENT_FROGLIGHT.bitMask()
                    | Target.PLACEMENT_SLAB.bitMask()
                    | Target.PLACEMENT_STAIRS.bitMask()
                    | Target.PLACEMENT_TRAPDOOR.bitMask()
                    | Target.PLACEMENT_LOG_WOOD.bitMask();

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
            case PLACEMENT -> PLACEMENT_MASK;
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
