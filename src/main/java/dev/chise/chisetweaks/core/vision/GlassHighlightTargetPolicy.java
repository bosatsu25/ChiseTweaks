package dev.chise.chisetweaks.core.vision;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Pure bake-time classification for vanilla Glass Highlight targets. */
public final class GlassHighlightTargetPolicy {
    private static final Set<String> DYE_COLORS = Set.of(
            "white", "orange", "magenta", "light_blue",
            "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue",
            "brown", "green", "red", "black");

    private static final Set<String> BLOCK_PATHS = createBlockPaths();
    private static final Set<String> PANE_PATHS = createPanePaths();

    private GlassHighlightTargetPolicy() {}

    public enum Shape {
        NONE,
        BLOCK,
        PANE
    }

    public static Shape classify(String rawNamespace, String rawPath) {
        String namespace = normalize(rawNamespace);
        String path = normalize(rawPath);
        if (!"minecraft".equals(namespace) || path.isEmpty()) return Shape.NONE;
        if (BLOCK_PATHS.contains(path)) return Shape.BLOCK;
        if (PANE_PATHS.contains(path)) return Shape.PANE;
        return Shape.NONE;
    }

    public static Shape classifyBlockId(String rawBlockId) {
        if (rawBlockId == null) return Shape.NONE;
        String normalized = rawBlockId.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        if (separator <= 0
                || separator == normalized.length() - 1
                || normalized.indexOf(':', separator + 1) >= 0) {
            return Shape.NONE;
        }
        return classify(
                normalized.substring(0, separator),
                normalized.substring(separator + 1));
    }

    public static Set<String> blockPaths() {
        return BLOCK_PATHS;
    }

    public static Set<String> panePaths() {
        return PANE_PATHS;
    }

    public static int blockVariantCount() {
        return BLOCK_PATHS.size();
    }

    public static int paneVariantCount() {
        return PANE_PATHS.size();
    }

    public static int totalVariantCount() {
        return BLOCK_PATHS.size() + PANE_PATHS.size();
    }

    private static Set<String> createBlockPaths() {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        paths.add("glass");
        paths.add("tinted_glass");
        for (String color : DYE_COLORS) {
            paths.add(color + "_stained_glass");
        }
        return Set.copyOf(paths);
    }

    private static Set<String> createPanePaths() {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        paths.add("glass_pane");
        for (String color : DYE_COLORS) {
            paths.add(color + "_stained_glass_pane");
        }
        return Set.copyOf(paths);
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
