package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.AncientDebrisAnalyzerPolicy;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * {@link LocalFeatureConfig} を直接参照するUI向け設定メタデータ。
 * メモリ上の状態はLocalFeatureConfigだけを正とし、このクラスは表示情報と変更通知だけを持つ。
 */
public final class LocalFeatureSettings {
    private static final Runnable NOOP = () -> {};

    private static Runnable worksiteVisibilityModeChangedCallback = NOOP;
    private static Runnable oreHighlightChangedCallback = NOOP;

    public static final SimpleBooleanSetting ORE_HIGHLIGHT_ANIMATION = bool(
            "localOreHighlightAnimation", false,
            "Ore highlight motion", "鉱石ハイライトの動き",
            "Animate Ore Highlights. Off keeps the same pattern static for reduced motion.",
            "鉱石ハイライトを動かします。OFFでは同じ模様を静止表示し、動きを抑えます。",
            () -> config().oreHighlightAnimationEnabled,
            value -> config().oreHighlightAnimationEnabled = value);

    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_HORIZONTAL_RADIUS = integer(
            "localWorksiteVisibilityHorizontalRadius", 5,
            WorksiteVisibilityBudgetPolicy.MIN_HORIZONTAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_HORIZONTAL_RADIUS,
            "Highlight scan radius", "ハイライト範囲",
            () -> config().worksiteVisibilityHorizontalRadius,
            value -> config().worksiteVisibilityHorizontalRadius = value);
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_VERTICAL_RADIUS = integer(
            "localWorksiteVisibilityVerticalRadius", 3,
            WorksiteVisibilityBudgetPolicy.MIN_VERTICAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_VERTICAL_RADIUS,
            "Highlight vertical radius", "ハイライト垂直範囲",
            () -> config().worksiteVisibilityVerticalRadius,
            value -> config().worksiteVisibilityVerticalRadius = value);
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_INTERVAL = integer(
            "localWorksiteVisibilityIntervalTicks", 10,
            WorksiteVisibilityBudgetPolicy.MIN_INTERVAL_TICKS,
            WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
            "Highlight scan interval", "ハイライト更新間隔",
            () -> config().worksiteVisibilityIntervalTicks,
            value -> config().worksiteVisibilityIntervalTicks = value);
    public static final ChiseIntegerSetting WORKSITE_VISIBILITY_MAX_OVERLAYS = integer(
            "localWorksiteVisibilityMaxOverlays", 12, 1,
            WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS,
            "Maximum highlight overlays", "ハイライト最大表示数",
            () -> config().worksiteVisibilityMaxOverlayResults,
            value -> config().worksiteVisibilityMaxOverlayResults = value);
    public static final SimpleBooleanSetting WORKSITE_VISIBILITY_WORLD_OVERLAY = bool(
            "localWorksiteVisibilityWorldOverlay", true,
            "World overlay", "ワールド表示",
            "Draw Chise-owned line markers for visible classified blocks.",
            "見えている対象ブロックにChise独自の補助線を描画します。",
            () -> config().worksiteVisibilityWorldOverlay,
            value -> config().worksiteVisibilityWorldOverlay = value);
    public static final SimpleBooleanSetting WORKSITE_VISIBILITY_EXCLUSIVE_MODE = bool(
            "localWorksiteVisibilityExclusiveMode", false,
            "Exclusive highlight mode", "ハイライト排他モード",
            "Keep at most one scan-based highlight mode active at a time.",
            "スキャン型ハイライトを同時に1つまでに制限します。",
            () -> config().worksiteVisibilityExclusiveMode,
            value -> config().worksiteVisibilityExclusiveMode = value);

    public static final ChiseIntegerSetting LAVA_ANALYZER_HORIZONTAL_RADIUS = integer(
            "localLavaAnalyzerHorizontalRadius", 5,
            WorksiteVisibilityBudgetPolicy.MIN_HORIZONTAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_HORIZONTAL_RADIUS,
            "Lava source highlight range", "溶岩源ハイライト範囲",
            () -> config().lavaAnalyzerHorizontalRadius,
            value -> config().lavaAnalyzerHorizontalRadius = value);
    public static final ChiseIntegerSetting LAVA_ANALYZER_VERTICAL_RADIUS = integer(
            "localLavaAnalyzerVerticalRadius", 3,
            WorksiteVisibilityBudgetPolicy.MIN_VERTICAL_RADIUS,
            WorksiteVisibilityBudgetPolicy.MAX_VERTICAL_RADIUS,
            "Lava source vertical range", "溶岩源垂直範囲",
            () -> config().lavaAnalyzerVerticalRadius,
            value -> config().lavaAnalyzerVerticalRadius = value);
    public static final ChiseIntegerSetting LAVA_ANALYZER_INTERVAL = integer(
            "localLavaAnalyzerIntervalTicks", 10,
            WorksiteVisibilityBudgetPolicy.MIN_INTERVAL_TICKS,
            WorksiteVisibilityBudgetPolicy.MAX_INTERVAL_TICKS,
            "Lava source update interval", "溶岩源更新間隔",
            () -> config().lavaAnalyzerIntervalTicks,
            value -> config().lavaAnalyzerIntervalTicks = value);
    public static final ChiseIntegerSetting LAVA_ANALYZER_MAX_OVERLAYS = integer(
            "localLavaAnalyzerMaxOverlays", 12, 1,
            WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS,
            "Maximum lava source markers", "溶岩源最大表示数",
            () -> config().lavaAnalyzerMaxOverlayResults,
            value -> config().lavaAnalyzerMaxOverlayResults = value);

    public static final ChiseIntegerSetting ANCIENT_DEBRIS_ANALYZER_RANGE = integer(
            "localAncientDebrisAnalyzerRange",
            AncientDebrisAnalyzerPolicy.DEFAULT_RANGE_BLOCKS,
            AncientDebrisAnalyzerPolicy.MIN_RANGE_BLOCKS,
            AncientDebrisAnalyzerPolicy.MAX_RANGE_BLOCKS,
            "Ancient Debris detection range", "古代の残骸の検出範囲",
            () -> config().ancientDebrisAnalyzerRangeBlocks,
            value -> config().ancientDebrisAnalyzerRangeBlocks = value);
    public static final ChiseIntegerSetting ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS = integer(
            "localAncientDebrisAnalyzerMaxMarkers",
            AncientDebrisAnalyzerPolicy.DEFAULT_MAX_MARKERS,
            AncientDebrisAnalyzerPolicy.MIN_MAX_MARKERS,
            AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS,
            "Maximum Ancient Debris markers", "古代の残骸の最大表示数",
            () -> config().ancientDebrisAnalyzerMaxMarkers,
            value -> config().ancientDebrisAnalyzerMaxMarkers = value);

    static {
        ORE_HIGHLIGHT_ANIMATION.setValueChangeCallback(
                ignored -> oreHighlightChangedCallback.run());
        WORKSITE_VISIBILITY_EXCLUSIVE_MODE.setValueChangeCallback(
                ignored -> worksiteVisibilityModeChangedCallback.run());
    }

    private LocalFeatureSettings() {}

    /** 起動処理の境界を明示するため残している。直接bindingのため状態同期処理は不要。 */
    public static void init() {}

    public static void setWorksiteVisibilityModeChangedCallback(Runnable callback) {
        worksiteVisibilityModeChangedCallback = callbackOrNoop(callback);
    }

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
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment,
            BooleanSupplier reader,
            Consumer<Boolean> writer) {
        return new SimpleBooleanSetting(
                name,
                defaultValue,
                englishName,
                japaneseName,
                englishComment,
                japaneseComment,
                reader,
                writer);
    }

    private static ChiseIntegerSetting integer(
            String name,
            int defaultValue,
            int minValue,
            int maxValue,
            String englishName,
            String japaneseName,
            IntSupplier reader,
            IntConsumer writer) {
        return new ChiseIntegerSetting(
                name,
                defaultValue,
                minValue,
                maxValue,
                englishName,
                japaneseName,
                englishName,
                japaneseName,
                reader,
                writer);
    }
}
