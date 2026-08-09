package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.policy.PumpkinScaffoldPolicy;

import java.util.function.Consumer;

/** Chise-owned UI adapters for bounded client-side settings. */
public final class LocalFeatureSettings {
    public static final ChiseIntegerSetting PUMPKIN_SCAFFOLD_PLACEMENT_RANGE = new ChiseIntegerSetting(
            "localPumpkinScaffoldPlacementRange",
            PumpkinScaffoldPolicy.DEFAULT_PLACEMENT_RANGE,
            PumpkinScaffoldPolicy.MIN_PLACEMENT_RANGE,
            PumpkinScaffoldPolicy.MAX_PLACEMENT_RANGE,
            "Pumpkin placement range", "かぼちゃ設置距離",
            "Maximum distance for a Pumpkin Scaffold air-placement attempt.",
            "かぼちゃを使った空中設置を試す最大距離です。");

    public static final SimpleBooleanSetting LAVA_SOURCE = bool(
            "localLavaHighlightSource", true,
            "Source lava", "溶岩源",
            "Apply the source-lava highlight color.", "溶岩源の強調色を適用します。");
    public static final SimpleBooleanSetting LAVA_FLOWING = bool(
            "localLavaHighlightFlowing", true,
            "Flowing lava", "流れる溶岩",
            "Apply the flowing-lava highlight color.", "流れる溶岩の強調色を適用します。");
    public static final ChiseIntegerSetting LAVA_SOURCE_COLOR = integer(
            "localLavaSourceColorArgb", 0xFFFF3B30, Integer.MIN_VALUE, Integer.MAX_VALUE,
            "Source lava ARGB", "溶岩源のARGB色");
    public static final ChiseIntegerSetting LAVA_FLOWING_COLOR = integer(
            "localLavaFlowingColorArgb", 0xFFFF9500, Integer.MIN_VALUE, Integer.MAX_VALUE,
            "Flowing lava ARGB", "流れる溶岩のARGB色");

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
            PUMPKIN_SCAFFOLD_PLACEMENT_RANGE.setIntegerValueSilently(c.pumpkinScaffoldPlacementRange);
            LAVA_SOURCE.setBooleanValueSilently(c.lavaHighlightSource);
            LAVA_FLOWING.setBooleanValueSilently(c.lavaHighlightFlowing);
            LAVA_SOURCE_COLOR.setIntegerValueSilently(c.lavaSourceColor);
            LAVA_FLOWING_COLOR.setIntegerValueSilently(c.lavaFlowingColor);
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
        PUMPKIN_SCAFFOLD_PLACEMENT_RANGE.setValueChangeCallback(ignored -> save(
                c -> c.pumpkinScaffoldPlacementRange = PUMPKIN_SCAFFOLD_PLACEMENT_RANGE.getIntegerValue()));
        LAVA_SOURCE.setValueChangeCallback(ignored -> save(c -> c.lavaHighlightSource = LAVA_SOURCE.getBooleanValue()));
        LAVA_FLOWING.setValueChangeCallback(ignored -> save(c -> c.lavaHighlightFlowing = LAVA_FLOWING.getBooleanValue()));
        LAVA_SOURCE_COLOR.setValueChangeCallback(ignored -> save(c -> c.lavaSourceColor = LAVA_SOURCE_COLOR.getIntegerValue()));
        LAVA_FLOWING_COLOR.setValueChangeCallback(ignored -> save(c -> c.lavaFlowingColor = LAVA_FLOWING_COLOR.getIntegerValue()));
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
