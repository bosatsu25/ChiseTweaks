package dev.chise.chisetweaks.core.vision;

/**
 * Pure policy for distinguishing Chise's slightly expanded ore-highlight overlay geometry
 * from the underlying 0..1 vanilla/resource-pack block cube.
 *
 * <p>The overlay is rendered emissively at Minecraft's maximum light level while the base
 * block keeps normal world lighting. Keeping this decision independent from Fabric rendering
 * classes makes the full-bright boundary deterministic and mutation-testable.</p>
 */
public final class OreHighlightLightingPolicy {
    public static final int FULL_BRIGHT_LIGHT_LEVEL = 15;
    public static final float BASE_MIN = 0.0f;
    public static final float BASE_MAX = 1.0f;
    public static final float OVERLAY_EPSILON = 0.001f;

    private OreHighlightLightingPolicy() {}

    /**
     * Returns true when a vertex is measurably outside the base block cube and therefore belongs
     * to the Chise overlay geometry. A small epsilon prevents floating-point bake noise around
     * exact vanilla 0/1 coordinates from being misclassified as emissive.
     */
    public static boolean isOverlayVertex(float x, float y, float z) {
        return belowBase(x) || aboveBase(x)
                || belowBase(y) || aboveBase(y)
                || belowBase(z) || aboveBase(z);
    }

    private static boolean belowBase(float value) {
        return value < BASE_MIN - OVERLAY_EPSILON;
    }

    private static boolean aboveBase(float value) {
        return value > BASE_MAX + OVERLAY_EPSILON;
    }
}
