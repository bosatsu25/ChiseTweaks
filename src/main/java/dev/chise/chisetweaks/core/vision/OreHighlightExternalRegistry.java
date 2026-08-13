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
import java.util.concurrent.atomic.AtomicLong;

/** Shared runtime registry behind pack/config-driven compatibility and the public Ore Highlight API. */
public final class OreHighlightExternalRegistry {
    private static final ConcurrentHashMap<String, OreHighlightStyle> API_BLOCKS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<TagRegistration> API_TAGS = new CopyOnWriteArrayList<>();
    private static final AtomicLong REVISION = new AtomicLong();
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

    /** Replaces definitions supplied by active resource packs/modpack resources. */
    public static void replaceResourceBlocks(Map<String, OreHighlightStyle> entries) {
        resourceBlocks = sanitizedCopy(entries);
        changed();
    }

    /** Replaces explicit per-user definitions from the local compatibility editor/config. */
    public static void replaceConfigBlocks(Map<String, OreHighlightStyle> entries) {
        configBlocks = sanitizedCopy(entries);
        changed();
    }

    /**
     * Explicit block precedence is local user override > resource/modpack definition > public API.
     * Tag/convention/fallback resolution is handled later by {@link OreHighlightResolver}.
     */
    public static @Nullable OreHighlightStyle styleForBlockId(String blockId) {
        String normalized = ModdedOreIdPolicy.normalize(blockId);
        OreHighlightStyle configured = configBlocks.get(normalized);
        if (configured != null) return configured;
        OreHighlightStyle resource = resourceBlocks.get(normalized);
        return resource != null ? resource : API_BLOCKS.get(normalized);
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
        return configBlocks.containsKey(normalized)
                || resourceBlocks.containsKey(normalized)
                || API_BLOCKS.containsKey(normalized);
    }

    public static long revision() {
        return REVISION.get();
    }

    public static Map<String, OreHighlightStyle> resourceBlockEntries() {
        return resourceBlocks;
    }

    public static Map<String, OreHighlightStyle> configBlockEntries() {
        return configBlocks;
    }

    public static List<TagRegistration> apiTagRegistrations() {
        return List.copyOf(API_TAGS);
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
        REVISION.incrementAndGet();
        OreHighlightResolver.invalidateCache();
    }

    public record TagRegistration(TagKey<Block> tag, OreHighlightStyle style) {}
}
