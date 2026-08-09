package dev.chise.chisetweaks.core.vision;

import java.util.Locale;

/** Pure style selection for the retained visual-assistance overlays. */
public final class VisualAssistanceStylePolicy {
    private VisualAssistanceStylePolicy() {}

    public static OverlayStyle styleFor(String blockId, BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return OverlayStyle.NONE;
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return switch (category) {
            case TECHNICAL_TRACE -> new OverlayStyle(0xFFB29CFF, 100, Marker.CROSS);
            case HIDDEN_SURFACE -> new OverlayStyle(hiddenColor(id), 90, Marker.CROSS);
            case GLASS_INSPECTION -> new OverlayStyle(0xD6A68BFF, 60, Marker.BOX);
            case MATERIAL_HIGHLIGHT -> new OverlayStyle(materialColor(id), 80, Marker.DIAGONAL);
            case NETHER_PALETTE -> new OverlayStyle(netherColor(id), 20, Marker.BOX);
            case NONE -> OverlayStyle.NONE;
        };
    }

    private static int materialColor(String id) {
        if (id.contains("diamond")) return 0xFFB29CFF;
        if (id.contains("emerald")) return 0xFFA68BFF;
        if (id.contains("redstone")) return 0xFF8F7AE5;
        if (id.contains("lapis")) return 0xFF7D6BDB;
        if (id.contains("gold")) return 0xFF9B7EDE;
        if (id.contains("copper")) return 0xFF725AC1;
        if (id.contains("iron")) return 0xFF8A76FF;
        if (id.contains("coal")) return 0xFF6B5B95;
        if (id.endsWith("ancient_debris")) return 0xFF4E3A8C;
        return 0xFF5E4FA2;
    }

    private static int hiddenColor(String id) {
        if (id.endsWith("powder_snow")) return 0xFFA68BFF;
        if (id.endsWith("blue_ice")) return 0xFF8A76FF;
        if (id.contains("dead_") && id.contains("coral")) return 0xFF7D6BDB;
        return 0xFF9B7EDE;
    }

    private static int netherColor(String id) {
        if (id.contains("warped")) return 0xFF7D6BDB;
        if (id.contains("crimson") || id.contains("nether_wart")) return 0xFF8F7AE5;
        if (id.contains("soul_")) return 0xFFA68BFF;
        if (id.contains("blackstone")) return 0xFF4E3A8C;
        if (id.contains("basalt")) return 0xFF6B5B95;
        if (id.contains("glowstone") || id.contains("shroomlight")) return 0xFF9B7EDE;
        return 0xFF725AC1;
    }

    public enum Marker { NONE, BOX, CROSS, DIAGONAL }

    public record OverlayStyle(int argb, int priority, Marker marker) {
        public static final OverlayStyle NONE = new OverlayStyle(0x00000000, 0, Marker.NONE);
        public boolean visible() {
            return marker != Marker.NONE && (argb >>> 24) != 0;
        }
    }
}
