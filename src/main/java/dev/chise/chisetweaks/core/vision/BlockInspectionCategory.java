package dev.chise.chisetweaks.core.vision;

/** Visual-assistance categories retained by the focused ChiseTweaks build. */
public enum BlockInspectionCategory {
    NONE(0xFFFFFFFF),
    TECHNICAL_TRACE(0xFFFFC857),
    HIDDEN_SURFACE(0xFF6EE7B7),
    GLASS_INSPECTION(0xFFC084FC),
    MATERIAL_HIGHLIGHT(0xFFFFD166),
    NETHER_PALETTE(0xFFFF7A90);

    private final int argb;

    BlockInspectionCategory(int argb) { this.argb = argb; }
    public int argb() { return argb; }
}
