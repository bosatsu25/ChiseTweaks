package dev.chise.chisetweaks.core.vision;

public final class VisualTargetSelectionPolicy {
    /**
     * 既存設定との互換性を守るため、保持対象のビット位置は過去の割り当てを維持する。
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
        MATERIAL_NETHER_QUARTZ_ORE(27),
        TECHNICAL_TRIPWIRE(28),
        TECHNICAL_TRIPWIRE_HOOK(29);

        private final int bit;

        Target(int bit) { this.bit = bit; }

        public int bitMask() { return 1 << bit; }
    }

    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int NETHER_TARGET_SCHEMA_VERSION = 2;
    public static final int CURRENT_SCHEMA_VERSION = 3;
    public static final int NEW_NETHER_TARGETS_MASK =
            Target.MATERIAL_CRYING_OBSIDIAN.bitMask()
                    | Target.MATERIAL_NETHER_GOLD_ORE.bitMask()
                    | Target.MATERIAL_NETHER_QUARTZ_ORE.bitMask();
    public static final int NEW_TECHNICAL_TARGETS_MASK =
            Target.TECHNICAL_TRIPWIRE.bitMask()
                    | Target.TECHNICAL_TRIPWIRE_HOOK.bitMask();

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

    public static final int TECHNICAL_TRACE_TARGETS_MASK = NEW_TECHNICAL_TARGETS_MASK;

    public static final int ALL_TARGETS_MASK =
            ORE_HIGHLIGHT_TARGETS_MASK
                    | HIDDEN_SURFACE_TARGETS_MASK
                    | TECHNICAL_TRACE_TARGETS_MASK;

    private VisualTargetSelectionPolicy() {}

    public static int sanitizeMask(int mask) {
        return mask & ALL_TARGETS_MASK;
    }

    public static int migrateMask(int mask, int schemaVersion) {
        int migrated = sanitizeMask(mask);
        if (schemaVersion < NETHER_TARGET_SCHEMA_VERSION) {
            migrated |= NEW_NETHER_TARGETS_MASK;
        }
        if (schemaVersion < CURRENT_SCHEMA_VERSION) {
            migrated |= NEW_TECHNICAL_TARGETS_MASK;
        }
        return migrated;
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

        Target target = VisualCapabilityCatalog.selectableTarget(rawBlockId, category);
        if (target != null) return isEnabled(mask, target);

        return category == BlockInspectionCategory.NETHER_PALETTE
                && VisualCapabilityCatalog.matches(rawBlockId, category);
    }

    public static Target hiddenTargetForBlockId(String rawBlockId) {
        return VisualCapabilityCatalog.selectableTarget(
                rawBlockId, BlockInspectionCategory.HIDDEN_SURFACE);
    }
}
