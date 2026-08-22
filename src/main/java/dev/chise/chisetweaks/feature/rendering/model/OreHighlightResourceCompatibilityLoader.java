package dev.chise.chisetweaks.feature.rendering.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

 
final class OreHighlightResourceCompatibilityLoader {
    static final int MAX_RESOURCE_ENTRIES = 512;
    private static final FileToIdConverter FINDER = FileToIdConverter.json("chisetweaks/ore_compat");

    private OreHighlightResourceCompatibilityLoader() {}

    static Map<String, OreHighlightStyle> load(ResourceManager resourceManager) {
        if (resourceManager == null) return Map.of();
        LinkedHashMap<String, OreHighlightStyle> result = new LinkedHashMap<>();
        try {
            FINDER.listMatchingResources(resourceManager).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> readResource(entry.getKey(), entry.getValue(), result));
            return Map.copyOf(result);
        } catch (RuntimeException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Ore Highlight compatibility resource discovery failed after {}; using no resource-pack mappings",
                    failure.getClass().getSimpleName());
            return Map.of();
        }
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
            Map<String, OreHighlightStyle> parsed = parseDocument(json.toString());
            for (Map.Entry<String, OreHighlightStyle> entry : parsed.entrySet()) {
                if (output.size() >= MAX_RESOURCE_ENTRIES) break;
                output.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Ignoring invalid Ore Highlight compatibility resource {} after {}",
                    resourceId,
                    failure.getClass().getSimpleName());
        }
    }

    static Map<String, OreHighlightStyle> parseDocument(String json) {
        StrictJsonSecurityPolicy.Validation validation = StrictJsonSecurityPolicy.validateObjectDocument(json);
        if (!validation.valid()) throw new IllegalArgumentException("unsafe compatibility JSON");

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("schemaVersion")
                && exactInt(root.get("schemaVersion"), "schemaVersion") != OreHighlightCompatibilityConfig.SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported compatibility schema");
        }
        JsonElement rawEntries = root.get("entries");
        if (rawEntries == null) return Map.of();
        if (!rawEntries.isJsonArray()) throw new IllegalArgumentException("entries must be an array");
        JsonArray entries = rawEntries.getAsJsonArray();
        if (entries.size() > OreHighlightCompatibilityConfig.MAX_ENTRIES) {
            throw new IllegalArgumentException("too many entries in one compatibility resource");
        }

        LinkedHashMap<String, OreHighlightStyle> parsed = new LinkedHashMap<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be an object");
            JsonObject object = element.getAsJsonObject();
            Identifier blockId = Identifier.tryParse(ModdedOreIdPolicy.normalize(requiredString(object, "block")));
            OreHighlightStyle style = OreHighlightStyle.fromKey(requiredString(object, "style"));
            if (blockId == null || style == null || "minecraft".equals(blockId.getNamespace())) {
                throw new IllegalArgumentException("invalid compatibility entry");
            }
            parsed.put(blockId.toString(), style);
        }
        return Map.copyOf(parsed);
    }

    private static String requiredString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalArgumentException("entry requires string field: " + key);
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isString()) {
            throw new IllegalArgumentException("entry requires string field: " + key);
        }
        return primitive.getAsString();
    }

    private static int exactInt(JsonElement value, String field) {
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (!primitive.isNumber()) throw new IllegalArgumentException(field + " must be an integer");
        try {
            BigDecimal number = new BigDecimal(primitive.getAsString()).stripTrailingZeros();
            if (number.scale() > 0) throw new ArithmeticException("fraction");
            return number.intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException(field + " must be an integer", failure);
        }
    }
}
