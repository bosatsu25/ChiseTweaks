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
import java.util.function.Consumer;

/** Chise管理Visibility packの軽量なtexture reloadを1本のqueueへ直列化する。 */
public final class ChiseTexturePackController {
    private static final ResourceReloadCoordinator RELOADS = new ResourceReloadCoordinator();
    private static final AtomicReference<ResourceReloadCoordinator.Recovery> TERMINAL_RECOVERY =
            new AtomicReference<>();
    private static final AtomicReference<Consumer<Boolean>> ACTIVE_RELOAD_COMPLETION =
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
        Consumer<Boolean> completion = ACTIVE_RELOAD_COMPLETION.getAndSet(null);
        notifyReloadCompletion(completion, false);
        if (client == null) {
            RELOADS.reset();
            return;
        }
        ResourceReloadCoordinator.Recovery recovery = RELOADS.cancel(
                List.copyOf(client.getResourcePackRepository().getSelectedIds()));
        if (recovery != null) TERMINAL_RECOVERY.set(recovery);
    }

    static boolean applyMigrationSelection(
            Minecraft client,
            List<String> selection,
            Consumer<Boolean> completion) {
        if (client == null || selection == null) return false;
        if (!recoverTerminalFailure(client) || RELOADS.isInFlight()) return false;
        return applySelection(
                client,
                "visibility-pack-migration",
                List.copyOf(selection),
                completion);
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
        applySelection(client, label, selected, null);
    }

    private static boolean applySelection(
            Minecraft client,
            String label,
            List<String> selected,
            Consumer<Boolean> completion) {
        PackRepository repository = client.getResourcePackRepository();
        List<String> previous = List.copyOf(repository.getSelectedIds());
        if (selected.equals(previous)) {
            notifyReloadCompletion(completion, true);
            return true;
        }
        if (completion != null && RELOADS.isInFlight()) return false;

        try {
            repository.setSelected(selected);
            client.options.updateResourcePacks(repository);
        } catch (RuntimeException | LinkageError failure) {
            restoreSelection(client, repository, previous);
            notifyReloadCompletion(completion, false);
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
        startReload(client, previous, selected, completion);
        return true;
    }

    private static void startReload(
            Minecraft client,
            List<String> fallbackSelection,
            List<String> targetSelection,
            Consumer<Boolean> completion) {
        RELOADS.begin(fallbackSelection, targetSelection);
        if (completion != null && !ACTIVE_RELOAD_COMPLETION.compareAndSet(null, completion)) {
            RELOADS.reset();
            notifyReloadCompletion(completion, false);
            throw new IllegalStateException("resource reload completion callback is already active");
        }
        try {
            // Bright Chest / Bright Concreteは前面の全resource reloadを直接呼ばず、
            // Minecraftが提供する遅延・並行texture reload経路で軽量に反映する。
            client.delayTextureReload().whenComplete((ignored, failure) -> {
                try {
                    client.execute(() -> completeReload(client, failure));
                } catch (RuntimeException | LinkageError schedulingFailure) {
                    Consumer<Boolean> reloadCompletion = ACTIVE_RELOAD_COMPLETION.getAndSet(null);
                    ResourceReloadCoordinator.Recovery recovery =
                            RELOADS.terminalFailure(failure == null);
                    if (recovery != null) TERMINAL_RECOVERY.set(recovery);
                    notifyReloadCompletion(reloadCompletion, false);
                    ChiseTweaksClient.LOGGER.error(
                            "Chise visibility texture reload completion could not reach the client thread after {}",
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
        Consumer<Boolean> reloadCompletion = ACTIVE_RELOAD_COMPLETION.getAndSet(null);
        ResourceReloadCoordinator.Completion completion = RELOADS.complete(failure == null);
        if (completion == null) {
            notifyReloadCompletion(reloadCompletion, false);
            return;
        }
        PackRepository repository = client.getResourcePackRepository();
        if (failure != null) logFailure("Chise visibility texture reload", failure);

        switch (completion.action()) {
            case RELOAD -> startReload(
                    client,
                    completion.activeSelection(),
                    completion.targetSelection(),
                    null);
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
        notifyReloadCompletion(reloadCompletion, failure == null);
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
            startReload(client, recovery.activeSelection(), recovery.desiredSelection(), null);
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

    private static void notifyReloadCompletion(Consumer<Boolean> completion, boolean succeeded) {
        if (completion == null) return;
        try {
            completion.accept(succeeded);
        } catch (RuntimeException | LinkageError callbackFailure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Chise visibility reload completion callback failed after {}",
                    callbackFailure.getClass().getSimpleName());
        }
    }

    private static void logFailure(String operation, Throwable failure) {
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}",
                operation,
                failure == null ? "unknown failure" : failure.getClass().getSimpleName());
    }
}
