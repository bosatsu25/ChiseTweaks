package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

/** Chise Textureの選択状態をMinecraft標準のresource-pack repositoryへ反映する。 */
public final class ChiseTexturePackController {
    private static long selectionRevision;

    private ChiseTexturePackController() {}

    public static boolean isEnabled() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        return client.getResourcePackRepository()
                .getSelectedIds()
                .contains(ChiseTexturePackRegistrar.repositoryPackId());
    }

    public static void setEnabled(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        PackRepository repository = client.getResourcePackRepository();
        String packId = ChiseTexturePackRegistrar.repositoryPackId();
        if (!repository.getAvailableIds().contains(packId)) {
            ChiseTweaksClient.LOGGER.warn(
                    "Chise Texture visibility was not changed because built-in pack {} is unavailable",
                    packId);
            return;
        }

        List<String> previous = new ArrayList<>(repository.getSelectedIds());
        List<String> selected = new ArrayList<>(previous);
        boolean changed;
        if (enabled) {
            changed = !selected.contains(packId);
            if (changed) selected.add(packId);
        } else {
            changed = selected.remove(packId);
        }
        if (!changed) return;

        long revision = ++selectionRevision;
        try {
            repository.setSelected(selected);
            client.options.updateResourcePacks(repository);
            client.reloadResourcePacks().whenComplete((ignored, failure) -> {
                if (failure == null) return;
                try {
                    client.execute(() -> rollbackIfCurrent(client, previous, revision, failure));
                } catch (RuntimeException | LinkageError schedulingFailure) {
                    logFailure(schedulingFailure);
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            rollbackIfCurrent(client, previous, revision, failure);
        }
    }

    private static void rollbackIfCurrent(
            Minecraft client,
            List<String> previous,
            long revision,
            Throwable failure) {
        if (revision != selectionRevision) {
            logFailure(failure);
            return;
        }
        PackRepository repository = client.getResourcePackRepository();
        try {
            repository.setSelected(previous);
            client.options.updateResourcePacks(repository);
        } catch (RuntimeException | LinkageError rollbackFailure) {
            ChiseTweaksClient.LOGGER.error(
                    "Chise Texture selection rollback failed after {}",
                    rollbackFailure.getClass().getSimpleName());
        }
        logFailure(failure);
    }

    private static void logFailure(Throwable failure) {
        ChiseTweaksClient.LOGGER.warn(
                "Chise Texture resource reload failed after {}",
                failure == null ? "unknown failure" : failure.getClass().getSimpleName());
    }
}
