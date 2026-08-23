package dev.chise.chisetweaks.feature.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.policy.VisibilityPackMigrationPolicy;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import dev.chise.chisetweaks.core.security.StrictJsonSecurityPolicy;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticDetail;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticEvent;
import dev.chise.chisetweaks.runtime.RuntimeDiagnostics;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 0.9.1以前の単一Visibility pack設定を0.9.2以降の分割packへ一度だけ移行する。 */
public final class VisibilityPackMigrationService {
    private static final String MARKER_FILE = "chisetweaks-visibility-pack-migration-v1.txt";
    private static final String COMPLETE_MARKER = "1";
    private static final String PENDING_SCHEMA = "1";
    private static final int MAX_PENDING_SELECTION_IDS = 512;
    private static final String LEGACY_PACK_ID = "chisetweaks:chise_texture";
    private static final String FEATURE_CONFIG_FILE = "chisetweaks.json";
    private static final String LOCAL_CONFIG_FILE = "chisetweaks-visual.json";
    private static final long MAX_OPTIONS_BYTES = 2L * 1024L * 1024L;

    private VisibilityPackMigrationService() {}

    public static boolean migrate(Minecraft client) {
        if (client == null) return false;
        FabricLoader loader = FabricLoader.getInstance();
        Path configDir = loader.getConfigDir();
        try {
            Optional<String> storedState = SecureConfigStorage.readUtf8(configDir, MARKER_FILE);
            String stateDocument = storedState.orElse("").strip();
            boolean markerPresent = COMPLETE_MARKER.equals(stateDocument);
            VisibilityPackMigrationPolicy.PendingIntent pendingIntent = markerPresent
                    ? null
                    : parsePendingIntentSafely(stateDocument);
            boolean priorConfigPresent = safeRegularFile(configDir.resolve(FEATURE_CONFIG_FILE))
                    || safeRegularFile(configDir.resolve(LOCAL_CONFIG_FILE));
            String optionsDocument = readOptionsDocument(loader.getGameDir());

            PackRepository repository = client.getResourcePackRepository();
            String chestId = VisibilityPack.CHEST.repositoryPackId();
            String concreteId = VisibilityPack.WHITE_CONCRETE.repositoryPackId();
            if (!repository.getAvailableIds().contains(chestId)
                    || !repository.getAvailableIds().contains(concreteId)) {
                ChiseTweaksClient.LOGGER.warn(
                        "Visibility pack migration deferred because split built-in packs are unavailable");
                RuntimeDiagnostics.log(
                        RuntimeDiagnosticEvent.VISIBILITY_PACK_MIGRATION_DEFERRED,
                        client,
                        RuntimeDiagnosticDetail.of("reason", "split-packs-unavailable"));
                return false;
            }

            List<String> currentSelection = List.copyOf(repository.getSelectedIds());
            VisibilityPackMigrationPolicy.Plan plan = pendingIntent == null
                    ? VisibilityPackMigrationPolicy.plan(
                            currentSelection,
                            markerPresent,
                            priorConfigPresent,
                            optionsDocument,
                            LEGACY_PACK_ID,
                            chestId,
                            concreteId)
                    : VisibilityPackMigrationPolicy.resumePending(
                            pendingIntent,
                            currentSelection,
                            LEGACY_PACK_ID,
                            chestId,
                            concreteId);
            if (plan.source() == VisibilityPackMigrationPolicy.Source.ALREADY_MIGRATED) return true;

            if (plan.selectionChanged()) {
                VisibilityPackMigrationPolicy.PendingIntent nextIntent =
                        VisibilityPackMigrationPolicy.pendingIntent(plan, currentSelection);
                writePendingIntent(configDir, nextIntent);
                boolean accepted = ChiseTexturePackController.applyMigrationSelection(
                        client,
                        plan.selectedIds(),
                        succeeded -> completeAsyncMigration(
                                configDir,
                                client,
                                nextIntent.source(),
                                succeeded));
                if (!accepted) {
                    RuntimeDiagnostics.log(
                            RuntimeDiagnosticEvent.VISIBILITY_PACK_MIGRATION_DEFERRED,
                            client,
                            RuntimeDiagnosticDetail.of("reason", "selection-not-accepted"),
                            RuntimeDiagnosticDetail.of("source", plan.source().name().toLowerCase()));
                    return false;
                }

                ChiseTweaksClient.LOGGER.info(
                        "Visibility pack migration selection staged source={}; completion awaits texture reload",
                        plan.source());
                return true;
            }

            completeMigration(configDir, client, plan.source(), false);
            return true;
        } catch (IOException | RuntimeException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Visibility pack migration deferred after {}",
                    failure.getClass().getSimpleName());
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.VISIBILITY_PACK_MIGRATION_DEFERRED,
                    client,
                    RuntimeDiagnosticDetail.of("reason", "exception"),
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()));
            return false;
        }
    }

    private static void completeAsyncMigration(
            Path configDir,
            Minecraft client,
            VisibilityPackMigrationPolicy.Source source,
            boolean succeeded) {
        if (!succeeded) {
            ChiseTweaksClient.LOGGER.warn(
                    "Visibility pack migration reload failed; persisted intent will be retried on the next startup");
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.VISIBILITY_PACK_MIGRATION_DEFERRED,
                    client,
                    RuntimeDiagnosticDetail.of("reason", "reload-failed"),
                    RuntimeDiagnosticDetail.of("source", source.name().toLowerCase()));
            return;
        }
        try {
            completeMigration(configDir, client, source, true);
        } catch (IOException | RuntimeException failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Visibility pack migration reload succeeded but completion marker could not be saved after {}",
                    failure.getClass().getSimpleName());
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.VISIBILITY_PACK_MIGRATION_DEFERRED,
                    client,
                    RuntimeDiagnosticDetail.of("reason", "completion-save-failed"),
                    RuntimeDiagnosticDetail.of("source", source.name().toLowerCase()));
        }
    }

    private static void completeMigration(
            Path configDir,
            Minecraft client,
            VisibilityPackMigrationPolicy.Source source,
            boolean selectionChanged) throws IOException {
        SecureConfigStorage.writeUtf8Atomic(configDir, MARKER_FILE, COMPLETE_MARKER + "\n");
        ChiseTweaksClient.LOGGER.info(
                "Visibility pack migration completed source={} selectionChanged={}",
                source,
                selectionChanged);
        RuntimeDiagnostics.log(
                RuntimeDiagnosticEvent.VISIBILITY_PACK_MIGRATION,
                client,
                RuntimeDiagnosticDetail.of("source", source.name().toLowerCase()),
                RuntimeDiagnosticDetail.of("selectionChanged", selectionChanged));
    }

    private static VisibilityPackMigrationPolicy.PendingIntent parsePendingIntentSafely(String document) {
        if (document == null || document.isBlank()) return null;
        try {
            StrictJsonSecurityPolicy.Validation validation =
                    StrictJsonSecurityPolicy.validateObjectDocument(document);
            if (!validation.valid()) return null;
            JsonObject root = JsonParser.parseString(document).getAsJsonObject();
            if (!PENDING_SCHEMA.equals(requiredString(root, "schema"))) return null;
            VisibilityPackMigrationPolicy.Source source = VisibilityPackMigrationPolicy.Source.valueOf(
                    requiredString(root, "source"));
            if (source == VisibilityPackMigrationPolicy.Source.ALREADY_MIGRATED) return null;
            return new VisibilityPackMigrationPolicy.PendingIntent(
                    source,
                    requiredStringList(root, "fallback"),
                    requiredStringList(root, "target"));
        } catch (RuntimeException invalid) {
            ChiseTweaksClient.LOGGER.warn(
                    "Ignoring invalid visibility migration pending state after {}",
                    invalid.getClass().getSimpleName());
            return null;
        }
    }

    private static void writePendingIntent(
            Path configDir,
            VisibilityPackMigrationPolicy.PendingIntent intent) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("schema", PENDING_SCHEMA);
        root.addProperty("source", intent.source().name());
        root.add("fallback", stringArray(intent.fallbackIds()));
        root.add("target", stringArray(intent.targetIds()));
        SecureConfigStorage.writeUtf8Atomic(configDir, MARKER_FILE, root.toString());
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(value);
        return array;
    }

    private static String requiredString(JsonObject root, String key) {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("migration state requires string field: " + key);
        }
        return value.getAsString();
    }

    private static List<String> requiredStringList(JsonObject root, String key) {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalArgumentException("migration state requires array field: " + key);
        }
        JsonArray array = value.getAsJsonArray();
        if (array.size() > MAX_PENDING_SELECTION_IDS) {
            throw new IllegalArgumentException("migration selection exceeds bounded size");
        }
        ArrayList<String> result = new ArrayList<>(array.size());
        for (JsonElement item : array) {
            if (item == null || !item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("migration selection contains a non-string id");
            }
            String id = item.getAsString().trim();
            if (id.isEmpty()) throw new IllegalArgumentException("migration selection contains a blank id");
            result.add(id);
        }
        return List.copyOf(result);
    }

    private static String readOptionsDocument(Path gameDir) throws IOException {
        if (gameDir == null) throw new IOException("Game directory is unavailable");
        Path normalizedRoot = gameDir.toAbsolutePath().normalize();
        Path options = normalizedRoot.resolve("options.txt").normalize();
        if (!options.getParent().equals(normalizedRoot)) throw new IOException("Options path escaped game directory");
        if (!Files.exists(options, LinkOption.NOFOLLOW_LINKS)) return "";
        if (Files.isSymbolicLink(options) || !Files.isRegularFile(options, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Options file is not a regular file");
        }
        long size = Files.size(options);
        if (size < 0 || size > MAX_OPTIONS_BYTES) throw new IOException("Options file exceeds migration budget");
        return Files.readString(options, StandardCharsets.UTF_8);
    }

    private static boolean safeRegularFile(Path path) {
        if (path == null) return false;
        return !Files.isSymbolicLink(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }
}
