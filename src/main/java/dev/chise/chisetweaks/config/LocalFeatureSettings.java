package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.policy.AncientDebrisAnalyzerPolicy;

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
            "Highlight scan radius", "ハイライト範囲");
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_VERTICAL_RADIUS = integer(
            "localWorksiteVisibilityVerticalRadius", 3, 1, 5,
            "Highlight vertical radius", "ハイライト垂直範囲");
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_INTERVAL = integer(
            "localWorksiteVisibilityIntervalTicks", 10, 5, 100,
            "Highlight scan interval", "ハイライト更新間隔");
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_MAX_OVERLAYS = integer(
            "localWorksiteVisibilityMaxOverlays", 12, 1, 24,
            "Maximum highlight overlays", "ハイライト最大表示数");
    public static final SimpleBooleanSetting WORKSITE_VISIBILITY_WORLD_OVERLAY = bool(
            "localWorksiteVisibilityWorldOverlay", true,
            "World overlay", "ワールド表示",
            "Draw Chise-owned line markers for visible classified blocks.",
            "見えている対象ブロックにChise独自の補助線を描画します。");
    public static final SimpleBooleanSetting WORKSITE_VISIBILITY_EXCLUSIVE_MODE = bool(
            "localWorksiteVisibilityExclusiveMode", false,
            "Exclusive highlight mode", "ハイライト排他モード",
            "Keep at most one scan-based highlight mode active at a time.",
            "スキャン型ハイライトを同時に1つまでに制限します。");

    public static final ChiseIntegerSetting LAVA_ANALYZER_HORIZONTAL_RADIUS = integer(
            "localLavaAnalyzerHorizontalRadius", 5, 1, 8,
            "Lava source highlight range", "溶岩源ハイライト範囲");
    public static final ChiseIntegerSetting LAVA_ANALYZER_VERTICAL_RADIUS = integer(
            "localLavaAnalyzerVerticalRadius", 3, 1, 5,
            "Lava source vertical range", "溶岩源垂直範囲");
    public static final ChiseIntegerSetting LAVA_ANALYZER_INTERVAL = integer(
            "localLavaAnalyzerIntervalTicks", 10, 5, 100,
            "Lava source update interval", "溶岩源更新間隔");
    public static final ChiseIntegerSetting LAVA_ANALYZER_MAX_OVERLAYS = integer(
            "localLavaAnalyzerMaxOverlays", 12, 1, 24,
            "Maximum lava source markers", "溶岩源最大表示数");

    public static final ChiseIntegerSetting ANCIENT_DEBRIS_ANALYZER_RANGE = integer(
            "localAncientDebrisAnalyzerRange",
            AncientDebrisAnalyzerPolicy.DEFAULT_RANGE_BLOCKS,
            AncientDebrisAnalyzerPolicy.MIN_RANGE_BLOCKS,
            AncientDebrisAnalyzerPolicy.MAX_RANGE_BLOCKS,
            "Ancient Debris detection range", "古代の残骸の検出範囲");
    public static final ChiseIntegerSetting ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS = integer(
            "localAncientDebrisAnalyzerMaxMarkers",
            AncientDebrisAnalyzerPolicy.DEFAULT_MAX_MARKERS,
            AncientDebrisAnalyzerPolicy.MIN_MAX_MARKERS,
            AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS,
            "Maximum Ancient Debris markers", "古代の残骸の最大表示数");

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
            LAVA_ANALYZER_HORIZONTAL_RADIUS.setIntegerValueSilently(c.lavaAnalyzerHorizontalRadius);
            LAVA_ANALYZER_VERTICAL_RADIUS.setIntegerValueSilently(c.lavaAnalyzerVerticalRadius);
            LAVA_ANALYZER_INTERVAL.setIntegerValueSilently(c.lavaAnalyzerIntervalTicks);
            LAVA_ANALYZER_MAX_OVERLAYS.setIntegerValueSilently(c.lavaAnalyzerMaxOverlayResults);
            ANCIENT_DEBRIS_ANALYZER_RANGE.setIntegerValueSilently(c.ancientDebrisAnalyzerRangeBlocks);
            ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS.setIntegerValueSilently(c.ancientDebrisAnalyzerMaxMarkers);
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
        LAVA_ANALYZER_HORIZONTAL_RADIUS.setValueChangeCallback(ignored -> save(
                c -> c.lavaAnalyzerHorizontalRadius = LAVA_ANALYZER_HORIZONTAL_RADIUS.getIntegerValue()));
        LAVA_ANALYZER_VERTICAL_RADIUS.setValueChangeCallback(ignored -> save(
                c -> c.lavaAnalyzerVerticalRadius = LAVA_ANALYZER_VERTICAL_RADIUS.getIntegerValue()));
        LAVA_ANALYZER_INTERVAL.setValueChangeCallback(ignored -> save(
                c -> c.lavaAnalyzerIntervalTicks = LAVA_ANALYZER_INTERVAL.getIntegerValue()));
        LAVA_ANALYZER_MAX_OVERLAYS.setValueChangeCallback(ignored -> save(
                c -> c.lavaAnalyzerMaxOverlayResults = LAVA_ANALYZER_MAX_OVERLAYS.getIntegerValue()));
        ANCIENT_DEBRIS_ANALYZER_RANGE.setValueChangeCallback(ignored -> save(
                c -> c.ancientDebrisAnalyzerRangeBlocks = ANCIENT_DEBRIS_ANALYZER_RANGE.getIntegerValue()));
        ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS.setValueChangeCallback(ignored -> save(
                c -> c.ancientDebrisAnalyzerMaxMarkers = ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS.getIntegerValue()));
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
