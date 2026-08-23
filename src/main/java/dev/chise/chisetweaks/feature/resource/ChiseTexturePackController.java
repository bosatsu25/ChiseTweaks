package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.policy.ResourcePackSelectionPolicy;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticDetail;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticEvent;
import dev.chise.chisetweaks.runtime.RuntimeDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Chise管理Visibility packのMinecraft resource reloadを1本のqueueへ直列化する。 */
public final class ChiseTexturePackController {
    private static final ResourceReloadCoordinator RELOADS = new ResourceReloadCoordinator();
    private static final AtomicReference<ResourceReloadCoordinator.Recovery> TERMINAL_RECOVERY =
            new AtomicReference<>();

    private ChiseTexturePackController() {}

    public static boolean isEnabled(VisibilityPack pack) {
        VisibilityPack checked = Objects.requireNonNull(pack, "pack");
        return isEnabled(checked.repositoryPackId());
    }

    public static void setEnabled(VisibilityPack pack, boolean enabled) {
        VisibilityPack checked = Objects.requireNonNull(pack, "pack");
        setEnabled(checked.repositoryPackId(), checked.displayName(), enabled);
    }

    public static boolean isReloadInFlight() {
        return RELOADS.snapshot().inFlight();
    }

    public static boolean hasPendingRecovery() {
        return TERMINAL_RECOVERY.get() != null;
    }

    public static List<String> selectedVisibilityPackIds(Minecraft client) {
        if (client == null) return List.of();
        List<String> selected = List.copyOf(client.getResourcePackRepository().getSelectedIds());
        ArrayList<String> result = new ArrayList<>(VisibilityPack.values().length);
        for (VisibilityPack pack : VisibilityPack.values()) {
            String packId = pack.repositoryPackId();
            if (selected.contains(packId)) result.add(packId);
        }
        return List.copyOf(result);
    }

    public static void onSessionStart(Minecraft client) {
        recoverTerminalFailure(client);
    }

    public static void onSessionEnd(Minecraft client) {
        if (client == null) {
            RELOADS.reset();
            return;
        }
        ResourceReloadCoordinator.Recovery recovery = RELOADS.cancel(
                List.copyOf(client.getResourcePackRepository().getSelectedIds()));
        if (recovery != null) TERMINAL_RECOVERY.set(recovery);
    }

    static boolean applyMigrationSelection(Minecraft client, List<String> selection) {
        if (client == null || selection == null) return false;
        if (!recoverTerminalFailure(client)) return false;
        return applySelection(client, "visibility-pack-migration", List.copyOf(selection));
    }

    private static boolean isEnabled(String packId) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        return client.getResourcePackRepository().getSelectedIds().contains(packId);
    }

    private static void setEnabled(String packId, String label, boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || !recoverTerminalFailure(client)) return;

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
        applySelection(client, label, selected);
    }

    private static boolean applySelection(Minecraft client, String label, List<String> selected) {
        PackRepository repository = client.getResourcePackRepository();
        List<String> previous = List.copyOf(repository.getSelectedIds());
        if (selected.equals(previous)) return true;

        try {
            repository.setSelected(selected);
            client.options.updateResourcePacks(repository);
        } catch (RuntimeException | LinkageError failure) {
            restoreSelection(client, repository, previous);
            logFailure(label, failure);
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.RESOURCE_PACK_SELECTION_FAILURE,
                    client,
                    RuntimeDiagnosticDetail.of("operation", label),
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()));
            return false;
        }

        if (RELOADS.isInFlight()) {
            RELOADS.markPending(selected);
            return true;
        }
        startReload(client, previous, selected);
        return true;
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
                    ResourceReloadCoordinator.Recovery recovery =
                            RELOADS.terminalFailure(failure == null);
                    if (recovery != null) TERMINAL_RECOVERY.set(recovery);
                    ChiseTweaksClient.LOGGER.error(
                            "Chise visibility resource reload completion could not reach the client thread after {}",
                            schedulingFailure.getClass().getSimpleName());
                    RuntimeDiagnostics.log(
                            RuntimeDiagnosticEvent.RESOURCE_RELOAD_TERMINAL_FAILURE,
                            client,
                            RuntimeDiagnosticDetail.of(
                                    "failure", schedulingFailure.getClass().getSimpleName()),
                            RuntimeDiagnosticDetail.of("reloadCompleted", failure == null));
                }
            });
        } catch (RuntimeException | LinkageError failure) {
            completeReload(client, failure);
        }
    }

    private static void completeReload(Minecraft client, Throwable failure) {
        ResourceReloadCoordinator.Completion completion = RELOADS.complete(failure == null);
        if (completion == null) return;
        PackRepository repository = client.getResourcePackRepository();
        if (failure != null) logFailure("Chise visibility resource reload", failure);

        switch (completion.action()) {
            case RELOAD -> startReload(
                    client,
                    completion.activeSelection(),
                    completion.targetSelection());
            case RESTORE -> {
                if (!restoreSelection(client, repository, completion.activeSelection())) {
                    TERMINAL_RECOVERY.set(new ResourceReloadCoordinator.Recovery(
                            completion.activeSelection(),
                            completion.activeSelection()));
                }
            }
            case NONE -> {
            }
        }
        if (failure != null) {
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.RESOURCE_RELOAD_FAILURE,
                    client,
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()),
                    RuntimeDiagnosticDetail.of("action", completion.action().name().toLowerCase()));
        }
    }

    private static boolean recoverTerminalFailure(Minecraft client) {
        ResourceReloadCoordinator.Recovery recovery = TERMINAL_RECOVERY.getAndSet(null);
        if (recovery == null) return true;
        if (client == null || RELOADS.isInFlight()) {
            TERMINAL_RECOVERY.compareAndSet(null, recovery);
            return false;
        }

        PackRepository repository = client.getResourcePackRepository();
        List<String> before = List.copyOf(repository.getSelectedIds());
        try {
            repository.setSelected(recovery.desiredSelection());
            client.options.updateResourcePacks(repository);
        } catch (RuntimeException | LinkageError failure) {
            TERMINAL_RECOVERY.compareAndSet(null, recovery);
            restoreSelection(client, repository, before);
            logFailure("Chise visibility terminal recovery", failure);
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.RESOURCE_RELOAD_TERMINAL_FAILURE,
                    client,
                    RuntimeDiagnosticDetail.of("stage", "recovery"),
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()));
            return false;
        }

        if (recovery.requiresReload()) {
            startReload(client, recovery.activeSelection(), recovery.desiredSelection());
        }
        RuntimeDiagnostics.log(
                RuntimeDiagnosticEvent.RESOURCE_RELOAD_RECOVERY,
                client,
                RuntimeDiagnosticDetail.of("requiresReload", recovery.requiresReload()));
        return true;
    }

    private static boolean restoreSelection(
            Minecraft client,
            PackRepository repository,
            List<String> selection) {
        try {
            repository.setSelected(selection);
            client.options.updateResourcePacks(repository);
            return true;
        } catch (RuntimeException | LinkageError rollbackFailure) {
            ChiseTweaksClient.LOGGER.error(
                    "Chise visibility pack selection rollback failed after {}",
                    rollbackFailure.getClass().getSimpleName());
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.RESOURCE_PACK_ROLLBACK_FAILURE,
                    client,
                    RuntimeDiagnosticDetail.of(
                            "failure", rollbackFailure.getClass().getSimpleName()));
            return false;
        }
    }

    private static void logFailure(String operation, Throwable failure) {
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}",
                operation,
                failure == null ? "unknown failure" : failure.getClass().getSimpleName());
    }
}
