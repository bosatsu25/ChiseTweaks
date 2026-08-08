package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.definition.FeatureArea;
import dev.chise.chisetweaks.core.policy.PumpkinScaffoldPolicy;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** MaLiLib UI adapters for bounded client-side settings. */
public final class LocalFeatureSettings {
    public static final ConfigInteger PUMPKIN_SCAFFOLD_PLACEMENT_RANGE = new ConfigInteger(
            "localPumpkinScaffoldPlacementRange",
            PumpkinScaffoldPolicy.DEFAULT_PLACEMENT_RANGE,
            PumpkinScaffoldPolicy.MIN_PLACEMENT_RANGE,
            PumpkinScaffoldPolicy.MAX_PLACEMENT_RANGE,
            "Maximum distance for a Pumpkin Scaffold air-placement attempt.");

    public static final ConfigBoolean LAVA_SOURCE = new ConfigBoolean(
            "localLavaHighlightSource", true, "Apply the source-lava highlight color.");
    public static final ConfigBoolean LAVA_FLOWING = new ConfigBoolean(
            "localLavaHighlightFlowing", true, "Apply the flowing-lava highlight color.");
    public static final ConfigInteger LAVA_SOURCE_COLOR = new ConfigInteger(
            "localLavaSourceColorArgb", 0xFFFF3B30, Integer.MIN_VALUE, Integer.MAX_VALUE,
            "Source-lava color as a signed ARGB integer.");
    public static final ConfigInteger LAVA_FLOWING_COLOR = new ConfigInteger(
            "localLavaFlowingColorArgb", 0xFFFF9500, Integer.MIN_VALUE, Integer.MAX_VALUE,
            "Flowing-lava color as a signed ARGB integer.");

    public static final ConfigInteger WORKSITE_VISIBILITY_HORIZONTAL_RADIUS = new ConfigInteger(
            "localWorksiteVisibilityHorizontalRadius", 5, 1, 8,
            "Horizontal radius for bounded local visibility scans. No chunks are loaded.");
    public static final ConfigInteger WORKSITE_VISIBILITY_VERTICAL_RADIUS = new ConfigInteger(
            "localWorksiteVisibilityVerticalRadius", 3, 1, 5,
            "Vertical radius for bounded local visibility scans.");
    public static final ConfigInteger WORKSITE_VISIBILITY_INTERVAL = new ConfigInteger(
            "localWorksiteVisibilityIntervalTicks", 10, 5, 100,
            "Ticks between local visibility scans.");
    public static final ConfigInteger WORKSITE_VISIBILITY_MAX_RESULTS = new ConfigInteger(
            "localWorksiteVisibilityMaxResults", 6, 1, 8,
            "Maximum nearby results shown in the visibility HUD.");
    public static final ConfigInteger WORKSITE_VISIBILITY_MAX_OVERLAYS = new ConfigInteger(
            "localWorksiteVisibilityMaxOverlays", 12, 1, 24,
            "Maximum visible blocks rendered by the bounded world overlay.");
    public static final ConfigBoolean WORKSITE_VISIBILITY_WORLD_OVERLAY = new ConfigBoolean(
            "localWorksiteVisibilityWorldOverlay", true,
            "Draw Chise-owned line markers for visible classified blocks.");
    public static final ConfigBoolean WORKSITE_VISIBILITY_EXCLUSIVE_MODE = new ConfigBoolean(
            "localWorksiteVisibilityExclusiveMode", false,
            "Keep at most one scan-based visibility mode active at a time.");

    public static final List<IConfigBase> BUILDING_OPTIONS = List.of(
            PUMPKIN_SCAFFOLD_PLACEMENT_RANGE);

    public static final List<IConfigBase> RENDERING_OPTIONS = List.of(
            LAVA_SOURCE,
            LAVA_FLOWING,
            LAVA_SOURCE_COLOR,
            LAVA_FLOWING_COLOR,
            WORKSITE_VISIBILITY_HORIZONTAL_RADIUS,
            WORKSITE_VISIBILITY_VERTICAL_RADIUS,
            WORKSITE_VISIBILITY_INTERVAL,
            WORKSITE_VISIBILITY_MAX_RESULTS,
            WORKSITE_VISIBILITY_MAX_OVERLAYS,
            WORKSITE_VISIBILITY_WORLD_OVERLAY,
            WORKSITE_VISIBILITY_EXCLUSIVE_MODE);

    public static final List<IConfigBase> ALL_OPTIONS = allOptions();

    private static boolean initialized;
    private static boolean syncing;
    private static Runnable worksiteVisibilityModeChangedCallback = () -> {};

    private LocalFeatureSettings() {}

    public static synchronized void init() {
        if (initialized) {
            refreshTranslations();
            return;
        }
        syncFromStorage();
        bindCallbacks();
        initialized = true;
        refreshTranslations();
    }

    public static void refreshTranslations() {
        for (IConfigBase option : ALL_OPTIONS) {
            String base = "config.option." + option.getName().toLowerCase();
            option.setPrettyName(StringUtils.getTranslatedOrFallback(
                    base + ".name", StringUtils.splitCamelCase(option.getName())));
            option.setComment(StringUtils.getTranslatedOrFallback(
                    base + ".comment", option.getComment()));
        }
    }

    public static List<IConfigBase> optionsFor(FeatureArea area) {
        if (area == null) return List.of();
        return switch (area) {
            case BUILDING -> BUILDING_OPTIONS;
            case RENDERING -> RENDERING_OPTIONS;
        };
    }

    private static List<IConfigBase> allOptions() {
        ArrayList<IConfigBase> result = new ArrayList<>(
                BUILDING_OPTIONS.size() + RENDERING_OPTIONS.size());
        result.addAll(BUILDING_OPTIONS);
        result.addAll(RENDERING_OPTIONS);
        return List.copyOf(result);
    }

    private static void syncFromStorage() {
        syncing = true;
        try {
            LocalFeatureConfig c = LocalFeatureConfig.getInstance();
            PUMPKIN_SCAFFOLD_PLACEMENT_RANGE.setIntegerValue(c.pumpkinScaffoldPlacementRange);
            LAVA_SOURCE.setBooleanValue(c.lavaHighlightSource);
            LAVA_FLOWING.setBooleanValue(c.lavaHighlightFlowing);
            LAVA_SOURCE_COLOR.setIntegerValue(c.lavaSourceColor);
            LAVA_FLOWING_COLOR.setIntegerValue(c.lavaFlowingColor);
            WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.setIntegerValue(c.worksiteVisibilityHorizontalRadius);
            WORKSITE_VISIBILITY_VERTICAL_RADIUS.setIntegerValue(c.worksiteVisibilityVerticalRadius);
            WORKSITE_VISIBILITY_INTERVAL.setIntegerValue(c.worksiteVisibilityIntervalTicks);
            WORKSITE_VISIBILITY_MAX_RESULTS.setIntegerValue(c.worksiteVisibilityMaxResults);
            WORKSITE_VISIBILITY_MAX_OVERLAYS.setIntegerValue(c.worksiteVisibilityMaxOverlayResults);
            WORKSITE_VISIBILITY_WORLD_OVERLAY.setBooleanValue(c.worksiteVisibilityWorldOverlay);
            WORKSITE_VISIBILITY_EXCLUSIVE_MODE.setBooleanValue(c.worksiteVisibilityExclusiveMode);
        } finally {
            syncing = false;
        }
    }

    private static void bindCallbacks() {
        PUMPKIN_SCAFFOLD_PLACEMENT_RANGE.setValueChangeCallback(ignored -> save(
                c -> c.pumpkinScaffoldPlacementRange = PUMPKIN_SCAFFOLD_PLACEMENT_RANGE.getIntegerValue()));
        LAVA_SOURCE.setValueChangeCallback(ignored -> save(
                c -> c.lavaHighlightSource = LAVA_SOURCE.getBooleanValue()));
        LAVA_FLOWING.setValueChangeCallback(ignored -> save(
                c -> c.lavaHighlightFlowing = LAVA_FLOWING.getBooleanValue()));
        LAVA_SOURCE_COLOR.setValueChangeCallback(ignored -> save(
                c -> c.lavaSourceColor = LAVA_SOURCE_COLOR.getIntegerValue()));
        LAVA_FLOWING_COLOR.setValueChangeCallback(ignored -> save(
                c -> c.lavaFlowingColor = LAVA_FLOWING_COLOR.getIntegerValue()));
        WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityHorizontalRadius = WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.getIntegerValue()));
        WORKSITE_VISIBILITY_VERTICAL_RADIUS.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityVerticalRadius = WORKSITE_VISIBILITY_VERTICAL_RADIUS.getIntegerValue()));
        WORKSITE_VISIBILITY_INTERVAL.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityIntervalTicks = WORKSITE_VISIBILITY_INTERVAL.getIntegerValue()));
        WORKSITE_VISIBILITY_MAX_RESULTS.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityMaxResults = WORKSITE_VISIBILITY_MAX_RESULTS.getIntegerValue()));
        WORKSITE_VISIBILITY_MAX_OVERLAYS.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityMaxOverlayResults = WORKSITE_VISIBILITY_MAX_OVERLAYS.getIntegerValue()));
        WORKSITE_VISIBILITY_WORLD_OVERLAY.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityWorldOverlay = WORKSITE_VISIBILITY_WORLD_OVERLAY.getBooleanValue()));
        WORKSITE_VISIBILITY_EXCLUSIVE_MODE.setValueChangeCallback(ignored -> {
            if (syncing) return;
            save(c -> c.worksiteVisibilityExclusiveMode =
                    WORKSITE_VISIBILITY_EXCLUSIVE_MODE.getBooleanValue());
            worksiteVisibilityModeChangedCallback.run();
        });
    }

    public static void setWorksiteVisibilityModeChangedCallback(Runnable callback) {
        worksiteVisibilityModeChangedCallback = callback == null ? () -> {} : callback;
    }

    private static void save(Consumer<LocalFeatureConfig> update) {
        if (syncing) return;
        LocalFeatureConfig c = LocalFeatureConfig.getInstance();
        update.accept(c);
        c.save();
    }
}
