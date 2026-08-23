package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.policy.VisibilityPackMigrationPolicy;
import dev.chise.chisetweaks.core.security.SecureConfigStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** 0.9.1以前の単一Visibility pack設定を0.9.2以降の分割packへ一度だけ移行する。 */
public final class VisibilityPackMigrationService {
    private static final String MARKER_FILE = "chisetweaks-visibility-pack-migration-v1.txt";
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
            boolean markerPresent = SecureConfigStorage.readUtf8(configDir, MARKER_FILE)
                    .filter(value -> value.strip().equals("1"))
                    .isPresent();
            boolean priorConfigPresent = safeRegularFile(configDir.resolve(FEATURE_CONFIG_FILE))
                    || safeRegularFile(configDir.resolve(LOCAL_CONFIG_FILE));
            String optionsDocument = readOptionsDocument(loader.getGameDir());

            PackRepository repository = client.getResourcePackRepository();
            String chestId = ChiseTexturePackRegistrar.chestRepositoryPackId();
            String concreteId = ChiseTexturePackRegistrar.whiteConcreteRepositoryPackId();
            if (!repository.getAvailableIds().contains(chestId)
                    || !repository.getAvailableIds().contains(concreteId)) {
                ChiseTweaksClient.LOGGER.warn(
                        "Visibility pack migration deferred because split built-in packs are unavailable");
                return false;
            }

            VisibilityPackMigrationPolicy.Plan plan = VisibilityPackMigrationPolicy.plan(
                    List.copyOf(repository.getSelectedIds()),
                    markerPresent,
                    priorConfigPresent,
                    optionsDocument,
                    LEGACY_PACK_ID,
                    chestId,
                    concreteId);
            if (plan.source() == VisibilityPackMigrationPolicy.Source.ALREADY_MIGRATED) return true;

            boolean accepted = !plan.selectionChanged()
                    || ChiseTexturePackController.applyMigrationSelection(client, plan.selectedIds());
            if (!accepted) return false;

            SecureConfigStorage.writeUtf8Atomic(configDir, MARKER_FILE, "1\n");
            ChiseTweaksClient.LOGGER.info(
                    "Visibility pack migration completed source={} selectionChanged={}",
                    plan.source(),
                    plan.selectionChanged());
            return true;
        } catch (IOException | RuntimeException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Visibility pack migration deferred after {}",
                    failure.getClass().getSimpleName());
            return false;
        }
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
