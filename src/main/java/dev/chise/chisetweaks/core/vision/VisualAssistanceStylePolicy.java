package dev.chise.chisetweaks.core.vision;

import java.util.Locale;
import java.util.Map;

/** Pure style selection for Chise-owned high-visibility world overlays. */
public final class VisualAssistanceStylePolicy {
    private static final Map<String, Integer> GLASS_COLORS = Map.ofEntries(
            Map.entry("white", 0xFFF0F0F0),
            Map.entry("orange", 0xFFF2A65A),
            Map.entry("magenta", 0xFFD66BD6),
            Map.entry("light_blue", 0xFF79C8F2),
            Map.entry("yellow", 0xFFF4E45C),
            Map.entry("lime", 0xFF8FD14F),
            Map.entry("pink", 0xFFF29AB2),
            Map.entry("gray", 0xFF777C83),
            Map.entry("light_gray", 0xFFB8BDC3),
            Map.entry("cyan", 0xFF48B8C4),
            Map.entry("purple", 0xFF9365C8),
            Map.entry("blue", 0xFF4D6FD6),
            Map.entry("brown", 0xFF8A5A3C),
            Map.entry("green", 0xFF4E9B56),
            Map.entry("red", 0xFFE05252),
            Map.entry("black", 0xFF404047));

    private VisualAssistanceStylePolicy() {}

    public static OverlayStyle styleFor(String blockId, BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return OverlayStyle.NONE;
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return switch (category) {
            case TECHNICAL_TRACE -> new OverlayStyle(0xFF46FF6A, 100, Marker.THREAD_SIGNAL);
            case HIDDEN_SURFACE -> new OverlayStyle(hiddenColor(id), 90, Marker.SURFACE_HATCH);
            case GLASS_INSPECTION -> new OverlayStyle(glassColor(id), 60, Marker.GLASS_GRID);
            case PLACEMENT_GUIDE -> new OverlayStyle(0xFF4DD0E1, 70, Marker.ORIENTATION);
            case MATERIAL_HIGHLIGHT -> new OverlayStyle(materialColor(id), 80, Marker.MATERIAL_PULSE);
            case NETHER_PALETTE -> new OverlayStyle(netherColor(id), 20, Marker.NETHER_GRID);
            case NONE -> OverlayStyle.NONE;
        };
    }

    private static int glassColor(String id) {
        if (id.equals("minecraft:glass") || id.equals("minecraft:glass_pane")) return 0xFFE8F7FF;
        if (id.equals("minecraft:tinted_glass")) return 0xFF655E78;
        String path = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
        String suffix = path.endsWith("_stained_glass_pane")
                ? "_stained_glass_pane"
                : path.endsWith("_stained_glass") ? "_stained_glass" : "";
        if (suffix.isEmpty()) return 0xFFD8D8D8;
        return GLASS_COLORS.getOrDefault(path.substring(0, path.length() - suffix.length()), 0xFFD8D8D8);
    }

    private static int materialColor(String id) {
        if (id.contains("diamond")) return 0xFF50E3E6;
        if (id.contains("emerald")) return 0xFF57E389;
        if (id.contains("redstone")) return 0xFFFF5A5A;
        if (id.contains("lapis")) return 0xFF5A7DFF;
        if (id.contains("gold")) return 0xFFFFD24A;
        if (id.contains("copper")) return 0xFFFF9B62;
        if (id.contains("iron")) return 0xFFE4D8C8;
        if (id.contains("coal")) return 0xFFA6A6A6;
        if (id.endsWith("ancient_debris")) return 0xFFFF8B6B;
        return 0xFFC6A0FF;
    }

    private static int hiddenColor(String id) {
        if (id.endsWith("powder_snow")) return 0xFFBDEBFF;
        if (id.endsWith("blue_ice")) return 0xFF1F5CFF;
        if (id.contains("dead_") && id.contains("coral")) return 0xFFD9C6A5;
        return 0xFF6EE7B7;
    }

    private static int netherColor(String id) {
        if (id.contains("warped")) return 0xFF24A6A0;
        if (id.contains("crimson") || id.contains("nether_wart")) return 0xFFC63D58;
        if (id.contains("soul_")) return 0xFF4DB5D6;
        if (id.contains("blackstone")) return 0xFF6E607B;
        if (id.contains("basalt")) return 0xFF8C8C8C;
        if (id.contains("glowstone") || id.contains("shroomlight")) return 0xFFFFC857;
        if (id.contains("magma")) return 0xFFD85B2D;
        if (id.contains("gravel")) return 0xFF8D8580;
        return 0xFF723232;
    }

    public enum Marker {
        NONE,
        THREAD_SIGNAL,
        SURFACE_HATCH,
        GLASS_GRID,
        ORIENTATION,
        MATERIAL_PULSE,
        NETHER_GRID
    }

    public record OverlayStyle(int argb, int priority, Marker marker) {
        public static final OverlayStyle NONE = new OverlayStyle(0x00000000, 0, Marker.NONE);
        public boolean visible() {
            return marker != Marker.NONE && (argb >>> 24) != 0;
        }
    }
}
