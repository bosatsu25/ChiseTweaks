package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class OreHighlightExternalRegistry {
    private static final ConcurrentHashMap<String, OreHighlightStyle> API_BLOCKS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<TagRegistration> API_TAGS = new CopyOnWriteArrayList<>();
    private static volatile Map<String, OreHighlightStyle> resourceBlocks = Map.of();
    private static volatile Map<String, OreHighlightStyle> configBlocks = Map.of();

    private OreHighlightExternalRegistry() {}

    public static void registerApiBlock(String blockId, OreHighlightStyle style) {
        String normalized = ModdedOreIdPolicy.normalize(blockId);
        if (normalized.isEmpty() || style == null) {
            throw new IllegalArgumentException("blockId and style are required");
        }
        API_BLOCKS.put(normalized, style);
        changed();
    }

    public static void registerApiTag(TagKey<Block> tag, OreHighlightStyle style) {
        if (tag == null || style == null) throw new IllegalArgumentException("tag and style are required");
        API_TAGS.removeIf(existing -> existing.tag().equals(tag));
        API_TAGS.add(new TagRegistration(tag, style));
        changed();
    }

    public static void replaceResourceBlocks(Map<String, OreHighlightStyle> entries) {
        resourceBlocks = sanitizedCopy(entries);
        changed();
    }

    public static void replaceConfigBlocks(Map<String, OreHighlightStyle> entries) {
        configBlocks = sanitizedCopy(entries);
        changed();
    }

    static @Nullable OreHighlightStyle styleForBlockId(String blockId) {
        String normalized = ModdedOreIdPolicy.normalize(blockId);
        OreHighlightStyle configured = configBlocks.get(normalized);
        if (configured != null) return configured;
        OreHighlightStyle resource = resourceBlocks.get(normalized);
        return resource != null ? resource : API_BLOCKS.get(normalized);
    }

    static @Nullable OreHighlightStyle styleForApiTag(BlockState state) {
        if (state == null) return null;
        for (TagRegistration registration : API_TAGS) {
            if (state.is(registration.tag())) return registration.style();
        }
        return null;
    }

    private static Map<String, OreHighlightStyle> sanitizedCopy(Map<String, OreHighlightStyle> entries) {
        if (entries == null || entries.isEmpty()) return Map.of();
        ConcurrentHashMap<String, OreHighlightStyle> sanitized = new ConcurrentHashMap<>();
        for (Map.Entry<String, OreHighlightStyle> entry : entries.entrySet()) {
            String normalized = ModdedOreIdPolicy.normalize(entry.getKey());
            if (!normalized.isEmpty() && entry.getValue() != null) {
                sanitized.put(normalized, entry.getValue());
            }
        }
        return Map.copyOf(sanitized);
    }

    private static void changed() {
        OreHighlightResolver.invalidateCache();
    }

    private record TagRegistration(TagKey<Block> tag, OreHighlightStyle style) {}
}
