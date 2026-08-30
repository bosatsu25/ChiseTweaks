package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.WorksiteHighlightProfilePolicy;
import dev.chise.chisetweaks.core.vision.FireVisibilityPolicy;
import dev.chise.chisetweaks.core.vision.HandheldSizePolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

import java.util.List;
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

    public static final ChiseIntegerSetting HANDHELD_BLOCK_SCALE = integer(
            "localHandheldBlockScalePercent",
            HandheldSizePolicy.DEFAULT_BLOCK_SCALE_PERCENT,
            HandheldSizePolicy.MIN_SCALE_PERCENT,
            HandheldSizePolicy.MAX_SCALE_PERCENT,
            () -> config().handheldBlockScalePercent,
            value -> config().handheldBlockScalePercent = value,
            value -> value + "%");
    public static final ChiseIntegerSetting HANDHELD_ITEM_SCALE = integer(
            "localHandheldItemScalePercent",
            HandheldSizePolicy.DEFAULT_ITEM_SCALE_PERCENT,
            HandheldSizePolicy.MIN_SCALE_PERCENT,
            HandheldSizePolicy.MAX_SCALE_PERCENT,
            () -> config().handheldItemScalePercent,
            value -> config().handheldItemScalePercent = value,
            value -> value + "%");
    public static final ChiseIntegerSetting HANDHELD_TOOL_SCALE = integer(
            "localHandheldToolScalePercent",
            HandheldSizePolicy.DEFAULT_TOOL_SCALE_PERCENT,
            HandheldSizePolicy.MIN_SCALE_PERCENT,
            HandheldSizePolicy.MAX_SCALE_PERCENT,
            () -> config().handheldToolScalePercent,
            value -> config().handheldToolScalePercent = value,
            value -> value + "%");

    public static final ChiseBooleanSetting ORE_HIGHLIGHT_ANIMATION = bool(
            "localOreHighlightAnimation", false,
            () -> config().oreHighlightAnimationEnabled,
            value -> config().oreHighlightAnimationEnabled = value);

    public static final ChiseBooleanSetting INTERACTION_HISTORY = bool(
            "localInteractionHistory", false,
            () -> config().interactionHistoryEnabled,
            value -> config().interactionHistoryEnabled = value);

    public static final ChiseBooleanSetting SCHEMATIC_PLACEMENT_INSPECTOR = bool(
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
    public static final ChiseBooleanSetting WORKSITE_VISIBILITY_WORLD_OVERLAY = bool(
            "localWorksiteVisibilityWorldOverlay", true,
            () -> config().worksiteVisibilityWorldOverlay,
            value -> config().worksiteVisibilityWorldOverlay = value);
    public static final ChiseBooleanSetting WORKSITE_VISIBILITY_DIMENSION_PRESETS = bool(
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

    public static final List<ChiseBooleanSetting> VISUAL_TARGETS = List.of(
            target(Target.MATERIAL_COAL_ORE, "visualTargetMaterialCoalOre"),
            target(Target.MATERIAL_IRON_ORE, "visualTargetMaterialIronOre"),
            target(Target.MATERIAL_COPPER_ORE, "visualTargetMaterialCopperOre"),
            target(Target.MATERIAL_GOLD_ORE, "visualTargetMaterialGoldOre"),
            target(Target.MATERIAL_LAPIS_ORE, "visualTargetMaterialLapisOre"),
            target(Target.MATERIAL_REDSTONE_ORE, "visualTargetMaterialRedstoneOre"),
            target(Target.MATERIAL_DIAMOND_ORE, "visualTargetMaterialDiamondOre"),
            target(Target.MATERIAL_EMERALD_ORE, "visualTargetMaterialEmeraldOre"),
            target(Target.MATERIAL_NETHER_GOLD_ORE, "visualTargetMaterialNetherGoldOre"),
            target(Target.MATERIAL_NETHER_QUARTZ_ORE, "visualTargetMaterialNetherQuartzOre"),
            target(Target.MATERIAL_ANCIENT_DEBRIS, "visualTargetMaterialAncientDebris"),
            target(Target.MATERIAL_OBSIDIAN, "visualTargetMaterialObsidian"),
            target(Target.MATERIAL_CRYING_OBSIDIAN, "visualTargetMaterialCryingObsidian"),
            target(Target.TECHNICAL_TRIPWIRE, "visualTargetTechnicalTripwire"),
            target(Target.TECHNICAL_TRIPWIRE_HOOK, "visualTargetTechnicalTripwireHook"),
            target(Target.HIDDEN_BLUE_ICE, "visualTargetHiddenBlueIce"),
            target(Target.HIDDEN_DEAD_CORAL, "visualTargetHiddenDeadCoral"),
            target(Target.HIDDEN_POWDER_SNOW, "visualTargetHiddenPowderSnow"),
            target(Target.HIDDEN_SCULK_CATALYST, "visualTargetHiddenSculkCatalyst"));

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

    public static synchronized void setAllOreHighlightTargets(boolean enabled) {
        LocalFeatureConfig config = config();
        int previous = config.visualTargetMask;
        config.visualTargetMask = VisualTargetSelectionPolicy.withAllOreHighlightTargets(
                config.visualTargetMask, enabled);
        if (config.visualTargetMask != previous) SettingChangeDispatcher.markChanged();
    }

    private static ChiseBooleanSetting target(Target target, String configName) {
        return new ChiseBooleanSetting(
                configName,
                true,
                () -> VisualTargetSelectionPolicy.isEnabled(config().visualTargetMask, target),
                enabled -> {
                    LocalFeatureConfig config = config();
                    config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                            config.visualTargetMask, target, enabled);
                },
                SettingPersistence.LOCAL_CONFIG);
    }

    private static LocalFeatureConfig config() {
        return LocalFeatureConfig.getInstance();
    }

    private static ChiseBooleanSetting bool(
            String name,
            boolean defaultValue,
            BooleanSupplier reader,
            Consumer<Boolean> writer) {
        return new ChiseBooleanSetting(
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
