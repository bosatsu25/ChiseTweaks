package dev.chise.chisetweaks.core.vision;

import java.util.Locale;

/** Pure style selection for the retained visual-assistance overlays. */
public final class VisualAssistanceStylePolicy {
    private VisualAssistanceStylePolicy() {}

    public static OverlayStyle styleFor(String blockId, BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return OverlayStyle.NONE;
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return switch (category) {
            case TECHNICAL_TRACE -> new OverlayStyle(0xFFFFC857, 100, Marker.CROSS);
            case HIDDEN_SURFACE -> new OverlayStyle(hiddenColor(id), 90, Marker.CROSS);
            case GLASS_INSPECTION -> new OverlayStyle(0xD6C084FC, 60, Marker.BOX);
            case PLACEMENT_GUIDE -> new OverlayStyle(0xE64DD0E1, 70, Marker.ORIENTATION);
            case MATERIAL_HIGHLIGHT -> new OverlayStyle(materialColor(id), 80, Marker.DIAGONAL);
            case NETHER_PALETTE -> new OverlayStyle(netherColor(id), 20, Marker.BOX);
            case NONE -> OverlayStyle.NONE;
        };
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
        if (id.endsWith("blue_ice")) return 0xFF6DB7FF;
        if (id.contains("dead_") && id.contains("coral")) return 0xFFB8A58A;
        return 0xFF6EE7B7;
    }

    private static int netherColor(String id) {
        if (id.contains("warped")) return 0xFF52D7D0;
        if (id.contains("crimson") || id.contains("nether_wart")) return 0xFFE45A72;
        if (id.contains("soul_")) return 0xFF69C9E8;
        if (id.contains("blackstone")) return 0xFFB49ACF;
        if (id.contains("basalt")) return 0xFFB7B7B7;
        if (id.contains("glowstone") || id.contains("shroomlight")) return 0xFFFFD166;
        return 0xFFE38C78;
    }

    public enum Marker { NONE, BOX, CROSS, DIAGONAL, ORIENTATION }

    public record OverlayStyle(int argb, int priority, Marker marker) {
        public static final OverlayStyle NONE = new OverlayStyle(0x00000000, 0, Marker.NONE);
        public boolean visible() {
            return marker != Marker.NONE && (argb >>> 24) != 0;
        }
    }
}
