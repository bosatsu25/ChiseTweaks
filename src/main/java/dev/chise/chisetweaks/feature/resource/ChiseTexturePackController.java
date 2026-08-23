package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

/** Chise Textureの選択状態をMinecraft標準のresource-pack repositoryへ反映する。 */
public final class ChiseTexturePackController {
    private static boolean reloadInFlight;
    private static boolean reloadPending;
    private static List<String> inFlightFallback = List.of();
    private static List<String> inFlightTarget = List.of();

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

        try {
            repository.setSelected(selected);
            client.options.updateResourcePacks(repository);
        } catch (RuntimeException | LinkageError failure) {
            restoreSelection(client, repository, previous);
            logFailure(failure);
            return;
        }

        if (reloadInFlight) {
            reloadPending = true;
            return;
        }
        startReload(client, previous, selected);
    }

    private static void startReload(
            Minecraft client,
            List<String> fallbackSelection,
            List<String> targetSelection) {
        reloadInFlight = true;
        inFlightFallback = List.copyOf(fallbackSelection);
        inFlightTarget = List.copyOf(targetSelection);
        try {
            client.reloadResourcePacks().whenComplete((ignored, failure) -> {
                try {
                    client.execute(() -> completeReload(client, failure));
                } catch (RuntimeException | LinkageError schedulingFailure) {
                    resetReloadState();
                    logFailure(schedulingFailure);
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            completeReload(client, failure);
        }
    }

    private static void completeReload(Minecraft client, Throwable failure) {
        PackRepository repository = client.getResourcePackRepository();
        List<String> fallback = inFlightFallback;
        List<String> completedTarget = inFlightTarget;
        List<String> desired = new ArrayList<>(repository.getSelectedIds());
        boolean pending = reloadPending;
        resetReloadState();

        if (failure != null) logFailure(failure);
        List<String> active = failure == null ? completedTarget : fallback;
        if (pending && !desired.equals(active)) {
            startReload(client, active, desired);
            return;
        }
        if (failure != null && !desired.equals(active)) {
            restoreSelection(client, repository, active);
        }
    }

    private static void restoreSelection(
            Minecraft client,
            PackRepository repository,
            List<String> selection) {
        try {
            repository.setSelected(selection);
            client.options.updateResourcePacks(repository);
        } catch (RuntimeException | LinkageError rollbackFailure) {
            ChiseTweaksClient.LOGGER.error(
                    "Chise Texture selection rollback failed after {}",
                    rollbackFailure.getClass().getSimpleName());
        }
    }

    private static void resetReloadState() {
        reloadInFlight = false;
        reloadPending = false;
        inFlightFallback = List.of();
        inFlightTarget = List.of();
    }

    private static void logFailure(Throwable failure) {
        ChiseTweaksClient.LOGGER.warn(
                "Chise Texture resource reload failed after {}",
                failure == null ? "unknown failure" : failure.getClass().getSimpleName());
    }
}
