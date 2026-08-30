package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.WorksiteHighlightProfilePolicy;
import dev.chise.chisetweaks.core.vision.FireVisibilityPolicy;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/** {@link LocalFeatureConfig} を直接参照するUI向けsetting binding。表示文言はUI/lang側を正本とする。 */
public final class LocalFeatureSettings {
    private static final Runnable NOOP = () -> {};
    private static Runnable oreHighlightChangedCallback = NOOP;

    public static final ChiseIntegerSetting FIRE_VISIBILITY_SIZE = integer(
            "localFireVisibilitySizePreset",
            FireVisibilityPolicy.DEFAULT_SIZE_PRESET,
            FireVisibilityPolicy.MIN_SIZE_PRESET,
            FireVisibilityPolicy.MAX_SIZE_PRESET,
            () -> config().fireVisibilitySizePreset,
            value -> config().fireVisibilitySizePreset = value,
            FireVisibilityPolicy::sizeLabel);

    public static final SimpleBooleanSetting ORE_HIGHLIGHT_ANIMATION = bool(
            "localOreHighlightAnimation", false,
            () -> config().oreHighlightAnimationEnabled,
            value -> config().oreHighlightAnimationEnabled = value);

    public static final SimpleBooleanSetting INTERACTION_HISTORY = bool(
            "localInteractionHistory", false,
            () -> config().interactionHistoryEnabled,
            value -> config().interactionHistoryEnabled = value);

    public static final SimpleBooleanSetting SCHEMATIC_PLACEMENT_INSPECTOR = bool(
            "localSchematicPlacementInspector", false,
            () -> config().schematicPlacementInspectorEnabled,
            value -> config().schematicPlacementInspectorEnabled = value);

    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_HORIZONTAL_RADIUS = integer(
            "localWorksiteVisibilityHorizontalRadius", 5,
            WorksiteVisibilityBudgetPolicy.MIN_HORIZONTAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_HORIZONTAL_RADIUS,
            () -> config().worksiteVisibilityHorizontalRadius,
            value -> config().worksiteVisibilityHorizontalRadius = value);
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_VERTICAL_RADIUS = integer(
            "localWorksiteVisibilityVerticalRadius", 3,
            WorksiteVisibilityBudgetPolicy.MIN_VERTICAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_VERTICAL_RADIUS,
            () -> config().worksiteVisibilityVerticalRadius,
            value -> config().worksiteVisibilityVerticalRadius = value);
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_INTERVAL = integer(
            "localWorksiteVisibilityIntervalTicks", 10,
            WorksiteVisibilityBudgetPolicy.MIN_INTERVAL_TICKS,
            WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
            () -> config().worksiteVisibilityIntervalTicks,
            value -> config().worksiteVisibilityIntervalTicks = value);
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_MAX_OVERLAYS = integer(
            "localWorksiteVisibilityMaxOverlays", 12, 1,
            WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS,
            () -> config().worksiteVisibilityMaxOverlayResults,
            value -> config().worksiteVisibilityMaxOverlayResults = value);
    public static final SimpleBooleanSetting WORKSITE_VISIBILITY_WORLD_OVERLAY = bool(
            "localWorksiteVisibilityWorldOverlay", true,
            () -> config().worksiteVisibilityWorldOverlay,
            value -> config().worksiteVisibilityWorldOverlay = value);
    public static final SimpleBooleanSetting WORKSITE_VISIBILITY_DIMENSION_PRESETS = bool(
            "localWorksiteVisibilityDimensionPresets", false,
            () -> config().worksiteVisibilityDimensionPresetsEnabled,
            value -> config().worksiteVisibilityDimensionPresetsEnabled = value);
    public static final ChiseIntegerSetting FINE_THREAD_TRACE_COLOR_PRESET = integer(
            "localFineThreadTraceColorPreset",
            WorksiteHighlightProfilePolicy.DEFAULT_COLOR_PRESET,
            WorksiteHighlightProfilePolicy.MIN_COLOR_PRESET,
            WorksiteHighlightProfilePolicy.MAX_COLOR_PRESET,
            () -> config().fineThreadTraceColorPreset,
            value -> config().fineThreadTraceColorPreset = value,
            WorksiteHighlightProfilePolicy::colorLabel);
    public static final ChiseIntegerSetting FINE_THREAD_TRACE_OPACITY = integer(
            "localFineThreadTraceOpacityPercent",
            WorksiteHighlightProfilePolicy.DEFAULT_OPACITY_PERCENT,
            WorksiteHighlightProfilePolicy.MIN_OPACITY_PERCENT,
            WorksiteHighlightProfilePolicy.MAX_OPACITY_PERCENT,
            () -> config().fineThreadTraceOpacityPercent,
            value -> config().fineThreadTraceOpacityPercent = value,
            value -> value + "%");
    public static final ChiseIntegerSetting HIDDEN_SURFACE_TRACE_COLOR_PRESET = integer(
            "localHiddenSurfaceTraceColorPreset",
            WorksiteHighlightProfilePolicy.DEFAULT_COLOR_PRESET,
            WorksiteHighlightProfilePolicy.MIN_COLOR_PRESET,
            WorksiteHighlightProfilePolicy.MAX_COLOR_PRESET,
            () -> config().hiddenSurfaceTraceColorPreset,
            value -> config().hiddenSurfaceTraceColorPreset = value,
            WorksiteHighlightProfilePolicy::colorLabel);
    public static final ChiseIntegerSetting HIDDEN_SURFACE_TRACE_OPACITY = integer(
            "localHiddenSurfaceTraceOpacityPercent",
            WorksiteHighlightProfilePolicy.DEFAULT_OPACITY_PERCENT,
            WorksiteHighlightProfilePolicy.MIN_OPACITY_PERCENT,
            WorksiteHighlightProfilePolicy.MAX_OPACITY_PERCENT,
            () -> config().hiddenSurfaceTraceOpacityPercent,
            value -> config().hiddenSurfaceTraceOpacityPercent = value,
            value -> value + "%");

    public static final ChiseIntegerSetting LAVA_ANALYZER_HORIZONTAL_RADIUS = integer(
            "localLavaAnalyzerHorizontalRadius", 5,
            WorksiteVisibilityBudgetPolicy.MIN_HORIZONTAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_HORIZONTAL_RADIUS,
            () -> config().lavaAnalyzerHorizontalRadius,
            value -> config().lavaAnalyzerHorizontalRadius = value);
    public static final ChiseIntegerSetting LAVA_ANALYZER_VERTICAL_RADIUS = integer(
            "localLavaAnalyzerVerticalRadius", 3,
            WorksiteVisibilityBudgetPolicy.MIN_VERTICAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_VERTICAL_RADIUS,
            () -> config().lavaAnalyzerVerticalRadius,
            value -> config().lavaAnalyzerVerticalRadius = value);
    public static final ChiseIntegerSetting LAVA_ANALYZER_INTERVAL = integer(
            "localLavaAnalyzerIntervalTicks", 10,
            WorksiteVisibilityBudgetPolicy.MIN_INTERVAL_TICKS,
            WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
            () -> config().lavaAnalyzerIntervalTicks,
            value -> config().lavaAnalyzerIntervalTicks = value);
    public static final ChiseIntegerSetting LAVA_ANALYZER_MAX_OVERLAYS = integer(
            "localLavaAnalyzerMaxOverlays", 12, 1,
            WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS,
            () -> config().lavaAnalyzerMaxOverlayResults,
            value -> config().lavaAnalyzerMaxOverlayResults = value);

    public static final ChiseIntegerSetting HIDDEN_ANALYZER_HORIZONTAL_RADIUS = integer(
            "localHiddenAnalyzerHorizontalRadius", 5,
            WorksiteVisibilityBudgetPolicy.MIN_HORIZONTAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_HORIZONTAL_RADIUS,
            () -> config().hiddenAnalyzerHorizontalRadius,
            value -> config().hiddenAnalyzerHorizontalRadius = value);
    public static final ChiseIntegerSetting HIDDEN_ANALYZER_VERTICAL_RADIUS = integer(
            "localHiddenAnalyzerVerticalRadius", 3,
            WorksiteVisibilityBudgetPolicy.MIN_VERTICAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_VERTICAL_RADIUS,
            () -> config().hiddenAnalyzerVerticalRadius,
            value -> config().hiddenAnalyzerVerticalRadius = value);
    public static final ChiseIntegerSetting HIDDEN_ANALYZER_INTERVAL = integer(
            "localHiddenAnalyzerIntervalTicks", 10,
            WorksiteVisibilityBudgetPolicy.MIN_INTERVAL_TICKS,
            WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
            () -> config().hiddenAnalyzerIntervalTicks,
            value -> config().hiddenAnalyzerIntervalTicks = value);
    public static final ChiseIntegerSetting HIDDEN_ANALYZER_MAX_OVERLAYS = integer(
            "localHiddenAnalyzerMaxOverlays", 12, 1,
            WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS,
            () -> config().hiddenAnalyzerMaxOverlayResults,
            value -> config().hiddenAnalyzerMaxOverlayResults = value);

    static {
        ORE_HIGHLIGHT_ANIMATION.setValueChangeCallback(
                ignored -> oreHighlightChangedCallback.run());
    }

    private LocalFeatureSettings() {}

    public static void setOreHighlightChangedCallback(Runnable callback) {
        oreHighlightChangedCallback = callbackOrNoop(callback);
    }

    private static Runnable callbackOrNoop(Runnable callback) {
        return callback == null ? NOOP : callback;
    }

    private static LocalFeatureConfig config() {
        return LocalFeatureConfig.getInstance();
    }

    private static SimpleBooleanSetting bool(
            String name,
            boolean defaultValue,
            BooleanSupplier reader,
            Consumer<Boolean> writer) {
        return new SimpleBooleanSetting(
                name,
                defaultValue,
                reader,
                writer,
                SettingPersistence.LOCAL_CONFIG);
    }

    private static ChiseIntegerSetting integer(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            IntSupplier reader,
            IntConsumer writer) {
        return new ChiseIntegerSetting(
                name,
                defaultValue,
                minValue,
                maxValue,
                reader,
                writer,
                SettingPersistence.LOCAL_CONFIG);
    }

    private static ChiseIntegerSetting integer(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            IntSupplier reader,
            IntConsumer writer,
            IntFunction<String> valueFormatter) {
        return new ChiseIntegerSetting(
                name,
                defaultValue,
                minValue,
                maxValue,
                reader,
                writer,
                valueFormatter,
                SettingPersistence.LOCAL_CONFIG);
    }
}
