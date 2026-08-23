package dev.chise.chisetweaks.feature.resource;

import java.util.List;

/** Minecraft resource reloadの連打・失敗・終了競合を直列化する決定的な状態機械。 */
final class ResourceReloadCoordinator {
    enum Action {
        NONE,
        RELOAD,
        RESTORE
    }

    record Completion(Action action, List<String> activeSelection, List<String> targetSelection) {
        Completion {
            activeSelection = List.copyOf(activeSelection);
            targetSelection = List.copyOf(targetSelection);
        }
    }

    record Recovery(List<String> activeSelection, List<String> desiredSelection) {
        Recovery {
            activeSelection = List.copyOf(activeSelection);
            desiredSelection = List.copyOf(desiredSelection);
        }

        boolean requiresReload() {
            return !activeSelection.equals(desiredSelection);
        }
    }

    record Snapshot(boolean inFlight, boolean pending) {}

    private boolean inFlight;
    private boolean pending;
    private List<String> fallback = List.of();
    private List<String> target = List.of();
    private List<String> pendingTarget = List.of();

    synchronized boolean isInFlight() {
        return inFlight;
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(inFlight, pending);
    }

    synchronized void markPending(List<String> desiredSelection) {
        if (!inFlight) return;
        pending = true;
        pendingTarget = List.copyOf(desiredSelection);
    }

    synchronized void begin(List<String> fallbackSelection, List<String> targetSelection) {
        if (inFlight) throw new IllegalStateException("resource reload is already in flight");
        inFlight = true;
        pending = false;
        fallback = List.copyOf(fallbackSelection);
        target = List.copyOf(targetSelection);
        pendingTarget = List.of();
    }

    synchronized Completion complete(boolean succeeded) {
        if (!inFlight) return null;
        boolean hadPending = pending;
        List<String> active = succeeded ? target : fallback;
        List<String> desired = hadPending
                ? pendingTarget
                : (succeeded ? target : fallback);
        resetInternal();

        if (!succeeded && !hadPending) {
            return new Completion(Action.RESTORE, active, active);
        }
        if (!desired.equals(active)) {
            return new Completion(Action.RELOAD, active, desired);
        }
        return new Completion(Action.NONE, active, active);
    }

    synchronized Recovery terminalFailure(boolean reloadSucceeded) {
        if (!inFlight) return null;
        List<String> active = reloadSucceeded ? target : fallback;
        List<String> desired = pending
                ? pendingTarget
                : (reloadSucceeded ? target : fallback);
        resetInternal();
        return new Recovery(active, desired);
    }

    synchronized Recovery cancel(List<String> desiredSelection) {
        if (!inFlight) return null;
        Recovery recovery = new Recovery(fallback, desiredSelection);
        resetInternal();
        return recovery;
    }

    synchronized void reset() {
        resetInternal();
    }

    private void resetInternal() {
        inFlight = false;
        pending = false;
        fallback = List.of();
        target = List.of();
        pendingTarget = List.of();
    }
}
