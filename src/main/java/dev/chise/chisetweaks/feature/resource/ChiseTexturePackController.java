package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.policy.ResourcePackSelectionPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.List;

/** Chise-owned visibility packs share one serialized Minecraft resource-reload pipeline. */
public final class ChiseTexturePackController {
    private static final ResourceReloadCoordinator RELOADS = new ResourceReloadCoordinator();

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

        if (RELOADS.isInFlight()) {
            RELOADS.markPending();
            return;
        }
        startReload(client, previous, selected);
    }

    private static void startReload(
            Minecraft client,
            List<String> fallbackSelection,
            List<String> targetSelection) {
        RELOADS.begin(fallbackSelection, targetSelection);
        try {
            client.reloadResourcePacks().whenComplete((ignored, failure) -> {
                try {
                    client.execute(() -> completeReload(client, failure));
                } catch (RuntimeException | LinkageError schedulingFailure) {
                    RELOADS.reset();
                    logFailure(schedulingFailure);
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            completeReload(client, failure);
        }
    }

    private static void completeReload(Minecraft client, Throwable failure) {
        PackRepository repository = client.getResourcePackRepository();
        List<String> desired = List.copyOf(repository.getSelectedIds());
        if (failure != null) logFailure(failure);

        ResourceReloadCoordinator.Completion completion =
                RELOADS.complete(desired, failure == null);
        switch (completion.action()) {
            case RELOAD -> startReload(
                    client,
                    completion.activeSelection(),
                    completion.targetSelection());
            case RESTORE -> restoreSelection(client, repository, completion.activeSelection());
            case NONE -> {
            }
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

    private static void logFailure(Throwable failure) {
        ChiseTweaksClient.LOGGER.warn(
                "Chise visibility resource reload failed after {}",
                failure == null ? "unknown failure" : failure.getClass().getSimpleName());
    }
}
