package dev.chise.chisetweaks.core.policy;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Extracts the facing cue still required by Fine Thread Trace tripwire hooks. */
public final class OrientationOverlayPolicy {
    private OrientationOverlayPolicy() {}

    public static Overlay inspect(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) return Overlay.EMPTY;
        return new Overlay(parseFacing(properties.get("facing")).orElse(null));
    }

    public static Optional<Facing> parseFacing(String value) {
        return parseEnum(value, Facing.class);
    }

    private static <T extends Enum<T>> Optional<T> parseEnum(String value, Class<T> type) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    public enum Facing { DOWN, UP, NORTH, SOUTH, WEST, EAST }

    public record Overlay(Facing facing) {
        public static final Overlay EMPTY = new Overlay(null);

        public boolean empty() { return facing == null; }
    }
}
