package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.policy.ResourcePackSelectionPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

/** Chise-owned visibility packs share one serialized Minecraft resource-reload pipeline. */
public final class ChiseTexturePackController {
    private static boolean reloadInFlight;
    private static boolean reloadPending;
    private static List<String> inFlightFallback = List.of();
    private static List<String> inFlightTarget = List.of();

    private ChiseTexturePackController() {}

    public static boolean isChestEnabled() {
        return isEnabled(ChiseTexturePackRegistrar.chestRepositoryPackId());
    }

    public static void setChestEnabled(boolean enabled) {
        setEnabled(
                ChiseTexturePackRegistrar.chestRepositoryPackId(),
                "Chest Visibility",
                enabled);
    }

    public static boolean isWhiteConcreteEnabled() {
        return isEnabled(ChiseTexturePackRegistrar.whiteConcreteRepositoryPackId());
    }

    public static void setWhiteConcreteEnabled(boolean enabled) {
        setEnabled(
                ChiseTexturePackRegistrar.whiteConcreteRepositoryPackId(),
                "White Concrete Visibility",
                enabled);
    }

    private static boolean isEnabled(String packId) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        return client.getResourcePackRepository().getSelectedIds().contains(packId);
    }

    private static void setEnabled(String packId, String label, boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        PackRepository repository = client.getResourcePackRepository();
        if (!repository.getAvailableIds().contains(packId)) {
            ChiseTweaksClient.LOGGER.warn(
                    "{} was not changed because built-in pack {} is unavailable",
                    label,
                    packId);
            return;
        }

        List<String> previous = List.copyOf(repository.getSelectedIds());
        List<String> selected = ResourcePackSelectionPolicy.withPack(previous, packId, enabled);
        if (selected.equals(previous)) return;

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
                    "Chise visibility pack selection rollback failed after {}",
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
                "Chise visibility resource reload failed after {}",
                failure == null ? "unknown failure" : failure.getClass().getSimpleName());
    }
}
