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

/** Owns config/domain mapping for the standalone Chise settings UI. */
final class ChiseTweaksSettingsController {
    private final boolean japanese;
    private final List<ChiseBooleanSetting> placementTargets;
    private final List<ChiseBooleanSetting> resourceTargets;
    private final List<ChiseBooleanSetting> visibilityTargets;

    ChiseTweaksSettingsController(boolean japanese) {
        this.japanese = japanese;
        this.placementTargets = targets("visualTargetPlacement");
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

    boolean japanese() {
        return japanese;
    }

    List<ChiseTweaksSettingRowDefinition> rowsFor(ChiseTweaksUiSection section) {
        if (section == null) return List.of();
        ArrayList<ChiseTweaksSettingRowDefinition> rows = new ArrayList<>();
        switch (section) {
            case PLACEMENT -> addPlacementRows(rows);
            case RESOURCES -> addResourceRows(rows);
            case VISIBILITY -> addVisibilityRows(rows);
            case HELP -> { }
        }
        return List.copyOf(rows);
    }

    boolean shouldTurnBulkOn(ChiseTweaksUiSection section) {
        VisualTargetGroupPolicy.Group group = groupFor(section);
        return group != null && !VisualTargetGroupPolicy.allEnabled(
                LocalFeatureConfig.getInstance().visualTargetMask,
                group);
    }

    void toggleBulk(ChiseTweaksUiSection section) {
        VisualTargetGroupPolicy.Group group = groupFor(section);
        if (group == null) return;
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        boolean enabled = !VisualTargetGroupPolicy.allEnabled(config.visualTargetMask, group);
        config.visualTargetMask = VisualTargetGroupPolicy.withAll(
                config.visualTargetMask,
                group,
                enabled);
        config.save();
        VisualTargetSettings.init();
    }

    boolean resetSection(ChiseTweaksUiSection section) {
        if (section == null) return false;
        switch (section) {
            case PLACEMENT -> {
                FeatureSwitches.PUMPKIN_SCAFFOLD.resetToDefault();
                FeatureSwitches.PLACEMENT_GUIDE.resetToDefault();
                LocalFeatureSettings.PUMPKIN_SCAFFOLD_PLACEMENT_RANGE.resetToDefault();
                resetTargetGroup(VisualTargetGroupPolicy.Group.PLACEMENT);
            }
            case RESOURCES -> {
                FeatureSwitches.MATERIAL_HIGHLIGHTS.resetToDefault();
                FeatureSwitches.NETHER_PALETTE.resetToDefault();
                resetTargetGroup(VisualTargetGroupPolicy.Group.MATERIAL);
            }
            case VISIBILITY -> {
                FeatureSwitches.FINE_THREAD_TRACE.resetToDefault();
                FeatureSwitches.HIDDEN_SURFACE_TRACE.resetToDefault();
                FeatureSwitches.GLASS_INSPECTION.resetToDefault();
                FeatureSwitches.BUILDER_FOCUS_BLOCKS.resetToDefault();
                FeatureSwitches.BUILDER_FOCUS_ENTITIES.resetToDefault();
                LocalFeatureSwitches.LAVA_HIGHLIGHT.resetToDefault();

                BuilderFocusConfig.REFRESH_RENDERER.resetToDefault();
                BuilderFocusConfig.BLOCK_RULE_MODE.resetToDefault();
                BuilderFocusConfig.BLOCK_WHITELIST.resetToDefault();
                BuilderFocusConfig.BLOCK_BLACKLIST.resetToDefault();
                BuilderFocusConfig.ENTITY_RULE_MODE.resetToDefault();
                BuilderFocusConfig.ENTITY_WHITELIST.resetToDefault();
                BuilderFocusConfig.ENTITY_BLACKLIST.resetToDefault();

                LocalFeatureSettings.LAVA_SOURCE.resetToDefault();
                LocalFeatureSettings.LAVA_FLOWING.resetToDefault();
                LocalFeatureSettings.LAVA_SOURCE_COLOR.resetToDefault();
                LocalFeatureSettings.LAVA_FLOWING_COLOR.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_EXCLUSIVE_MODE.resetToDefault();
                resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
            }
            case HELP -> { return false; }
        }
        return true;
    }

    void saveFeatureConfig() {
        FeatureConfig.saveToFile();
    }

    private void addPlacementRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "header.placement", "設置・向き", "Placement & Direction");
        bool(rows, "pumpkin", FeatureSwitches.PUMPKIN_SCAFFOLD,
                "Pumpkin Scaffold", "かぼちゃを使った設置作業を補助する");
        integer(rows, "pumpkinRange", LocalFeatureSettings.PUMPKIN_SCAFFOLD_PLACEMENT_RANGE,
                "かぼちゃ設置距離", "かぼちゃを置く最大距離", 1);
        bool(rows, "placementGuide", FeatureSwitches.PLACEMENT_GUIDE,
                "設置方向ガイド", "ブロックの向きや設置状態を見やすくする");
        header(rows, "header.placementTargets", "設置方向ガイドの対象", "Placement Guide targets");
        for (ChiseBooleanSetting option : placementTargets) {
            boolTarget(rows, option, "向きや設置状態を確認しやすくする");
        }
    }

    private void addResourceRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "header.resources", "資源", "Resources");
        bool(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS,
                "鉱石ハイライト",
                "バニラ鉱石" + VanillaOreVisualCatalog.blockVariantCount()
                        + "ブロック種をリソースパックなしで発光枠表示する");
        bool(rows, "nether", FeatureSwitches.NETHER_PALETTE,
                "ネザー配色ガイド", "ネザーの主要な建材・地形素材を色分けして見やすくする");

        header(rows, "header.vanillaOreTargets", "バニラ鉱石の対象", "Vanilla ore targets");
        for (ChiseBooleanSetting option : resourceTargets) {
            if (!isSpecialMaterialTarget(option)) {
                boolTarget(rows, option, "対象鉱石を発光枠で強調する");
            }
        }

        header(rows, "header.specialMaterialTargets", "特殊資材", "Special materials");
        for (ChiseBooleanSetting option : resourceTargets) {
            if (isSpecialMaterialTarget(option)) {
                boolTarget(rows, option, "対象資材を発光枠で強調する");
            }
        }
    }

    private void addVisibilityRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "header.visibility", "見やすさ", "Visibility");
        bool(rows, "thread", FeatureSwitches.FINE_THREAD_TRACE,
                "細線トレース", "細い補助線で輪郭を見やすくする");
        bool(rows, "hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                "隠面トレース", "見えにくいブロックを視認しやすくする");
        bool(rows, "glass", FeatureSwitches.GLASS_INSPECTION,
                "ガラス検査", "ガラスや板ガラスの境界を確認しやすくする");

        header(rows, "header.hiddenTargets", "見えにくいブロックの対象", "Hidden-surface targets");
        for (ChiseBooleanSetting option : visibilityTargets) {
            boolTarget(rows, option, targetDescription(option));
        }

        header(rows, "header.sceneFilter", "表示を絞る対象", "Scene Filter");
        bool(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS,
                "ブロック", "登録したルールでブロック表示を絞る");
        action(rows,
                "focusBlocksEdit",
                "ブロックの対象",
                "表示を残す／隠すブロックIDと方式を編集する",
                ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER);
        bool(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES,
                "エンティティ", "登録したルールでエンティティ表示を絞る");
        action(rows,
                "focusEntitiesEdit",
                "エンティティの対象",
                "表示を残す／隠すエンティティIDと方式を編集する",
                ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER);

        header(rows, "header.visibilityDetails", "Lava Analyzer・視認の詳細設定", "Lava Analyzer & visibility details");
        bool(rows, "lava", LocalFeatureSwitches.LAVA_HIGHLIGHT,
                "Lava Analyzer", "溶岩源を解析し、深緑の発光枠で1ブロックずつ表示する");
        integer(rows, "scanRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS,
                "視認スキャン範囲", "周辺を確認する水平範囲", 1);
        integer(rows, "scanInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL,
                "スキャン間隔", "周辺確認を行うtick間隔", 5);
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
            String japaneseDescription) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                id,
                japanese ? japaneseName : config.getDisplayName(false),
                japanese ? japaneseDescription : config.getComment(false),
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
            String japaneseDescription,
            int step) {
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                id,
                japanese ? japaneseName : config.getDisplayName(false),
                japanese ? japaneseDescription : config.getComment(false),
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
                },
                japanese ? japaneseDescription : switch (action) {
                    case EDIT_BLOCK_FILTER -> "Edit the block include/exclude mode and block IDs.";
                    case EDIT_ENTITY_FILTER -> "Edit the entity include/exclude mode and entity IDs.";
                },
                action,
                japanese ? "対象を編集" : "Edit targets"));
    }

    private void resetTargetGroup(VisualTargetGroupPolicy.Group group) {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask = VisualTargetGroupPolicy.withAll(
                config.visualTargetMask,
                group,
                true);
        config.save();
        VisualTargetSettings.init();
    }

    private static VisualTargetGroupPolicy.Group groupFor(ChiseTweaksUiSection section) {
        if (section == null) return null;
        return switch (section) {
            case PLACEMENT -> VisualTargetGroupPolicy.Group.PLACEMENT;
            case RESOURCES -> VisualTargetGroupPolicy.Group.MATERIAL;
            case VISIBILITY -> VisualTargetGroupPolicy.Group.HIDDEN;
            case HELP -> null;
        };
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
