package dev.chise.chisetweaks.core.vision;

public final class FireVisibilityPolicy {
    public static final int SIZE_LARGE = 0;
    public static final int SIZE_MEDIUM = 1;
    public static final int SIZE_SMALL = 2;
    public static final int DEFAULT_SIZE_PRESET = 1;
    public static final int MIN_SIZE_PRESET = 0;
    public static final int MAX_SIZE_PRESET = 2;

    private FireVisibilityPolicy() {}

    public static boolean shouldLower(boolean enabled) {
        return enabled;
    }

    public static int clampSizePreset(int preset) {
        return Math.max(MIN_SIZE_PRESET, Math.min(MAX_SIZE_PRESET, preset));
    }

    public static float heightScale(boolean enabled, int preset) {
        if (!enabled) return 1.0F;
        return switch (clampSizePreset(preset)) {
            case SIZE_LARGE -> 0.78F;
            case SIZE_SMALL -> 0.34F;
            default -> 0.56F;
        };
    }

    public static float verticalOffset(boolean enabled, int preset) {
        if (!enabled) return 0.0F;
        return switch (clampSizePreset(preset)) {
            case SIZE_LARGE -> -0.14F;
            case SIZE_SMALL -> -0.34F;
            default -> -0.24F;
        };
    }

    public static String sizeLabel(int preset) {
        return switch (clampSizePreset(preset)) {
            case SIZE_LARGE -> "Large";
            case SIZE_SMALL -> "Small";
            default -> "Medium";
        };
    }
}
