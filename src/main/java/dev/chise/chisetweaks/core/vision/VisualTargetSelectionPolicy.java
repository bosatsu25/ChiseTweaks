package dev.chise.chisetweaks.core.vision;

import java.util.Locale;

/** Fine-grained persisted selection policy for the retained visual target families. */
public final class VisualTargetSelectionPolicy {
    /**
     * Retained targets keep their historical bit positions so existing Ore Highlights and Hidden
     * Surface Trace selections survive the scope reset. Removed Placement Guide bits 0-10 are never
     * accepted by the new mask.
     */
    public enum Target {
        MATERIAL_OBSIDIAN(11),
        MATERIAL_ANCIENT_DEBRIS(12),
        MATERIAL_DIAMOND_ORE(13),
        MATERIAL_GOLD_ORE(14),
        MATERIAL_EMERALD_ORE(15),
        MATERIAL_COAL_ORE(16),
        MATERIAL_IRON_ORE(17),
        MATERIAL_COPPER_ORE(18),
        MATERIAL_LAPIS_ORE(19),
        MATERIAL_REDSTONE_ORE(20),
        HIDDEN_BLUE_ICE(21),
        HIDDEN_DEAD_CORAL(22),
        HIDDEN_POWDER_SNOW(23),
        HIDDEN_SCULK_CATALYST(24),
        MATERIAL_CRYING_OBSIDIAN(25),
        MATERIAL_NETHER_GOLD_ORE(26),
        MATERIAL_NETHER_QUARTZ_ORE(27);

        private final int bit;

        Target(int bit) { this.bit = bit; }

        public int bitMask() { return 1 << bit; }
    }

    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final int NEW_NETHER_TARGETS_MASK =
            Target.MATERIAL_CRYING_OBSIDIAN.bitMask()
                    | Target.MATERIAL_NETHER_GOLD_ORE.bitMask()
                    | Target.MATERIAL_NETHER_QUARTZ_ORE.bitMask();

    public static final int ORE_HIGHLIGHT_TARGETS_MASK =
            Target.MATERIAL_OBSIDIAN.bitMask()
                    | Target.MATERIAL_CRYING_OBSIDIAN.bitMask()
                    | Target.MATERIAL_ANCIENT_DEBRIS.bitMask()
                    | Target.MATERIAL_DIAMOND_ORE.bitMask()
                    | Target.MATERIAL_GOLD_ORE.bitMask()
                    | Target.MATERIAL_EMERALD_ORE.bitMask()
                    | Target.MATERIAL_COAL_ORE.bitMask()
                    | Target.MATERIAL_IRON_ORE.bitMask()
                    | Target.MATERIAL_COPPER_ORE.bitMask()
                    | Target.MATERIAL_LAPIS_ORE.bitMask()
                    | Target.MATERIAL_REDSTONE_ORE.bitMask()
                    | Target.MATERIAL_NETHER_GOLD_ORE.bitMask()
                    | Target.MATERIAL_NETHER_QUARTZ_ORE.bitMask();

    public static final int HIDDEN_SURFACE_TARGETS_MASK =
            Target.HIDDEN_BLUE_ICE.bitMask()
                    | Target.HIDDEN_DEAD_CORAL.bitMask()
                    | Target.HIDDEN_POWDER_SNOW.bitMask()
                    | Target.HIDDEN_SCULK_CATALYST.bitMask();

    public static final int ALL_TARGETS_MASK =
            ORE_HIGHLIGHT_TARGETS_MASK | HIDDEN_SURFACE_TARGETS_MASK;

    private VisualTargetSelectionPolicy() {}

    public static int sanitizeMask(int mask) {
        return mask & ALL_TARGETS_MASK;
    }

    public static int migrateMask(int mask, int schemaVersion) {
        int sanitized = sanitizeMask(mask);
        return schemaVersion < CURRENT_SCHEMA_VERSION
                ? sanitized | NEW_NETHER_TARGETS_MASK
                : sanitized;
    }

    public static boolean isEnabled(int mask, Target target) {
        return target != null && (sanitizeMask(mask) & target.bitMask()) != 0;
    }

    public static int withEnabled(int mask, Target target, boolean enabled) {
        int sanitized = sanitizeMask(mask);
        if (target == null) return sanitized;
        return enabled ? sanitized | target.bitMask() : sanitized & ~target.bitMask();
    }

    public static boolean isOreHighlightTarget(Target target) {
        return target != null && (target.bitMask() & ORE_HIGHLIGHT_TARGETS_MASK) != 0;
    }

    public static int withAllOreHighlightTargets(int mask, boolean enabled) {
        int sanitized = sanitizeMask(mask);
        return enabled
                ? sanitized | ORE_HIGHLIGHT_TARGETS_MASK
                : sanitized & ~ORE_HIGHLIGHT_TARGETS_MASK;
    }

    public static int withOnlyOreHighlightTarget(int mask, Target target) {
        int sanitized = sanitizeMask(mask);
        if (!isOreHighlightTarget(target)) return sanitized;
        return (sanitized & ~ORE_HIGHLIGHT_TARGETS_MASK) | target.bitMask();
    }

    public static boolean matchesEnabled(
            int mask,
            String rawBlockId,
            BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return false;
        String id = normalize(rawBlockId);
        if (id.isEmpty()) return false;

        return switch (category) {
            case TECHNICAL_TRACE, NETHER_PALETTE -> true;
            case MATERIAL_HIGHLIGHT -> materialEnabled(mask, id);
            case HIDDEN_SURFACE -> hiddenEnabled(mask, id);
            case NONE -> false;
        };
    }

    private static boolean materialEnabled(int mask, String id) {
        if (id.equals("minecraft:obsidian")) return isEnabled(mask, Target.MATERIAL_OBSIDIAN);
        if (id.equals("minecraft:crying_obsidian")) {
            return isEnabled(mask, Target.MATERIAL_CRYING_OBSIDIAN);
        }
        Target vanillaOreTarget = VanillaOreVisualCatalog.targetForBlockId(id);
        return vanillaOreTarget != null && isEnabled(mask, vanillaOreTarget);
    }

    private static boolean hiddenEnabled(int mask, String id) {
        if (id.equals("minecraft:blue_ice")) return isEnabled(mask, Target.HIDDEN_BLUE_ICE);
        if (id.equals("minecraft:powder_snow")) return isEnabled(mask, Target.HIDDEN_POWDER_SNOW);
        if (id.equals("minecraft:sculk_catalyst")) {
            return isEnabled(mask, Target.HIDDEN_SCULK_CATALYST);
        }
        if (id.contains(":dead_") && (id.endsWith("_coral_block")
                || id.endsWith("_coral") || id.endsWith("_coral_fan")
                || id.endsWith("_coral_wall_fan"))) {
            return isEnabled(mask, Target.HIDDEN_DEAD_CORAL);
        }
        return false;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
