package dev.chise.chisetweaks.core.vision;

public final class FireVisibilityPolicy {

    public static final float LOWERED_OVERLAY_Y = -0.30F;

    private FireVisibilityPolicy() {}

    public static boolean shouldLower(boolean enabled) {
        return enabled;
    }

    public static float verticalOffset(boolean enabled) {
        return enabled ? LOWERED_OVERLAY_Y : 0.0F;
    }
}
