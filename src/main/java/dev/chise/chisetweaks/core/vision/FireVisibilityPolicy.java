package dev.chise.chisetweaks.core.vision;

/** Pure policy for the first-person fire overlay. No texture or resource-pack assets are replaced. */
public final class FireVisibilityPolicy {
    /** Moderate downward shift that keeps fire feedback visible while opening the center of the view. */
    public static final float LOWERED_OVERLAY_Y = -0.30F;

    private FireVisibilityPolicy() {}

    public static boolean shouldLower(boolean enabled) {
        return enabled;
    }

    public static float verticalOffset(boolean enabled) {
        return enabled ? LOWERED_OVERLAY_Y : 0.0F;
    }
}
