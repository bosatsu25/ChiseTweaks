package dev.chise.chisetweaks.core.policy;

import java.util.Objects;
import java.util.Set;

/** Pure safety boundary for explicit entity visibility filtering. */
public final class BuilderEntityVisibilityPolicy {
    private BuilderEntityVisibilityPolicy() {}

    public enum Mode { NONE, BLACKLIST, WHITELIST }
    public enum Decision { SHOW_DISABLED, SHOW_LOCAL_PLAYER, SHOW, HIDE }

    public record Input(
            boolean enabled,
            boolean localPlayer,
            String entityId,
            Mode mode,
            Set<String> blacklist,
            Set<String> whitelist) {
        public Input {
            entityId = entityId == null ? "" : entityId;
            mode = Objects.requireNonNull(mode, "mode");
            blacklist = Set.copyOf(Objects.requireNonNull(blacklist, "blacklist"));
            whitelist = Set.copyOf(Objects.requireNonNull(whitelist, "whitelist"));
        }
    }

    public static Decision evaluate(Input input) {
        Objects.requireNonNull(input, "input");
        if (!input.enabled()) return Decision.SHOW_DISABLED;
        if (input.localPlayer()) return Decision.SHOW_LOCAL_PLAYER;
        if (input.entityId().isBlank() || input.mode() == Mode.NONE) return Decision.SHOW;
        boolean hide = switch (input.mode()) {
            case BLACKLIST -> input.blacklist().contains(input.entityId());
            case WHITELIST -> !input.whitelist().contains(input.entityId());
            case NONE -> false;
        };
        return hide ? Decision.HIDE : Decision.SHOW;
    }

    public static boolean filtersPlayers(Mode mode, Set<String> blacklist, Set<String> whitelist) {
        return evaluate(new Input(true, false, "minecraft:player", mode, blacklist, whitelist)) == Decision.HIDE;
    }
}
