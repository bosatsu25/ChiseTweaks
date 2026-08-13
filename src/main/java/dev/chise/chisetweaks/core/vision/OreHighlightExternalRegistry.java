package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Shared runtime registry behind config-driven compatibility and the public Ore Highlight API. */
public final class OreHighlightExternalRegistry {
    private static final ConcurrentHashMap<String, OreHighlightStyle> API_BLOCKS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<TagRegistration> API_TAGS = new CopyOnWriteArrayList<>();
    private static volatile Map<String, OreHighlightStyle> configBlocks = Map.of();

    private OreHighlightExternalRegistry() {}

    public static void registerApiBlock(String blockId, OreHighlightStyle style) {
        String normalized = ModdedOreIdPolicy.normalize(blockId);
        if (normalized.isEmpty() || style == null) {
            throw new IllegalArgumentException("blockId and style are required");
        }
        API_BLOCKS.put(normalized, style);
        OreHighlightResolver.invalidateCache();
    }

    public static void registerApiTag(TagKey<Block> tag, OreHighlightStyle style) {
        if (tag == null || style == null) throw new IllegalArgumentException("tag and style are required");
        API_TAGS.removeIf(existing -> existing.tag().equals(tag));
        API_TAGS.add(new TagRegistration(tag, style));
        OreHighlightResolver.invalidateCache();
    }

    public static void replaceConfigBlocks(Map<String, OreHighlightStyle> entries) {
        configBlocks = entries == null ? Map.of() : Map.copyOf(entries);
        OreHighlightResolver.invalidateCache();
    }

    public static @Nullable OreHighlightStyle styleForBlockId(String blockId) {
        String normalized = ModdedOreIdPolicy.normalize(blockId);
        OreHighlightStyle configured = configBlocks.get(normalized);
        return configured != null ? configured : API_BLOCKS.get(normalized);
    }

    public static @Nullable OreHighlightStyle styleForApiTag(BlockState state) {
        if (state == null) return null;
        for (TagRegistration registration : API_TAGS) {
            if (state.is(registration.tag())) return registration.style();
        }
        return null;
    }

    public static boolean hasExplicitBlock(String blockId) {
        String normalized = ModdedOreIdPolicy.normalize(blockId);
        return configBlocks.containsKey(normalized) || API_BLOCKS.containsKey(normalized);
    }

    public static List<TagRegistration> apiTagRegistrations() {
        return List.copyOf(API_TAGS);
    }

    public record TagRegistration(TagKey<Block> tag, OreHighlightStyle style) {}
}
