package dev.chise.chisetweaks.core.vision;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure classification and bounded state-summary policy for visual inspection. */
public final class BlockInspectionPolicy {
    private static final int MAX_DETAILS = 8;
    private static final int MAX_VALUE_LENGTH = 48;
    private static final Pattern BLOCK_ID_PATTERN =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern SAFE_TOKEN_PATTERN =
            Pattern.compile("[a-z0-9_.-]+");

    private static final Set<String> TECHNICAL_PROPERTIES = Set.of(
            "attached", "disarmed", "powered", "facing", "north", "east", "south", "west");
    private static final Set<String> HIDDEN_PROPERTIES = Set.of(
            "bloom", "waterlogged", "facing");
    private static final Set<String> GLASS_PROPERTIES = Set.of(
            "north", "east", "south", "west", "waterlogged");
    private static final Set<String> PLACEMENT_PROPERTIES = Set.of(
            "facing", "axis", "half", "type", "shape", "face", "open",
            "lit", "honey_level", "in_wall", "powered");

    private static final Set<String> MATERIAL_HIGHLIGHT_IDS = Set.of(
            "minecraft:obsidian",
            "minecraft:crying_obsidian",
            "minecraft:ancient_debris",
            "minecraft:nether_gold_ore",
            "minecraft:nether_quartz_ore",
            "minecraft:coal_ore",
            "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore",
            "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore",
            "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore",
            "minecraft:deepslate_gold_ore",
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:deepslate_emerald_ore",
            "minecraft:lapis_ore",
            "minecraft:deepslate_lapis_ore",
            "minecraft:redstone_ore",
            "minecraft:deepslate_redstone_ore");

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

    private BlockInspectionPolicy() {}

    public static InspectionPresentation inspect(String rawBlockId, Map<String, String> rawProperties) {
        String blockId = normalizeBlockId(rawBlockId);
        return inspect(blockId, rawProperties, classify(blockId));
    }

    public static InspectionPresentation inspect(
            String rawBlockId,
            Map<String, String> rawProperties,
            BlockInspectionCategory category) {
        String blockId = normalizeBlockId(rawBlockId);
        Map<String, String> properties = rawProperties == null ? Map.of() : rawProperties;
        if (category == null || category == BlockInspectionCategory.NONE
                || !matchesNormalized(blockId, category)) {
            return InspectionPresentation.none(blockId);
        }

        Set<String> allowed = switch (category) {
            case TECHNICAL_TRACE -> TECHNICAL_PROPERTIES;
            case HIDDEN_SURFACE -> HIDDEN_PROPERTIES;
            case GLASS_INSPECTION -> GLASS_PROPERTIES;
            case PLACEMENT_GUIDE -> PLACEMENT_PROPERTIES;
            case MATERIAL_HIGHLIGHT, NETHER_PALETTE, NONE -> Set.of();
        };

        ArrayList<String> details = new ArrayList<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (details.size() >= MAX_DETAILS) break;
            String key = safeToken(entry.getKey());
            String value = safeToken(entry.getValue());
            if (!key.isEmpty() && !value.isEmpty() && allowed.contains(key)) {
                details.add(key + "=" + value);
            }
        }
        details.sort(String::compareTo);
        return new InspectionPresentation(
                blockId, category, List.copyOf(details), category.argb());
    }

    public static BlockInspectionCategory classify(String rawBlockId) {
        Set<BlockInspectionCategory> categories = categories(rawBlockId);
        if (categories.isEmpty()) return BlockInspectionCategory.NONE;
        for (BlockInspectionCategory category : BlockInspectionCategory.values()) {
            if (category != BlockInspectionCategory.NONE && categories.contains(category)) {
                return category;
            }
        }
        return BlockInspectionCategory.NONE;
    }

    public static Set<BlockInspectionCategory> categories(String rawBlockId) {
        String id = normalizeBlockId(rawBlockId);
        if (id.isEmpty()) return Set.of();
        EnumSet<BlockInspectionCategory> result =
                EnumSet.noneOf(BlockInspectionCategory.class);
        if (isTechnical(id)) result.add(BlockInspectionCategory.TECHNICAL_TRACE);
        if (isHiddenSurface(id)) result.add(BlockInspectionCategory.HIDDEN_SURFACE);
        if (isGlass(id)) result.add(BlockInspectionCategory.GLASS_INSPECTION);
        if (isPlacementGuide(id)) result.add(BlockInspectionCategory.PLACEMENT_GUIDE);
        if (MATERIAL_HIGHLIGHT_IDS.contains(id)) result.add(BlockInspectionCategory.MATERIAL_HIGHLIGHT);
        if (NETHER_PALETTE_IDS.contains(id)) result.add(BlockInspectionCategory.NETHER_PALETTE);
        return Set.copyOf(result);
    }

    public static boolean matches(String rawBlockId, BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return false;
        return matchesNormalized(normalizeBlockId(rawBlockId), category);
    }

    private static boolean matchesNormalized(String id, BlockInspectionCategory category) {
        if (id.isEmpty()) return false;
        return switch (category) {
            case TECHNICAL_TRACE -> isTechnical(id);
            case HIDDEN_SURFACE -> isHiddenSurface(id);
            case GLASS_INSPECTION -> isGlass(id);
            case PLACEMENT_GUIDE -> isPlacementGuide(id);
            case MATERIAL_HIGHLIGHT -> MATERIAL_HIGHLIGHT_IDS.contains(id);
            case NETHER_PALETTE -> NETHER_PALETTE_IDS.contains(id);
            case NONE -> false;
        };
    }

    public static boolean isScanCategory(BlockInspectionCategory category) {
        return category != null && category != BlockInspectionCategory.NONE;
    }

    public static Set<String> materialHighlightIds() { return MATERIAL_HIGHLIGHT_IDS; }
    public static Set<String> netherPaletteIds() { return NETHER_PALETTE_IDS; }

    private static boolean isTechnical(String id) {
        return id.equals("minecraft:tripwire") || id.equals("minecraft:tripwire_hook");
    }

    private static boolean isHiddenSurface(String id) {
        return id.equals("minecraft:powder_snow")
                || id.equals("minecraft:blue_ice")
                || id.equals("minecraft:sculk_catalyst")
                || id.contains(":dead_") && (id.endsWith("_coral_block")
                || id.endsWith("_coral") || id.endsWith("_coral_fan")
                || id.endsWith("_coral_wall_fan"));
    }

    private static boolean isGlass(String id) {
        return id.equals("minecraft:glass")
                || id.equals("minecraft:glass_pane")
                || id.equals("minecraft:tinted_glass")
                || id.endsWith("_stained_glass")
                || id.endsWith("_stained_glass_pane");
    }

    private static boolean isPlacementGuide(String id) {
        if (!id.startsWith("minecraft:")) return false;
        return id.equals("minecraft:anvil")
                || id.equals("minecraft:beehive")
                || id.equals("minecraft:campfire")
                || id.equals("minecraft:soul_campfire")
                || id.equals("minecraft:grindstone")
                || id.equals("minecraft:bamboo_block")
                || id.equals("minecraft:stripped_bamboo_block")
                || id.endsWith("_fence_gate")
                || id.endsWith("_froglight")
                || id.endsWith("_glazed_terracotta")
                || id.endsWith("_slab")
                || id.endsWith("_stairs")
                || id.endsWith("_trapdoor")
                || id.endsWith("_log")
                || id.endsWith("_wood")
                || id.endsWith("_stem")
                || id.endsWith("_hyphae");
    }

    private static String normalizeBlockId(String raw) {
        if (raw == null) return "";
        String normalized = Normalizer.normalize(
                raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        if (normalized.length() > 256
                || !BLOCK_ID_PATTERN.matcher(normalized).matches()) return "";
        return normalized;
    }

    private static String safeToken(String raw) {
        if (raw == null) return "";
        String value = Normalizer.normalize(
                raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        if (value.length() > MAX_VALUE_LENGTH
                || !SAFE_TOKEN_PATTERN.matcher(value).matches()) return "";
        return value;
    }

    public record InspectionPresentation(
            String blockId,
            BlockInspectionCategory category,
            List<String> details,
            int argb) {
        public InspectionPresentation {
            blockId = Objects.requireNonNull(blockId, "blockId");
            category = Objects.requireNonNull(category, "category");
            details = List.copyOf(Objects.requireNonNull(details, "details"));
            if (details.size() > MAX_DETAILS) throw new IllegalArgumentException("too many details");
        }

        private static InspectionPresentation none(String blockId) {
            return new InspectionPresentation(
                    blockId, BlockInspectionCategory.NONE, List.of(), 0xFFFFFFFF);
        }

        public String compactDetails() {
            return details.isEmpty() ? "" : String.join(", ", details);
        }
    }
}
