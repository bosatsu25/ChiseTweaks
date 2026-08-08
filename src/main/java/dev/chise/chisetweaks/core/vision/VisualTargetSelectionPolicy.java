package dev.chise.chisetweaks.core.vision;

import java.util.Locale;

/**
 * Compact persisted selection policy for fine-grained visual-assistance targets.
 *
 * <p>The parent visual features remain the primary on/off switches. This policy only decides
 * which block families are eligible once a parent feature is enabled. All targets default to
 * enabled so existing configurations keep their previous behaviour.</p>
 */
public final class VisualTargetSelectionPolicy {
    public enum Target {
        PLACEMENT_ANVIL(0),
        PLACEMENT_BEEHIVE(1),
        PLACEMENT_CAMPFIRE(2),
        PLACEMENT_GLAZED_TERRACOTTA(3),
        PLACEMENT_GRINDSTONE(4),
        PLACEMENT_FENCE_GATE(5),
        PLACEMENT_FROGLIGHT(6),
        PLACEMENT_SLAB(7),
        PLACEMENT_STAIRS(8),
        PLACEMENT_TRAPDOOR(9),
        PLACEMENT_LOG_WOOD(10),

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
        HIDDEN_SCULK_CATALYST(24);

        private final int bit;

        Target(int bit) {
            this.bit = bit;
        }

        public int bitMask() {
            return 1 << bit;
        }
    }

    public static final int ALL_TARGETS_MASK = (1 << Target.values().length) - 1;

    private VisualTargetSelectionPolicy() {}

    public static int sanitizeMask(int mask) {
        return mask & ALL_TARGETS_MASK;
    }

    public static boolean isEnabled(int mask, Target target) {
        return target != null && (sanitizeMask(mask) & target.bitMask()) != 0;
    }

    public static int withEnabled(int mask, Target target, boolean enabled) {
        int sanitized = sanitizeMask(mask);
        if (target == null) return sanitized;
        return enabled
                ? sanitized | target.bitMask()
                : sanitized & ~target.bitMask();
    }

    /**
     * Returns whether a block remains eligible for its active visual-assistance category.
     * Categories with no useful sub-selection stay enabled as a group.
     */
    public static boolean matchesEnabled(
            int mask,
            String rawBlockId,
            BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return false;
        String id = normalize(rawBlockId);
        if (id.isEmpty()) return false;

        return switch (category) {
            case TECHNICAL_TRACE, GLASS_INSPECTION, NETHER_PALETTE -> true;
            case PLACEMENT_GUIDE -> placementEnabled(mask, id);
            case MATERIAL_HIGHLIGHT -> materialEnabled(mask, id);
            case HIDDEN_SURFACE -> hiddenEnabled(mask, id);
            case NONE -> false;
        };
    }

    private static boolean placementEnabled(int mask, String id) {
        if (id.equals("minecraft:anvil")) return isEnabled(mask, Target.PLACEMENT_ANVIL);
        if (id.equals("minecraft:beehive")) return isEnabled(mask, Target.PLACEMENT_BEEHIVE);
        if (id.equals("minecraft:campfire") || id.equals("minecraft:soul_campfire")) {
            return isEnabled(mask, Target.PLACEMENT_CAMPFIRE);
        }
        if (id.endsWith("_glazed_terracotta")) {
            return isEnabled(mask, Target.PLACEMENT_GLAZED_TERRACOTTA);
        }
        if (id.equals("minecraft:grindstone")) return isEnabled(mask, Target.PLACEMENT_GRINDSTONE);
        if (id.endsWith("_fence_gate")) return isEnabled(mask, Target.PLACEMENT_FENCE_GATE);
        if (id.endsWith("_froglight")) return isEnabled(mask, Target.PLACEMENT_FROGLIGHT);
        if (id.endsWith("_slab")) return isEnabled(mask, Target.PLACEMENT_SLAB);
        if (id.endsWith("_stairs")) return isEnabled(mask, Target.PLACEMENT_STAIRS);
        if (id.endsWith("_trapdoor")) return isEnabled(mask, Target.PLACEMENT_TRAPDOOR);
        if (id.equals("minecraft:bamboo_block")
                || id.equals("minecraft:stripped_bamboo_block")
                || id.endsWith("_log")
                || id.endsWith("_wood")
                || id.endsWith("_stem")
                || id.endsWith("_hyphae")) {
            return isEnabled(mask, Target.PLACEMENT_LOG_WOOD);
        }
        return false;
    }

    private static boolean materialEnabled(int mask, String id) {
        if (id.equals("minecraft:obsidian")) return isEnabled(mask, Target.MATERIAL_OBSIDIAN);
        if (id.equals("minecraft:ancient_debris")) {
            return isEnabled(mask, Target.MATERIAL_ANCIENT_DEBRIS);
        }
        if (orePair(id, "diamond")) return isEnabled(mask, Target.MATERIAL_DIAMOND_ORE);
        if (orePair(id, "gold")) return isEnabled(mask, Target.MATERIAL_GOLD_ORE);
        if (orePair(id, "emerald")) return isEnabled(mask, Target.MATERIAL_EMERALD_ORE);
        if (orePair(id, "coal")) return isEnabled(mask, Target.MATERIAL_COAL_ORE);
        if (orePair(id, "iron")) return isEnabled(mask, Target.MATERIAL_IRON_ORE);
        if (orePair(id, "copper")) return isEnabled(mask, Target.MATERIAL_COPPER_ORE);
        if (orePair(id, "lapis")) return isEnabled(mask, Target.MATERIAL_LAPIS_ORE);
        if (orePair(id, "redstone")) return isEnabled(mask, Target.MATERIAL_REDSTONE_ORE);
        return false;
    }

    private static boolean hiddenEnabled(int mask, String id) {
        if (id.equals("minecraft:blue_ice")) return isEnabled(mask, Target.HIDDEN_BLUE_ICE);
        if (id.equals("minecraft:powder_snow")) return isEnabled(mask, Target.HIDDEN_POWDER_SNOW);
        if (id.equals("minecraft:sculk_catalyst")) {
            return isEnabled(mask, Target.HIDDEN_SCULK_CATALYST);
        }
        if (id.contains(":dead_") && (id.endsWith("_coral_block")
                || id.endsWith("_coral")
                || id.endsWith("_coral_fan")
                || id.endsWith("_coral_wall_fan"))) {
            return isEnabled(mask, Target.HIDDEN_DEAD_CORAL);
        }
        return false;
    }

    private static boolean orePair(String id, String ore) {
        return id.equals("minecraft:" + ore + "_ore")
                || id.equals("minecraft:deepslate_" + ore + "_ore");
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
