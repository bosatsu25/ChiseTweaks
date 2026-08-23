package dev.chise.chisetweaks.core.policy;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;

/**
 * Fine Thread / Hidden Surface向けの見た目とdimension別scan profileを純粋関数で決める。
 * Minecraft型へ依存させず、設定値の境界・色・alpha・preset切替を単体試験できるようにする。
 */
public final class WorksiteHighlightProfilePolicy {
    public static final int DEFAULT_COLOR_PRESET = -1;
    public static final int MIN_COLOR_PRESET = DEFAULT_COLOR_PRESET;
    public static final int MAX_COLOR_PRESET = 7;
    public static final int DEFAULT_OPACITY_PERCENT = 100;
    public static final int MIN_OPACITY_PERCENT = 20;
    public static final int MAX_OPACITY_PERCENT = 100;

    private static final int[] PRESET_RGB = {
            0xB29CFF, // violet
            0x6FE7F7, // cyan
            0xFFD166, // amber
            0x8BE28B, // lime
            0xFF72D2, // magenta
            0xF0F4FF, // ice white
            0xFF9F5A, // orange
            0xFF6B6B  // red
    };

    private WorksiteHighlightProfilePolicy() {}

    public enum DimensionProfile {
        OVERWORLD,
        NETHER,
        OTHER
    }

    public record ScanProfile(int horizontalRadius, int verticalRadius) {}

    public static int clampColorPreset(int requested) {
        return Math.max(MIN_COLOR_PRESET, Math.min(MAX_COLOR_PRESET, requested));
    }

    public static int clampOpacityPercent(int requested) {
        return Math.max(MIN_OPACITY_PERCENT, Math.min(MAX_OPACITY_PERCENT, requested));
    }

    public static String colorLabel(int preset) {
        int normalized = clampColorPreset(preset);
        if (normalized == DEFAULT_COLOR_PRESET) return "AUTO";
        return String.format("#%06X", PRESET_RGB[normalized]);
    }

    public static ScanProfile scanProfile(
            int requestedHorizontalRadius,
            int requestedVerticalRadius,
            boolean dimensionPresetsEnabled,
            DimensionProfile dimension) {
        int horizontal = WorksiteVisibilityBudgetPolicy.clampHorizontalRadius(requestedHorizontalRadius);
        int vertical = WorksiteVisibilityBudgetPolicy.clampVerticalRadius(requestedVerticalRadius);
        DimensionProfile resolved = dimension == null ? DimensionProfile.OTHER : dimension;
        if (dimensionPresetsEnabled && resolved == DimensionProfile.NETHER) {
            horizontal = Math.max(
                    WorksiteVisibilityBudgetPolicy.MIN_HORIZONTAL_RADIUS,
                    horizontal - 1);
        }
        return new ScanProfile(horizontal, vertical);
    }

    public static VisualAssistanceStylePolicy.OverlayStyle customize(
            VisualAssistanceStylePolicy.OverlayStyle base,
            BlockInspectionCategory category,
            int fineThreadColorPreset,
            int fineThreadOpacityPercent,
            int hiddenSurfaceColorPreset,
            int hiddenSurfaceOpacityPercent,
            boolean dimensionPresetsEnabled,
            DimensionProfile dimension) {
        if (base == null || !base.visible() || category == null) {
            return VisualAssistanceStylePolicy.OverlayStyle.NONE;
        }
        if (category != BlockInspectionCategory.TECHNICAL_TRACE
                && category != BlockInspectionCategory.HIDDEN_SURFACE) {
            return base;
        }

        int preset = category == BlockInspectionCategory.TECHNICAL_TRACE
                ? clampColorPreset(fineThreadColorPreset)
                : clampColorPreset(hiddenSurfaceColorPreset);
        int opacity = category == BlockInspectionCategory.TECHNICAL_TRACE
                ? clampOpacityPercent(fineThreadOpacityPercent)
                : clampOpacityPercent(hiddenSurfaceOpacityPercent);

        DimensionProfile resolved = dimension == null ? DimensionProfile.OTHER : dimension;
        if (dimensionPresetsEnabled && resolved == DimensionProfile.NETHER) {
            if (preset == DEFAULT_COLOR_PRESET) {
                preset = category == BlockInspectionCategory.TECHNICAL_TRACE ? 1 : 2;
            }
            opacity = clampOpacityPercent(opacity + 10);
        }

        int rgb = preset == DEFAULT_COLOR_PRESET
                ? base.argb() & 0x00FFFFFF
                : PRESET_RGB[preset];
        int alpha = (clampOpacityPercent(opacity) * 255 + 50) / 100;
        return new VisualAssistanceStylePolicy.OverlayStyle(
                alpha << 24 | rgb,
                base.priority(),
                base.marker());
    }
}
