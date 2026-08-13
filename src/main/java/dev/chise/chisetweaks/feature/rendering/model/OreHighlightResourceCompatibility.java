package dev.chise.chisetweaks.feature.rendering.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Loads optional modpack/resource-pack Ore Highlight mappings before block models are baked. */
final class OreHighlightResourceCompatibility {
    private static final FileToIdConverter FINDER = FileToIdConverter.json("chisetweaks/ore_compat");
    private static final int MAX_FILE_CHARS = 65_536;
    private static final int MAX_ENTRIES = 256;

    private OreHighlightResourceCompatibility() {}

    static CompletableFuture<Map<String, OreHighlightStyle>> load(
            PreparableReloadListener.SharedState sharedState,
            Executor executor) {
        return CompletableFuture.supplyAsync(() -> loadNow(sharedState.resourceManager()), executor);
    }

    private static Map<String, OreHighlightStyle> loadNow(
            net.minecraft.server.packs.resources.ResourceManager resourceManager) {
        Map<Identifier, Resource> resources = FINDER.listMatchingResources(resourceManager);
        List<Map.Entry<Identifier, Resource>> ordered = new ArrayList<>(resources.entrySet());
        ordered.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        LinkedHashMap<String, OreHighlightStyle> result = new LinkedHashMap<>();

        for (Map.Entry<Identifier, Resource> resource : ordered) {
            if (result.size() >= MAX_ENTRIES) break;
            try {
                for (Map.Entry<String, OreHighlightStyle> entry : parse(readBounded(resource.getValue())).entrySet()) {
                    if (result.size() >= MAX_ENTRIES) break;
                    result.put(entry.getKey(), entry.getValue());
                }
            } catch (RuntimeException | java.io.IOException failure) {
                ChiseTweaksClient.LOGGER.warn(
                        "Ignoring invalid Ore Highlight compatibility resource {} after {}",
                        resource.getKey(), failure.getClass().getSimpleName());
            }
        }
        return Map.copyOf(result);
    }

    private static String readBounded(Resource resource) throws java.io.IOException {
        try (BufferedReader reader = resource.openAsReader()) {
            StringBuilder out = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                if (out.length() + read > MAX_FILE_CHARS) {
                    throw new IllegalArgumentException("compatibility resource is too large");
                }
                out.append(buffer, 0, read);
            }
            return out.toString();
        }
    }

    private static Map<String, OreHighlightStyle> parse(String json) {
        StrictJsonSecurityPolicy.Validation validation = StrictJsonSecurityPolicy.validateObjectDocument(json);
        if (!validation.valid()) throw new IllegalArgumentException("unsafe compatibility resource");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonElement rawEntries = root.get("entries");
        if (rawEntries == null || !rawEntries.isJsonArray()) {
            throw new IllegalArgumentException("entries array is required");
        }
        JsonArray entries = rawEntries.getAsJsonArray();
        if (entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("too many compatibility entries");

        LinkedHashMap<String, OreHighlightStyle> result = new LinkedHashMap<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be an object");
            JsonObject object = element.getAsJsonObject();
            if (!object.has("block") || !object.has("style")) {
                throw new IllegalArgumentException("block and style are required");
            }
            Identifier blockId = Identifier.tryParse(object.get("block").getAsString());
            OreHighlightStyle style = OreHighlightStyle.fromKey(object.get("style").getAsString());
            if (blockId == null || style == null || "minecraft".equals(blockId.getNamespace())) {
                throw new IllegalArgumentException("invalid modded ore compatibility entry");
            }
            result.put(blockId.toString(), style);
        }
        return Map.copyOf(result);
    }
}
