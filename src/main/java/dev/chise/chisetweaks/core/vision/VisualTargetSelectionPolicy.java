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
        HIDDEN_SCULK_CATALYST(24),

        // Persisted bit indices are append-only. Never insert above these entries or old masks
        // would silently select a different target after an upgrade.
        MATERIAL_CRYING_OBSIDIAN(25),
        MATERIAL_NETHER_GOLD_ORE(26),
        MATERIAL_NETHER_QUARTZ_ORE(27);

        private final int bit;

        Target(int bit) {
            this.bit = bit;
        }

        public int bitMask() {
            return 1 << bit;
        }
    }

    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final int NEW_NETHER_TARGETS_MASK =
            Target.MATERIAL_CRYING_OBSIDIAN.bitMask()
                    | Target.MATERIAL_NETHER_GOLD_ORE.bitMask()
                    | Target.MATERIAL_NETHER_QUARTZ_ORE.bitMask();
    public static final int ALL_TARGETS_MASK = (1 << Target.values().length) - 1;

    /** Only the targets owned by Ore Highlights; placement/hidden bits are deliberately excluded. */
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

    private VisualTargetSelectionPolicy() {}

    public static int sanitizeMask(int mask) {
        return mask & ALL_TARGETS_MASK;
    }

    /**
     * Upgrades a persisted mask without changing the meaning of any pre-existing target bit.
     * Targets introduced after schema v1 are enabled once during migration, then future loads
     * preserve the user's explicit choices through the stored schema version.
     */
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
        return enabled
                ? sanitized | target.bitMask()
                : sanitized & ~target.bitMask();
    }

    public static boolean isOreHighlightTarget(Target target) {
        return target != null && (target.bitMask() & ORE_HIGHLIGHT_TARGETS_MASK) != 0;
    }

    /** Enables or disables every Ore Highlights target without touching other feature bits. */
    public static int withAllOreHighlightTargets(int mask, boolean enabled) {
        int sanitized = sanitizeMask(mask);
        return enabled
                ? sanitized | ORE_HIGHLIGHT_TARGETS_MASK
                : sanitized & ~ORE_HIGHLIGHT_TARGETS_MASK;
    }

    /**
     * Leaves exactly one Ore Highlights resource family enabled while preserving all unrelated
     * visual-target selections. Invalid/non-material targets are a no-op.
     */
    public static int withOnlyOreHighlightTarget(int mask, Target target) {
        int sanitized = sanitizeMask(mask);
        if (!isOreHighlightTarget(target)) return sanitized;
        return (sanitized & ~ORE_HIGHLIGHT_TARGETS_MASK) | target.bitMask();
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
                || id.endsWith("_coral")
                || id.endsWith("_coral_fan")
                || id.endsWith("_coral_wall_fan"))) {
            return isEnabled(mask, Target.HIDDEN_DEAD_CORAL);
        }
        return false;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
