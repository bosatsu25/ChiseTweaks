package dev.chise.chisetweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.core.vision.ModdedOreIdPolicy;
import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Safe local compatibility definitions used by modpacks and the custom ore editor. */
public final class OreHighlightCompatibilityConfig {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_ENTRIES = 256;
    private static final String FILE_NAME = "chisetweaks-ore-compat.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile List<Entry> entries = List.of();

    private OreHighlightCompatibilityConfig() {}

    public static synchronized void load() {
        try {
            Optional<String> stored = SecureConfigStorage.readUtf8(
                    FabricLoader.getInstance().getConfigDir(), FILE_NAME);
            if (stored.isEmpty() || stored.get().isBlank()) {
                replaceEntries(List.of(), true);
                return;
            }
            replaceEntries(parse(stored.get()), false);
        } catch (java.io.IOException | RuntimeException failure) {
            entries = List.of();
            publish();
            ChiseTweaksClient.LOGGER.warn(
                    "Unable to load Ore Highlight compatibility config after {}; using no custom entries",
                    failure.getClass().getSimpleName());
        }
    }

    public static List<Entry> entries() {
        return entries;
    }

    public static synchronized boolean put(String rawBlockId, OreHighlightStyle style) {
        Entry candidate = validatedEntry(rawBlockId, style);
        if (candidate == null) return false;
        ArrayList<Entry> updated = new ArrayList<>(entries);
        updated.removeIf(entry -> entry.blockId().equals(candidate.blockId()));
        if (updated.size() >= MAX_ENTRIES) return false;
        updated.add(candidate);
        replaceEntries(updated, true);
        return true;
    }

    public static synchronized boolean remove(String rawBlockId) {
        String normalized = ModdedOreIdPolicy.normalize(rawBlockId);
        ArrayList<Entry> updated = new ArrayList<>(entries);
        boolean changed = updated.removeIf(entry -> entry.blockId().equals(normalized));
        if (changed) replaceEntries(updated, true);
        return changed;
    }

    public static synchronized void clear() {
        replaceEntries(List.of(), true);
    }

    static synchronized void replaceEntries(List<Entry> input, boolean save) {
        LinkedHashMap<String, Entry> unique = new LinkedHashMap<>();
        if (input != null) {
            for (Entry entry : input) {
                if (unique.size() >= MAX_ENTRIES) break;
                Entry checked = entry == null ? null : validatedEntry(entry.blockId(), entry.style());
                if (checked != null) unique.put(checked.blockId(), checked);
            }
        }
        entries = List.copyOf(unique.values());
        publish();
        if (save) save();
    }

    private static void publish() {
        LinkedHashMap<String, OreHighlightStyle> styles = new LinkedHashMap<>();
        for (Entry entry : entries) styles.put(entry.blockId(), entry.style());
        OreHighlightExternalRegistry.replaceConfigBlocks(Map.copyOf(styles));
    }

    private static List<Entry> parse(String json) {
        StrictJsonSecurityPolicy.Validation validation = StrictJsonSecurityPolicy.validateObjectDocument(json);
        if (!validation.valid()) throw new IllegalArgumentException("unsafe compatibility JSON");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("schemaVersion") && root.get("schemaVersion").getAsInt() != SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported compatibility schema");
        }
        JsonElement rawEntries = root.get("entries");
        if (rawEntries == null) return List.of();
        if (!rawEntries.isJsonArray()) throw new IllegalArgumentException("entries must be an array");
        JsonArray array = rawEntries.getAsJsonArray();
        if (array.size() > MAX_ENTRIES) throw new IllegalArgumentException("too many compatibility entries");

        ArrayList<Entry> parsed = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be an object");
            JsonObject object = element.getAsJsonObject();
            if (!object.has("block") || !object.has("style")) {
                throw new IllegalArgumentException("entry requires block and style");
            }
            Entry entry = validatedEntry(
                    object.get("block").getAsString(),
                    OreHighlightStyle.fromKey(object.get("style").getAsString()));
            if (entry == null) throw new IllegalArgumentException("invalid compatibility entry");
            parsed.add(entry);
        }
        return List.copyOf(parsed);
    }

    private static Entry validatedEntry(String rawBlockId, OreHighlightStyle style) {
        if (style == null) return null;
        String normalized = ModdedOreIdPolicy.normalize(rawBlockId);
        Identifier id = Identifier.tryParse(normalized);
        if (id == null || "minecraft".equals(id.getNamespace())) return null;
        return new Entry(id.toString(), style);
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray array = new JsonArray();
        for (Entry entry : entries) {
            JsonObject object = new JsonObject();
            object.addProperty("block", entry.blockId());
            object.addProperty("style", entry.style().key());
            array.add(object);
        }
        root.add("entries", array);
        try {
            SecureConfigStorage.writeUtf8Atomic(
                    FabricLoader.getInstance().getConfigDir(), FILE_NAME, GSON.toJson(root));
        } catch (java.io.IOException | RuntimeException failure) {
            ChiseTweaksClient.LOGGER.error(
                    "Unable to save Ore Highlight compatibility config after {}",
                    failure.getClass().getSimpleName());
        }
    }

    public record Entry(String blockId, OreHighlightStyle style) {}
}
