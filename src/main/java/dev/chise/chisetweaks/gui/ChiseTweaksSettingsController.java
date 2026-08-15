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
import dev.chise.chisetweaks.core.vision.VanillaOreVisualCatalog;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Owns config/domain mapping for the focused standalone Chise settings UI. */
final class ChiseTweaksSettingsController {
    private static final List<ChiseBooleanSetting> HIGHLIGHT_FEATURES = List.of(
            FeatureSwitches.MATERIAL_HIGHLIGHTS,
            FeatureSwitches.NETHER_PALETTE,
            FeatureSwitches.FINE_THREAD_TRACE,
            FeatureSwitches.HIDDEN_SURFACE_TRACE,
            FeatureSwitches.GLASS_INSPECTION);

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
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        addHighlightRows(rows);
        addVisualFilterRows(rows);
        addVisibilityImprovementRows(rows);
        return List.copyOf(rows);
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

    boolean resetAll() {
        FeatureSwitches.MATERIAL_HIGHLIGHTS.resetToDefault();
        FeatureSwitches.NETHER_PALETTE.resetToDefault();
        FeatureSwitches.FINE_THREAD_TRACE.resetToDefault();
        FeatureSwitches.HIDDEN_SURFACE_TRACE.resetToDefault();
        FeatureSwitches.GLASS_INSPECTION.resetToDefault();
        FeatureSwitches.BUILDER_FOCUS_BLOCKS.resetToDefault();
        FeatureSwitches.BUILDER_FOCUS_ENTITIES.resetToDefault();
        LocalFeatureSwitches.LAVA_HIGHLIGHT.resetToDefault();
        LocalFeatureSwitches.FIRE_VISIBILITY.resetToDefault();

        LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault();
        LocalFeatureSettings.WORKSITE_VISIBILITY_EXCLUSIVE_MODE.resetToDefault();

        BuilderFocusConfig.REFRESH_RENDERER.resetToDefault();
        BuilderFocusConfig.BLOCK_RULE_MODE.resetToDefault();
        BuilderFocusConfig.BLOCK_WHITELIST.resetToDefault();
        BuilderFocusConfig.BLOCK_BLACKLIST.resetToDefault();
        BuilderFocusConfig.ENTITY_RULE_MODE.resetToDefault();
        BuilderFocusConfig.ENTITY_WHITELIST.resetToDefault();
        BuilderFocusConfig.ENTITY_BLACKLIST.resetToDefault();

        resetTargetGroup(VisualTargetGroupPolicy.Group.MATERIAL);
        resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
        return true;
    }

    boolean saveConfig() {
        boolean featureSaved = FeatureConfig.saveToFile();
        boolean localSaved = LocalFeatureConfig.getInstance().save();
        return featureSaved && localSaved;
    }

    private void addHighlightRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "header.highlight", "ハイライト", "Highlight");
        bool(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS,
                "鉱石ハイライト", "Ore Highlight",
                "鉱石や古代の残骸を発光枠で見つけやすくする",
                "Highlight ores and ancient debris with visible frames.");
        bool(rows, "nether", FeatureSwitches.NETHER_PALETTE,
                "ネザーハイライト", "Nether Highlight",
                "ネザーの主要な建材・地形素材を色分けして見やすくする",
                "Highlight visible Nether building materials with color-coded outlines.");
        bool(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE,
                "細線ハイライト", "Fine Line Highlight",
                "糸などの細く見えにくい対象を強調する",
                "Highlight thin visible targets such as tripwire and hooks.");
        bool(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                "隠れブロックハイライト", "Hidden Block Highlight",
                "粉雪など見分けにくいブロックを強調する",
                "Highlight blocks that are difficult to distinguish at a glance.");
        bool(rows, "glass", FeatureSwitches.GLASS_INSPECTION,
                "ガラスハイライト", "Glass Highlight",
                "ガラスや板ガラスの境界を見やすくする",
                "Highlight glass boundaries and pane connections.");

        bool(rows, "oreMotion", LocalFeatureSettings.ORE_HIGHLIGHT_ANIMATION,
                "ハイライトを動かす", "Animate Ore Highlight",
                "OFFでは発光する模様を静止表示し、ONで控えめにアニメーションする",
                "Keep the highlight static when OFF or use subtle animation when ON.");
        action(rows,
                "moddedOreTargets",
                "MOD鉱石の対象",
                "MOD鉱石のRegistry IDとChiseの発光スタイルを編集する",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT);
        for (ChiseBooleanSetting option : resourceTargets) {
            boolTarget(rows, option, isSpecialMaterialTarget(option)
                    ? "対象資材を発光枠で強調する"
                    : "対象鉱石を発光枠で強調する");
        }
        for (ChiseBooleanSetting option : visibilityTargets) {
            boolTarget(rows, option, targetDescription(option));
        }
    }

    private void addVisualFilterRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "header.visualFilter", "Visual Filter", "Visual Filter");
        bool(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS,
                "ブロックフィルター", "Block Filter",
                "登録したルールでブロック表示を絞る",
                "Filter block rendering with the configured visibility rules.");
        action(rows,
                "focusBlocksEdit",
                "ブロックの対象",
                "表示を残す／隠すブロックIDと方式を編集する",
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER);
        bool(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES,
                "エンティティフィルター", "Entity Filter",
                "登録したルールでエンティティ表示を絞る",
                "Filter entity rendering with the configured visibility rules.");
        action(rows,
                "focusEntitiesEdit",
                "エンティティの対象",
                "表示を残す／隠すエンティティIDと方式を編集する",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER);
    }

    private void addVisibilityImprovementRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "header.visibilityImprovement", "視認改善", "Visibility Improvements");
        bool(rows, "lava", LocalFeatureSwitches.LAVA_HIGHLIGHT,
                "溶岩解析", "Lava Analysis",
                "近くの溶岩源を解析し、壁越しでも距離に応じた深緑の発光枠で表示する",
                "Analyze nearby lava sources and show distance-aware deep-green frames through terrain.");
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                "fireVisibility",
                japanese ? "火炎表示を低くする" : "Lower Fire Overlay",
                japanese
                        ? "燃焼中の炎を画面下部へ寄せ、前方を見やすくする。ワールド上の炎は変更しない"
                        : "Lower only the first-person fire overlay. World fire remains unchanged.",
                LocalFeatureSwitches.FIRE_VISIBILITY));
        integer(rows, "scanRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS,
                "溶岩解析範囲", "Lava Analysis Range",
                "周辺の溶岩源を確認する水平範囲", "Horizontal radius used for nearby lava analysis.", 1);
        integer(rows, "scanInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL,
                "溶岩解析間隔", "Lava Analysis Interval",
                "周辺確認を行うtick間隔", "Ticks between nearby lava analysis updates.", 5);
    }

    private void header(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            String japaneseName,
            String englishName) {
        rows.add(ChiseTweaksSettingRowDefinition.header(id, japanese ? japaneseName : englishName));
    }

    private void bool(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            ChiseBooleanSetting config,
            String japaneseName,
            String englishName,
            String japaneseDescription,
            String englishDescription) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                id,
                japanese ? japaneseName : englishName,
                japanese ? japaneseDescription : englishDescription,
                config));
    }

    private void boolTarget(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            ChiseBooleanSetting config,
            String japaneseDescription) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                config.getName(),
                compactTargetName(config.getDisplayName(japanese)),
                japanese ? japaneseDescription : config.getComment(false),
                config));
    }

    private void integer(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            ChiseIntegerSetting config,
            String japaneseName,
            String englishName,
            String japaneseDescription,
            String englishDescription,
            int step) {
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                id,
                japanese ? japaneseName : englishName,
                japanese ? japaneseDescription : englishDescription,
                config,
                step));
    }

    private void action(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            String japaneseName,
            String japaneseDescription,
            ChiseTweaksSettingRowDefinition.Action action) {
        rows.add(ChiseTweaksSettingRowDefinition.action(
                id,
                japanese ? japaneseName : switch (action) {
                    case EDIT_BLOCK_FILTER -> "Block targets";
                    case EDIT_ENTITY_FILTER -> "Entity targets";
                    case EDIT_ORE_COMPAT -> "Modded ore targets";
                },
                japanese ? japaneseDescription : switch (action) {
                    case EDIT_BLOCK_FILTER -> "Edit the block include/exclude mode and block IDs.";
                    case EDIT_ENTITY_FILTER -> "Edit the entity include/exclude mode and entity IDs.";
                    case EDIT_ORE_COMPAT -> "Edit modded block IDs and their Chise highlight styles.";
                },
                action,
                japanese ? "対象を編集" : "Edit targets"));
    }

    private void resetTargetGroup(VisualTargetGroupPolicy.Group group) {
        if (group == VisualTargetGroupPolicy.Group.MATERIAL) {
            VisualTargetSettings.setAllOreHighlightTargets(true);
            return;
        }
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask = VisualTargetGroupPolicy.withAll(
                config.visualTargetMask,
                group,
                true);
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
