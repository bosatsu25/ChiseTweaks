package dev.chise.chisetweaks.config;

import java.util.function.Consumer;

/** Chise-owned UI adapters for retained bounded client-side visibility settings. */
public final class LocalFeatureSettings {
    public static final SimpleBooleanSetting ORE_HIGHLIGHT_ANIMATION = bool(
            "localOreHighlightAnimation", false,
            "Ore highlight motion", "鉱石ハイライトの動き",
            "Animate Ore Highlights. Off keeps the same pattern static for reduced motion.",
            "鉱石ハイライトを動かします。OFFでは同じ模様を静止表示し、動きを抑えます。");
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_HORIZONTAL_RADIUS = integer(
            "localWorksiteVisibilityHorizontalRadius", 5, 1, 8,
            "Horizontal scan radius", "視認スキャン範囲");
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_VERTICAL_RADIUS = integer(
            "localWorksiteVisibilityVerticalRadius", 3, 1, 5,
            "Vertical scan radius", "垂直スキャン範囲");
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_INTERVAL = integer(
            "localWorksiteVisibilityIntervalTicks", 10, 5, 100,
            "Scan interval", "スキャン間隔");
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_MAX_OVERLAYS = integer(
            "localWorksiteVisibilityMaxOverlays", 12, 1, 24,
            "Maximum overlays", "最大表示数");
    public static final SimpleBooleanSetting WORKSITE_VISIBILITY_WORLD_OVERLAY = bool(
            "localWorksiteVisibilityWorldOverlay", true,
            "World overlay", "ワールド表示",
            "Draw Chise-owned line markers for visible classified blocks.",
            "見えている対象ブロックにChise独自の補助線を描画します。");
    public static final SimpleBooleanSetting WORKSITE_VISIBILITY_EXCLUSIVE_MODE = bool(
            "localWorksiteVisibilityExclusiveMode", false,
            "Exclusive visibility mode", "視認モード排他",
            "Keep at most one scan-based visibility mode active at a time.",
            "スキャン型の視認機能を同時に1つまでに制限します。");

    private static boolean initialized;
    private static boolean syncing;
    private static Runnable worksiteVisibilityModeChangedCallback = () -> {};
    private static Runnable oreHighlightChangedCallback = () -> {};

    private LocalFeatureSettings() {}

    public static synchronized void init() {
        syncFromStorage();
        if (initialized) return;
        bindCallbacks();
        initialized = true;
    }

    private static void syncFromStorage() {
        syncing = true;
        try {
            LocalFeatureConfig c = LocalFeatureConfig.getInstance();
            ORE_HIGHLIGHT_ANIMATION.setBooleanValueSilently(c.oreHighlightAnimationEnabled);
            WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.setIntegerValueSilently(c.worksiteVisibilityHorizontalRadius);
            WORKSITE_VISIBILITY_VERTICAL_RADIUS.setIntegerValueSilently(c.worksiteVisibilityVerticalRadius);
            WORKSITE_VISIBILITY_INTERVAL.setIntegerValueSilently(c.worksiteVisibilityIntervalTicks);
            WORKSITE_VISIBILITY_MAX_OVERLAYS.setIntegerValueSilently(c.worksiteVisibilityMaxOverlayResults);
            WORKSITE_VISIBILITY_WORLD_OVERLAY.setBooleanValueSilently(c.worksiteVisibilityWorldOverlay);
            WORKSITE_VISIBILITY_EXCLUSIVE_MODE.setBooleanValueSilently(c.worksiteVisibilityExclusiveMode);
        } finally {
            syncing = false;
        }
    }

    private static void bindCallbacks() {
        ORE_HIGHLIGHT_ANIMATION.setValueChangeCallback(ignored -> {
            save(c -> c.oreHighlightAnimationEnabled = ORE_HIGHLIGHT_ANIMATION.getBooleanValue());
            oreHighlightChangedCallback.run();
        });
        WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityHorizontalRadius = WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.getIntegerValue()));
        WORKSITE_VISIBILITY_VERTICAL_RADIUS.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityVerticalRadius = WORKSITE_VISIBILITY_VERTICAL_RADIUS.getIntegerValue()));
        WORKSITE_VISIBILITY_INTERVAL.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityIntervalTicks = WORKSITE_VISIBILITY_INTERVAL.getIntegerValue()));
        WORKSITE_VISIBILITY_MAX_OVERLAYS.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityMaxOverlayResults = WORKSITE_VISIBILITY_MAX_OVERLAYS.getIntegerValue()));
        WORKSITE_VISIBILITY_WORLD_OVERLAY.setValueChangeCallback(ignored -> save(
                c -> c.worksiteVisibilityWorldOverlay = WORKSITE_VISIBILITY_WORLD_OVERLAY.getBooleanValue()));
        WORKSITE_VISIBILITY_EXCLUSIVE_MODE.setValueChangeCallback(ignored -> {
            if (syncing) return;
            save(c -> c.worksiteVisibilityExclusiveMode = WORKSITE_VISIBILITY_EXCLUSIVE_MODE.getBooleanValue());
            worksiteVisibilityModeChangedCallback.run();
        });
    }

    public static void setWorksiteVisibilityModeChangedCallback(Runnable callback) {
        worksiteVisibilityModeChangedCallback = callback == null ? () -> {} : callback;
    }

    public static void setOreHighlightChangedCallback(Runnable callback) {
        oreHighlightChangedCallback = callback == null ? () -> {} : callback;
    }

    private static void save(Consumer<LocalFeatureConfig> update) {
        if (syncing) return;
        LocalFeatureConfig c = LocalFeatureConfig.getInstance();
        update.accept(c);
        c.save();
    }

    private static SimpleBooleanSetting bool(
            String name,
            boolean defaultValue,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {
        return new SimpleBooleanSetting(
                name, defaultValue, englishName, japaneseName, englishComment, japaneseComment);
    }

    private static ChiseIntegerSetting integer(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            String englishName,
            String japaneseName) {
        return new ChiseIntegerSetting(
                name,
                defaultValue,
                minValue,
                maxValue,
                englishName,
                japaneseName,
                englishName,
                japaneseName);
    }
}
