package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

 
final class ChiseTweaksSettingsController {
    enum Surface {
        MAIN,
        HIGHLIGHT_DETAILS,
        LAVA_DETAILS
    }

     
    private static final List<ChiseBooleanSetting> HIGHLIGHT_FEATURES = List.of(
            FeatureSwitches.MATERIAL_HIGHLIGHTS,
            FeatureSwitches.GLASS_INSPECTION,
            FeatureSwitches.KELP_HIGHLIGHT);

    private final boolean japanese;
    private final List<ChiseBooleanSetting> resourceTargets;
    private final List<ChiseBooleanSetting> visibilityTargets;

    ChiseTweaksSettingsController(boolean japanese) {
        this.japanese = japanese;
        this.resourceTargets = targets("visualTargetMaterial");
        this.visibilityTargets = targets("visualTargetHidden");
    }

    static ChiseTweaksSettingsController forCurrentLanguage() {
        String probe = Component.translatable("screen.chisetweaks.help.language.probe").getString();
        return new ChiseTweaksSettingsController("ja".equalsIgnoreCase(probe));
    }

    void initialize() {
        LocalFeatureSettings.init();
        VisualTargetSettings.init();
    }

    boolean japanese() { return japanese; }

    List<ChiseTweaksSettingRowDefinition> rows() {
        return rows(Surface.MAIN);
    }

    List<ChiseTweaksSettingRowDefinition> rows(Surface surface) {
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        switch (surface == null ? Surface.MAIN : surface) {
            case MAIN -> addMainRows(rows);
            case HIGHLIGHT_DETAILS -> addHighlightDetailRows(rows);
            case LAVA_DETAILS -> addAnalyzerDetailRows(rows);
        }
        return List.copyOf(rows);
    }

    String surfaceTitle(Surface surface) {
        return switch (surface == null ? Surface.MAIN : surface) {
            case MAIN -> "";
            case HIGHLIGHT_DETAILS -> japanese ? "ハイライト設定" : "Highlight Settings";
            case LAVA_DETAILS -> japanese ? "アナライザー設定" : "Analyzer Settings";
        };
    }

    boolean shouldTurnHighlightBulkOn() {
        for (ChiseBooleanSetting feature : HIGHLIGHT_FEATURES) {
            if (!feature.getBooleanValue()) return true;
        }
        return false;
    }

    void toggleHighlightBulk() {
        boolean enabled = shouldTurnHighlightBulkOn();
        for (ChiseBooleanSetting feature : HIGHLIGHT_FEATURES) {
            feature.setBooleanValue(enabled);
        }
    }

    boolean reset(Surface surface) {
        switch (surface == null ? Surface.MAIN : surface) {
            case MAIN -> resetAll();
            case HIGHLIGHT_DETAILS -> resetHighlightDetails();
            case LAVA_DETAILS -> resetAnalyzerDetails();
        }
        return true;
    }

    boolean resetAll() {
        FeatureSwitches.MATERIAL_HIGHLIGHTS.resetToDefault();
        FeatureSwitches.NETHER_PALETTE.resetToDefault();
        FeatureSwitches.FINE_THREAD_TRACE.resetToDefault();
        FeatureSwitches.HIDDEN_SURFACE_TRACE.resetToDefault();
        FeatureSwitches.GLASS_INSPECTION.resetToDefault();
        FeatureSwitches.KELP_HIGHLIGHT.resetToDefault();
        FeatureSwitches.BUILDER_FOCUS_BLOCKS.resetToDefault();
        FeatureSwitches.BUILDER_FOCUS_ENTITIES.resetToDefault();
        LocalFeatureSwitches.LAVA_HIGHLIGHT.resetToDefault();
        LocalFeatureSwitches.ANCIENT_DEBRIS_ANALYZER.resetToDefault();
        LocalFeatureSwitches.FIRE_VISIBILITY.resetToDefault();

        resetHighlightDetails();
        resetAnalyzerDetails();

        BuilderFocusConfig.REFRESH_RENDERER.resetToDefault();
        BuilderFocusConfig.BLOCK_RULE_MODE.resetToDefault();
        BuilderFocusConfig.BLOCK_WHITELIST.resetToDefault();
        BuilderFocusConfig.BLOCK_BLACKLIST.resetToDefault();
        BuilderFocusConfig.ENTITY_RULE_MODE.resetToDefault();
        BuilderFocusConfig.ENTITY_WHITELIST.resetToDefault();
        BuilderFocusConfig.ENTITY_BLACKLIST.resetToDefault();
        return true;
    }

    boolean saveConfig() {
        boolean featureSaved = FeatureConfig.saveToFile();
        boolean localSaved = LocalFeatureConfig.getInstance().save();
        return featureSaved && localSaved;
    }

    private void addMainRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        rows.add(ChiseTweaksSettingRowDefinition.headerAction(
                "header.highlight",
                japanese ? "ハイライト" : "Highlight",
                ChiseTweaksSettingRowDefinition.Action.OPEN_HIGHLIGHT_DETAILS,
                japanese ? "設定" : "Settings"));
        compactBool(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS,
                "鉱石ハイライト", "Ore Highlight");
        compactBool(rows, "nether", FeatureSwitches.NETHER_PALETTE,
                "ネザーハイライト", "Nether Highlight");
        compactBool(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE,
                "細線ハイライト", "Fine Line Highlight");
        compactBool(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                "隠れブロックハイライト", "Hidden Block Highlight");
        compactBool(rows, "glass", FeatureSwitches.GLASS_INSPECTION,
                "ガラスハイライト", "Glass Highlight");
        compactBool(rows, "kelp", FeatureSwitches.KELP_HIGHLIGHT,
                "昆布ハイライト", "Kelp Highlight");

        header(rows, "header.visualFilter", "Visual Filter", "Visual Filter");
        compactBoolAction(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS,
                "ブロックフィルター", "Block Filter",
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER);
        compactBoolAction(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES,
                "エンティティフィルター", "Entity Filter",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER);

        rows.add(ChiseTweaksSettingRowDefinition.headerAction(
                "header.analyzer",
                japanese ? "アナライザー" : "Analyzer",
                ChiseTweaksSettingRowDefinition.Action.OPEN_LAVA_DETAILS,
                japanese ? "設定" : "Settings"));
        compactBool(rows, "lava", LocalFeatureSwitches.LAVA_HIGHLIGHT,
                "溶岩源ハイライト", "Lava Source Highlight");
        compactBool(rows, "ancientDebrisAnalyzer", LocalFeatureSwitches.ANCIENT_DEBRIS_ANALYZER,
                "古代の残骸アナライザー", "Ancient Debris Analyzer");

        header(rows, "header.visibilityImprovement", "見やすさ", "Visibility");
        compactBool(rows, "fireVisibility", LocalFeatureSwitches.FIRE_VISIBILITY,
                "火炎表示を低くする", "Lower Fire Overlay");
    }

    private void addHighlightDetailRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "detail.highlight.general", "共通設定", "Shared Settings");
        bool(rows, "oreMotion", LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION,
                "ハイライトを動かす", "Animate Ore Highlight",
                "OFFでは静止表示、ONで控えめにアニメーションする",
                "Keep highlights static when OFF or use subtle motion when ON.");
        action(rows, "moddedOreTargets", "MOD鉱石の対象",
                "MOD鉱石のRegistry IDとChiseの発光スタイルを編集する",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT);
        integer(rows, "highlightRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS,
                "ハイライト範囲", "Highlight Range",
                "周辺ハイライトの水平範囲", "Horizontal radius for scan-based highlights.", 1);
        integer(rows, "highlightVerticalRange", LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS,
                "ハイライト垂直範囲", "Highlight Vertical Range",
                "周辺ハイライトの垂直範囲", "Vertical radius for scan-based highlights.", 1);
        integer(rows, "highlightInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL,
                "ハイライト更新間隔", "Highlight Update Interval",
                "周辺確認を行うtick間隔", "Ticks between scan-based highlight updates.", 5);
        integer(rows, "highlightMaxOverlays", LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS,
                "ハイライト最大表示数", "Maximum Highlights",
                "同時に保持する補助表示数", "Maximum scan-based highlight overlays.", 1);
        bool(rows, "highlightWorldOverlay", LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY,
                "ワールド表示", "World Overlay",
                "スキャン型ハイライトの補助線を表示する",
                "Draw scan-based highlight overlays in the world.");
        bool(rows, "highlightExclusiveMode", LocalFeatureSettings.WORKSITE_VISIBILITY_EXCLUSIVE_MODE,
                "ハイライト排他モード", "Exclusive Highlight Mode",
                "スキャン型ハイライトを同時に1つまでに制限する",
                "Keep at most one scan-based highlight mode active at a time.");

        header(rows, "detail.highlight.materialTargets", "鉱石・資材の対象", "Ore and Material Targets");
        for (ChiseBooleanSetting option : resourceTargets) {
            boolTarget(rows, option, isSpecialMaterialTarget(option)
                    ? "対象資材を発光枠で強調する"
                    : "対象鉱石を発光枠で強調する");
        }

        header(rows, "detail.highlight.hiddenTargets", "隠れブロックの対象", "Hidden Block Targets");
        for (ChiseBooleanSetting option : visibilityTargets) {
            boolTarget(rows, option, targetDescription(option));
        }
    }

    private void addAnalyzerDetailRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "detail.analyzer.lava", "溶岩源", "Lava Source");
        integer(rows, "lavaRange", LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS,
                "検出範囲", "Source Range",
                "読み込み済みチャンク内で溶岩源を確認する水平範囲",
                "Horizontal radius used for lava source detection in already-loaded chunks.", 1);
        integer(rows, "lavaVerticalRange", LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS,
                "垂直範囲", "Vertical Range",
                "溶岩源を確認する垂直範囲", "Vertical radius used for lava source detection.", 1);
        integer(rows, "lavaInterval", LocalFeatureSettings.LAVA_ANALYZER_INTERVAL,
                "更新間隔", "Update Interval",
                "動的な溶岩源を再確認するtick間隔", "Ticks between dynamic lava source detection updates.", 5);
        integer(rows, "lavaMaxOverlays", LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS,
                "最大表示数", "Maximum Markers",
                "同時に保持する溶岩源マーカー数", "Maximum retained lava source markers.", 1);

        header(rows, "detail.analyzer.ancientDebris", "古代の残骸", "Ancient Debris");
        integer(rows, "ancientDebrisRange", LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_RANGE,
                "検出範囲", "Detection Range",
                "ネザーのロード済みチャンク内で古代の残骸を表示する最大距離（最大256ブロック）",
                "Maximum Ancient Debris marker distance in already-loaded Nether chunks (up to 256 blocks).", 16);
        integer(rows, "ancientDebrisMaxMarkers", LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS,
                "最大表示数", "Maximum Markers",
                "同時に保持する古代の残骸マーカー数",
                "Maximum retained Ancient Debris markers.", 8);
    }

    private void resetHighlightDetails() {
        LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_EXCLUSIVE_MODE.resetToDefault();
        resetTargetGroup(VisualTargetGroupPolicy.Group.MATERIAL);
        resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
    }

    private void resetAnalyzerDetails() {
        LocalFeatureSettings.LAVA_ANALYZER_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_INTERVAL.resetToDefault();
        LocalFeatureSettings.LAVA_ANALYZER_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_RANGE.resetToDefault();
        LocalFeatureSettings.ANCIENT_DEBRIS_ANALYZER_MAX_MARKERS.resetToDefault();
    }

    private void header(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
                        String japaneseName, String englishName) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, japanese ? japaneseName : englishName));
    }

    private void compactBool(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
                             ChiseBooleanSetting config, String japaneseName, String englishName) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                id, japanese ? japaneseName : englishName, "", config));
    }

    private void compactBoolAction(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
                                   ChiseBooleanSetting config, String japaneseName, String englishName,
                                   ChiseTweaksSettingRowDefinition.Action action) {
        rows.add(ChiseTweaksSettingRowDefinition.boolAction(
                id, japanese ? japaneseName : englishName, "", config, action,
                japanese ? "設定" : "Settings"));
    }

    private void bool(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
                      ChiseBooleanSetting config, String japaneseName, String englishName,
                      String japaneseDescription, String englishDescription) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                id, japanese ? japaneseName : englishName,
                japanese ? japaneseDescription : englishDescription, config));
    }

    private void boolTarget(ArrayList<ChiseTweaksSettingRowDefinition> rows,
                            ChiseBooleanSetting config, String japaneseDescription) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                config.getName(), compactTargetName(config.getDisplayName(japanese)),
                japanese ? japaneseDescription : config.getComment(false), config));
    }

    private void integer(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
                         ChiseIntegerSetting config, String japaneseName, String englishName,
                         String japaneseDescription, String englishDescription, int step) {
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                id, japanese ? japaneseName : englishName,
                japanese ? japaneseDescription : englishDescription, config, step));
    }

    private void action(ArrayList<ChiseTweaksSettingRowDefinition> rows, String id,
                        String japaneseName, String japaneseDescription,
                        ChiseTweaksSettingRowDefinition.Action action) {
        rows.add(ChiseTweaksSettingRowDefinition.action(
                id, japanese ? japaneseName : englishActionName(action),
                japanese ? japaneseDescription : englishActionDescription(action),
                action, japanese ? "設定" : "Settings"));
    }

    private static String englishActionName(ChiseTweaksSettingRowDefinition.Action action) {
        return switch (action) {
            case OPEN_HIGHLIGHT_DETAILS -> "Highlight settings";
            case OPEN_LAVA_DETAILS -> "Analyzer settings";
            case EDIT_BLOCK_FILTER -> "Block targets";
            case EDIT_ENTITY_FILTER -> "Entity targets";
            case EDIT_ORE_COMPAT -> "Modded ore targets";
        };
    }

    private static String englishActionDescription(ChiseTweaksSettingRowDefinition.Action action) {
        return switch (action) {
            case OPEN_HIGHLIGHT_DETAILS -> "Open highlight target and scan settings.";
            case OPEN_LAVA_DETAILS -> "Open Lava Source and Ancient Debris analyzer settings.";
            case EDIT_BLOCK_FILTER -> "Edit the block include/exclude mode and block IDs.";
            case EDIT_ENTITY_FILTER -> "Edit the entity include/exclude mode and entity IDs.";
            case EDIT_ORE_COMPAT -> "Edit modded block IDs and their Chise highlight styles.";
        };
    }

    private void resetTargetGroup(VisualTargetGroupPolicy.Group group) {
        if (group == VisualTargetGroupPolicy.Group.MATERIAL) {
            VisualTargetSettings.setAllOreHighlightTargets(true);
            return;
        }
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask = VisualTargetGroupPolicy.withAll(config.visualTargetMask, group, true);
        VisualTargetSettings.init();
    }

    private static List<ChiseBooleanSetting> targets(String prefix) {
        ArrayList<ChiseBooleanSetting> result = new ArrayList<>();
        for (ChiseBooleanSetting option : VisualTargetSettings.ALL_OPTIONS) {
            if (option.getName().startsWith(prefix)) result.add(option);
        }
        return List.copyOf(result);
    }

    private static boolean isSpecialMaterialTarget(ChiseBooleanSetting option) {
        if (option == null) return false;
        String name = option.getName();
        return name.endsWith("Obsidian") || name.endsWith("CryingObsidian");
    }

    private String compactTargetName(String value) {
        if (!japanese || value == null) return value == null ? "" : value;
        int colon = value.indexOf('：');
        return colon >= 0 && colon + 1 < value.length() ? value.substring(colon + 1) : value;
    }

    private static String targetDescription(ChiseBooleanSetting option) {
        String name = option.getName();
        if (name.endsWith("BlueIce")) return "見えにくい青氷を確認";
        if (name.endsWith("DeadCoral")) return "サンゴ系ブロックを確認";
        if (name.endsWith("PowderSnow")) return "粉雪を視認しやすくする";
        if (name.endsWith("SculkCatalyst")) return "周辺作業時に確認しやすくする";
        return "見つけやすくする";
    }
}
