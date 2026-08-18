package dev.chise.chisetweaks.core.vision;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class OreHighlightExternalRegistry {
    private static final int MAX_API_BLOCKS = 2048;
    private static final int MAX_API_TAGS = 256;
    private static final int MAX_PUBLISHED_BLOCKS = 512;
    private static final ConcurrentHashMap<String, OreHighlightStyle> API_BLOCKS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<TagRegistration> API_TAGS = new CopyOnWriteArrayList<>();
    private static volatile Map<String, OreHighlightStyle> resourceBlocks = Map.of();
    private static volatile Map<String, OreHighlightStyle> configBlocks = Map.of();

    private OreHighlightExternalRegistry() {}

    public static void registerApiBlock(String blockId, OreHighlightStyle style) {
        if (style == null) throw new IllegalArgumentException("style is required");
        String normalized = ModdedOreIdPolicy.normalize(blockId);
        Identifier id = Identifier.tryParse(normalized);
        if (id == null || "minecraft".equals(id.getNamespace())) {
            throw new IllegalArgumentException("blockId must be a valid non-minecraft block id");
        }

        synchronized (API_BLOCKS) {
            if (!API_BLOCKS.containsKey(normalized) && API_BLOCKS.size() >= MAX_API_BLOCKS) {
                throw new IllegalStateException("Ore Highlight API block registration limit exceeded");
            }
            API_BLOCKS.put(normalized, style);
        }
        changed();
    }

    public static void registerApiTag(TagKey<Block> tag, OreHighlightStyle style) {
        if (tag == null || style == null) throw new IllegalArgumentException("tag and style are required");
        synchronized (API_TAGS) {
            boolean replacing = API_TAGS.stream().anyMatch(existing -> existing.tag().equals(tag));
            if (!replacing && API_TAGS.size() >= MAX_API_TAGS) {
                throw new IllegalStateException("Ore Highlight API tag registration limit exceeded");
            }
            API_TAGS.removeIf(existing -> existing.tag().equals(tag));
            API_TAGS.add(new TagRegistration(tag, style));
        }
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
        LinkedHashMap<String, OreHighlightStyle> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, OreHighlightStyle> entry : entries.entrySet()) {
            if (sanitized.size() >= MAX_PUBLISHED_BLOCKS) break;
            if (entry == null || entry.getValue() == null) continue;
            Identifier id = Identifier.tryParse(ModdedOreIdPolicy.normalize(entry.getKey()));
            if (id == null || "minecraft".equals(id.getNamespace())) continue;
            sanitized.put(id.toString(), entry.getValue());
        }
        return Map.copyOf(sanitized);
    }

    private static void changed() {
        OreHighlightResolver.invalidateCache();
    }

    private record TagRegistration(TagKey<Block> tag, OreHighlightStyle style) {}
}
