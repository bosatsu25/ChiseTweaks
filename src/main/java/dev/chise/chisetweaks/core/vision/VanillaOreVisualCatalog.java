package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

 







public final class VanillaOreVisualCatalog {
    private static final List<Family> FAMILIES = List.of(
            family(Target.MATERIAL_COAL_ORE, "coal",
                    "minecraft:coal_ore", "minecraft:deepslate_coal_ore"),
            family(Target.MATERIAL_IRON_ORE, "iron",
                    "minecraft:iron_ore", "minecraft:deepslate_iron_ore"),
            family(Target.MATERIAL_COPPER_ORE, "copper",
                    "minecraft:copper_ore", "minecraft:deepslate_copper_ore"),
            family(Target.MATERIAL_GOLD_ORE, "gold",
                    "minecraft:gold_ore", "minecraft:deepslate_gold_ore"),
            family(Target.MATERIAL_LAPIS_ORE, "lapis",
                    "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"),
            family(Target.MATERIAL_REDSTONE_ORE, "redstone",
                    "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"),
            family(Target.MATERIAL_DIAMOND_ORE, "diamond",
                    "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
            family(Target.MATERIAL_EMERALD_ORE, "emerald",
                    "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"),
            family(Target.MATERIAL_NETHER_GOLD_ORE, "nether_gold",
                    "minecraft:nether_gold_ore"),
            family(Target.MATERIAL_NETHER_QUARTZ_ORE, "nether_quartz",
                    "minecraft:nether_quartz_ore"),
            family(Target.MATERIAL_ANCIENT_DEBRIS, "ancient_debris",
                    "minecraft:ancient_debris"));

    private static final Map<String, Family> FAMILY_BY_BLOCK_ID;
    private static final Set<String> BLOCK_IDS;
    public static final int TARGET_MASK;

    static {
        LinkedHashMap<String, Family> byBlock = new LinkedHashMap<>();
        LinkedHashSet<String> blockIds = new LinkedHashSet<>();
        int targetMask = 0;
        for (Family family : FAMILIES) {
            targetMask |= family.target().bitMask();
            for (String blockId : family.blockIds()) {
                if (byBlock.put(blockId, family) != null) {
                    throw new IllegalStateException("duplicate vanilla ore block id: " + blockId);
                }
                blockIds.add(blockId);
            }
        }
        FAMILY_BY_BLOCK_ID = Map.copyOf(byBlock);
        BLOCK_IDS = Set.copyOf(blockIds);
        TARGET_MASK = targetMask;
    }

    private VanillaOreVisualCatalog() {}

    public static List<Family> families() {
        return FAMILIES;
    }

    public static Set<String> blockIds() {
        return BLOCK_IDS;
    }

    public static int familyCount() {
        return FAMILIES.size();
    }

    public static int blockVariantCount() {
        return BLOCK_IDS.size();
    }

    public static boolean isVanillaOreBlock(String rawBlockId) {
        return targetForBlockId(rawBlockId) != null;
    }

    public static Target targetForBlockId(String rawBlockId) {
        Family family = FAMILY_BY_BLOCK_ID.get(normalize(rawBlockId));
        return family == null ? null : family.target();
    }

    public static String highlightKeyForBlockId(String rawBlockId) {
        Family family = FAMILY_BY_BLOCK_ID.get(normalize(rawBlockId));
        return family == null ? "" : family.highlightKey();
    }

    private static Family family(Target target, String highlightKey, String... blockIds) {
        return new Family(target, highlightKey, List.of(blockIds));
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public record Family(Target target, String highlightKey, List<String> blockIds) {
        public Family {
            if (target == null) throw new IllegalArgumentException("target must not be null");
            if (highlightKey == null || highlightKey.isBlank()) {
                throw new IllegalArgumentException("highlightKey must not be blank");
            }
            blockIds = List.copyOf(blockIds);
            if (blockIds.isEmpty()) throw new IllegalArgumentException("blockIds must not be empty");
            for (String blockId : blockIds) {
                if (blockId == null || !blockId.startsWith("minecraft:")) {
                    throw new IllegalArgumentException("vanilla block id required: " + blockId);
                }
            }
        }
    }
}
