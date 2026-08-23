package dev.chise.chisetweaks.feature.resource;

import java.util.List;

/** Small deterministic state machine for serializing Minecraft resource reload transitions. */
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

    private boolean inFlight;
    private boolean pending;
    private List<String> fallback = List.of();
    private List<String> target = List.of();

    boolean isInFlight() {
        return inFlight;
    }

    void markPending() {
        if (inFlight) pending = true;
    }

    void begin(List<String> fallbackSelection, List<String> targetSelection) {
        if (inFlight) throw new IllegalStateException("resource reload is already in flight");
        inFlight = true;
        pending = false;
        fallback = List.copyOf(fallbackSelection);
        target = List.copyOf(targetSelection);
    }

    Completion complete(List<String> desiredSelection, boolean succeeded) {
        if (!inFlight) throw new IllegalStateException("no resource reload is in flight");
        List<String> desired = List.copyOf(desiredSelection);
        List<String> active = succeeded ? target : fallback;
        boolean hadPending = pending;
        reset();

        if (hadPending && !desired.equals(active)) {
            return new Completion(Action.RELOAD, active, desired);
        }
        if (!succeeded && !desired.equals(active)) {
            return new Completion(Action.RESTORE, active, active);
        }
        return new Completion(Action.NONE, active, active);
    }

    void reset() {
        inFlight = false;
        pending = false;
        fallback = List.of();
        target = List.of();
    }
}
