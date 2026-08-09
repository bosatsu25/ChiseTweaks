package dev.chise.chisetweaks.core.policy;

/** Pure visual semantics for Chise's lava-source guide. */
public final class LavaVisionPalettePolicy {
    /** Deep forest/signal green reserved for lava-source boundaries, distinct from ore highlights. */
    public static final int SOURCE_OUTLINE_ARGB = 0xFF075B32;
    /** Slightly heavier than ordinary inspection lines so the source cube remains legible in lava. */
    public static final float SOURCE_LINE_WIDTH = 3.4f;

    /** Legacy no-tint value retained for old config/document compatibility tests. */
    public static final int NO_TINT = 0xFFFFFFFF;

    private LavaVisionPalettePolicy() {}

    /**
     * Returns whether one block should receive the source-guide cube.
     *
     * <p>The guide is deliberately source-only. Flowing lava is left untouched, and fully
     * surrounded source blocks are skipped so dense lava volumes do not turn into a wall of lines.</p>
     */
    public static boolean shouldHighlight(boolean enabled, boolean source, boolean exposed) {
        return enabled && source && exposed;
    }

    /**
     * Legacy tint selector retained while old persisted fields are accepted by the config schema.
     * New runtime rendering does not use this tint path.
     */
    public static int color(boolean enabled, boolean source, boolean showSource,
                            boolean showFlowing, int sourceRgb, int flowingRgb) {
        if (!enabled) return NO_TINT;
        if (source && showSource) return 0xFF000000 | sourceRgb;
        if (!source && showFlowing) return 0xFF000000 | flowingRgb;
        return NO_TINT;
    }
}
