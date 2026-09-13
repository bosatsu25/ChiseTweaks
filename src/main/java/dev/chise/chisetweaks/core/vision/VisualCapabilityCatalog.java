package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Single target-ownership catalog for the retained P0 visual domains.
 *
 * <p>Feature switches, renderers and inspectors may present these targets differently, but they
 * must not maintain independent block-id lists. Vanilla ore family/style metadata remains owned by
 * {@link VanillaOreVisualCatalog}; this catalog composes it with the retained material, hidden,
 * technical and Nether scopes.</p>
 */
public final class VisualCapabilityCatalog {
    private static final Set<String> TECHNICAL_TRACE_IDS = Set.of(
            "minecraft:tripwire",
            "minecraft:tripwire_hook");

    private static final Set<String> MATERIAL_HIGHLIGHT_IDS = createMaterialHighlightIds();

    private static final Set<String> NETHER_PALETTE_IDS = Set.of(
            "minecraft:netherrack",
            "minecraft:gravel",
            "minecraft:soul_sand",
            "minecraft:soul_soil",
            "minecraft:magma_block",
            "minecraft:glowstone",
            "minecraft:shroomlight",
            "minecraft:crimson_nylium",
            "minecraft:warped_nylium",
            "minecraft:crimson_stem",
            "minecraft:warped_stem",
            "minecraft:nether_wart_block",
            "minecraft:warped_wart_block",
            "minecraft:basalt",
            "minecraft:polished_basalt",
            "minecraft:blackstone",
            "minecraft:gilded_blackstone",
            "minecraft:polished_blackstone",
            "minecraft:chiseled_polished_blackstone",
            "minecraft:polished_blackstone_bricks",
            "minecraft:cracked_polished_blackstone_bricks",
            "minecraft:nether_bricks",
            "minecraft:chiseled_nether_bricks",
            "minecraft:cracked_nether_bricks",
            "minecraft:nether_gold_ore",
            "minecraft:nether_quartz_ore",
            "minecraft:crying_obsidian");

    private VisualCapabilityCatalog() {}

    public static Set<BlockInspectionCategory> categories(String rawBlockId) {
        String id = normalize(rawBlockId);
        if (id.isEmpty()) return Set.of();

        EnumSet<BlockInspectionCategory> result = EnumSet.noneOf(BlockInspectionCategory.class);
        for (BlockInspectionCategory category : BlockInspectionCategory.values()) {
            if (category != BlockInspectionCategory.NONE && matchesNormalized(id, category)) {
                result.add(category);
            }
        }
        return Set.copyOf(result);
    }

    public static boolean matches(String rawBlockId, BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return false;
        String id = normalize(rawBlockId);
        return !id.isEmpty() && matchesNormalized(id, category);
    }

    public static Target selectableTarget(String rawBlockId, BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return null;
        String id = normalize(rawBlockId);
        if (id.isEmpty()) return null;

        return switch (category) {
            case TECHNICAL_TRACE -> technicalTarget(id);
            case HIDDEN_SURFACE -> hiddenTarget(id);
            case MATERIAL_HIGHLIGHT -> materialTarget(id);
            case NETHER_PALETTE, NONE -> null;
        };
    }

    public static Set<String> materialHighlightIds() {
        return MATERIAL_HIGHLIGHT_IDS;
    }

    public static Set<String> netherPaletteIds() {
        return NETHER_PALETTE_IDS;
    }

    private static boolean matchesNormalized(String id, BlockInspectionCategory category) {
        return switch (category) {
            case TECHNICAL_TRACE -> TECHNICAL_TRACE_IDS.contains(id);
            case HIDDEN_SURFACE -> hiddenTarget(id) != null;
            case MATERIAL_HIGHLIGHT -> MATERIAL_HIGHLIGHT_IDS.contains(id);
            case NETHER_PALETTE -> NETHER_PALETTE_IDS.contains(id);
            case NONE -> false;
        };
    }

    private static Target technicalTarget(String id) {
        if (id.equals("minecraft:tripwire")) return Target.TECHNICAL_TRIPWIRE;
        if (id.equals("minecraft:tripwire_hook")) return Target.TECHNICAL_TRIPWIRE_HOOK;
        return null;
    }

    private static Target materialTarget(String id) {
        if (id.equals("minecraft:obsidian")) return Target.MATERIAL_OBSIDIAN;
        if (id.equals("minecraft:crying_obsidian")) return Target.MATERIAL_CRYING_OBSIDIAN;
        return VanillaOreVisualCatalog.targetForBlockId(id);
    }

    private static Target hiddenTarget(String id) {
        if (id.equals("minecraft:blue_ice")) return Target.HIDDEN_BLUE_ICE;
        if (id.equals("minecraft:powder_snow")) return Target.HIDDEN_POWDER_SNOW;
        if (id.equals("minecraft:sculk_catalyst")) return Target.HIDDEN_SCULK_CATALYST;
        return isDeadCoral(id) ? Target.HIDDEN_DEAD_CORAL : null;
    }

    private static boolean isDeadCoral(String id) {
        return id.contains(":dead_") && (id.endsWith("_coral_block")
                || id.endsWith("_coral")
                || id.endsWith("_coral_fan")
                || id.endsWith("_coral_wall_fan"));
    }

    private static Set<String> createMaterialHighlightIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>(VanillaOreVisualCatalog.blockIds());
        ids.add("minecraft:obsidian");
        ids.add("minecraft:crying_obsidian");
        return Set.copyOf(ids);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
