package dev.chise.chisetweaks.feature.rendering.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.core.vision.ModdedOreIdPolicy;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads optional modpack/resource-pack ore mappings from
 * {@code assets/<namespace>/chisetweaks/ore_compat/*.json}.
 *
 * <p>The format intentionally mirrors the local editor document:
 * {@code {"schemaVersion":1,"entries":[{"block":"mod:ore","style":"copper"}]}}.
 * Definitions can only target non-Minecraft namespaces and only select Chise-owned styles.</p>
 */
final class OreHighlightResourceCompatibilityLoader {
    static final int MAX_RESOURCE_ENTRIES = 512;
    private static final FileToIdConverter FINDER = FileToIdConverter.json("chisetweaks/ore_compat");

    private OreHighlightResourceCompatibilityLoader() {}

    static Map<String, OreHighlightStyle> load(ResourceManager resourceManager) {
        if (resourceManager == null) return Map.of();
        LinkedHashMap<String, OreHighlightStyle> result = new LinkedHashMap<>();
        FINDER.listMatchingResources(resourceManager).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> readResource(entry.getKey(), entry.getValue(), result));
        return Map.copyOf(result);
    }

    private static void readResource(
            Identifier resourceId,
            Resource resource,
            LinkedHashMap<String, OreHighlightStyle> output) {
        if (output.size() >= MAX_RESOURCE_ENTRIES) return;
        try (BufferedReader reader = resource.openAsReader()) {
            StringBuilder json = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                json.append(buffer, 0, read);
                if (json.length() > 256 * 1024) {
                    throw new IllegalArgumentException("compat resource exceeds size budget");
                }
            }
            parseInto(json.toString(), output);
        } catch (Exception failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Ignoring invalid Ore Highlight compatibility resource {} after {}",
                    resourceId,
                    failure.getClass().getSimpleName());
        }
    }

    static void parseInto(String json, LinkedHashMap<String, OreHighlightStyle> output) {
        StrictJsonSecurityPolicy.Validation validation = StrictJsonSecurityPolicy.validateObjectDocument(json);
        if (!validation.valid()) throw new IllegalArgumentException("unsafe compatibility JSON");

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("schemaVersion")
                && root.get("schemaVersion").getAsInt() != OreHighlightCompatibilityConfig.SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported compatibility schema");
        }
        JsonElement rawEntries = root.get("entries");
        if (rawEntries == null) return;
        if (!rawEntries.isJsonArray()) throw new IllegalArgumentException("entries must be an array");
        JsonArray entries = rawEntries.getAsJsonArray();
        if (entries.size() > OreHighlightCompatibilityConfig.MAX_ENTRIES) {
            throw new IllegalArgumentException("too many entries in one compatibility resource");
        }

        for (JsonElement element : entries) {
            if (output.size() >= MAX_RESOURCE_ENTRIES) return;
            if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be an object");
            JsonObject object = element.getAsJsonObject();
            if (!object.has("block") || !object.has("style")) {
                throw new IllegalArgumentException("entry requires block and style");
            }
            Identifier blockId = Identifier.tryParse(ModdedOreIdPolicy.normalize(object.get("block").getAsString()));
            OreHighlightStyle style = OreHighlightStyle.fromKey(object.get("style").getAsString());
            if (blockId == null || style == null || "minecraft".equals(blockId.getNamespace())) {
                throw new IllegalArgumentException("invalid compatibility entry");
            }
            output.put(blockId.toString(), style);
        }
    }
}
