package dev.chise.chisetweaks.core.policy;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Pure extraction of bounded orientation markers from block-state properties. */
public final class OrientationOverlayPolicy {
    private OrientationOverlayPolicy() {}

    public static Overlay inspect(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) return Overlay.EMPTY;
        Half half = parseHalf(properties.get("half"))
                .or(() -> parseSlabType(properties.get("type")))
                .orElse(null);
        return new Overlay(
                parseFacing(properties.get("facing")).orElse(null),
                parseAxis(properties.get("axis")).orElse(null),
                half,
                parseShape(properties.get("shape")).orElse(null),
                parseMountFace(properties.get("face")).orElse(null),
                parseBoolean(properties.get("open")).orElse(null));
    }

    public static Optional<Facing> parseFacing(String value) {
        return parseEnum(value, Facing.class);
    }

    public static Optional<Axis> parseAxis(String value) {
        return parseEnum(value, Axis.class);
    }

    public static Optional<Half> parseHalf(String value) {
        return parseEnum(value, Half.class);
    }

    public static Optional<Shape> parseShape(String value) {
        return parseEnum(value, Shape.class);
    }

    public static Optional<MountFace> parseMountFace(String value) {
        return parseEnum(value, MountFace.class);
    }

    public static Optional<Boolean> parseBoolean(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.equals("true")) return Optional.of(true);
        if (normalized.equals("false")) return Optional.of(false);
        return Optional.empty();
    }

    private static Optional<Half> parseSlabType(String value) {
        if (value == null) return Optional.empty();
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "top" -> Optional.of(Half.TOP);
            case "bottom" -> Optional.of(Half.BOTTOM);
            default -> Optional.empty();
        };
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
    public enum Axis { X, Y, Z }
    public enum Half { TOP, BOTTOM, UPPER, LOWER }
    public enum Shape { STRAIGHT, INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT }
    public enum MountFace { FLOOR, WALL, CEILING }

    public record Overlay(
            Facing facing,
            Axis axis,
            Half half,
            Shape shape,
            MountFace mountFace,
            Boolean open) {
        public static final Overlay EMPTY = new Overlay(null, null, null, null, null, null);

        public boolean empty() {
            return facing == null && axis == null && half == null
                    && shape == null && mountFace == null && open == null;
        }
    }
}
