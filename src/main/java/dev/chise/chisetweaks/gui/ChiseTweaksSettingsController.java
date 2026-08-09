package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.ConfigUiLocalization;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import dev.chise.chisetweaks.core.vision.VisualTargetGroupPolicy;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns config/domain mapping for the Chise settings UI.
 *
 * <p>The Minecraft Screen is responsible only for widgets, layout and rendering. This controller
 * keeps feature/target grouping, bulk operations, resets and persistence out of the view layer.</p>
 */
final class ChiseTweaksSettingsController {
    private final boolean japanese;
    private final List<IConfigBase> placementTargets;
    private final List<IConfigBase> resourceTargets;
    private final List<IConfigBase> visibilityTargets;

    ChiseTweaksSettingsController(boolean japanese) {
        this.japanese = japanese;
        this.placementTargets = targets("visualTargetPlacement");
        this.resourceTargets = targets("visualTargetMaterial");
        this.visibilityTargets = targets("visualTargetHidden");
    }

    static ChiseTweaksSettingsController forCurrentLanguage() {
        return new ChiseTweaksSettingsController("ja".equals(
                StringUtils.getTranslatedOrFallback(
                        "screen.chisetweaks.help.language.probe", "en")));
    }

    void initialize() {
        LocalFeatureSettings.init();
        VisualTargetSettings.init();
        ConfigUiLocalization.refresh();
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
            case HOTKEYS -> addHotkeyRows(rows);
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
                LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
                resetTargetGroup(VisualTargetGroupPolicy.Group.HIDDEN);
            }
            case HOTKEYS, HELP -> {
                return false;
            }
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
        for (IConfigBase option : placementTargets) {
            boolTarget(rows, option, "向きや設置状態を確認しやすくする");
        }
    }

    private void addResourceRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "header.resources", "資源", "Resources");
        bool(rows, "materials", FeatureSwitches.MATERIAL_HIGHLIGHTS,
                "鉱石ハイライト", "鉱石や資源を見つけやすくする");
        bool(rows, "nether", FeatureSwitches.NETHER_PALETTE,
                "ネザー配色ガイド", "ネザーの資源を見分けやすくする");
        header(rows, "header.resourceTargets", "ハイライト対象", "Highlight targets");
        for (IConfigBase option : resourceTargets) {
            boolTarget(rows, option, "対象資源を個別にON/OFFする");
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
        for (IConfigBase option : visibilityTargets) {
            boolTarget(rows, option, targetDescription(option));
        }

        header(rows, "header.sceneFilter", "表示を絞る対象", "Scene Filter");
        bool(rows, "focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS,
                "ブロック", "必要なブロックだけ見やすくする");
        bool(rows, "focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES,
                "エンティティ", "必要なエンティティだけを表示する");

        header(rows, "header.visibilityDetails", "溶岩・視認の詳細設定", "Lava & visibility details");
        bool(rows, "lava", LocalFeatureSwitches.LAVA_HIGHLIGHT,
                "溶岩ハイライト", "溶岩やマグマを強調表示する");
        integer(rows, "scanRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS,
                "視認スキャン範囲", "周辺を確認する水平範囲", 1);
        integer(rows, "scanInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL,
                "スキャン間隔", "周辺確認を行うtick間隔", 5);
    }

    private void addHotkeyRows(ArrayList<ChiseTweaksSettingRowDefinition> rows) {
        header(rows, "header.hotkeys", "キー設定", "Keybinds");
        for (FeatureSwitch feature : FeatureSwitches.VALUES) {
            rows.add(ChiseTweaksSettingRowDefinition.action(
                    feature.getName(),
                    display(feature),
                    japanese ? "キー割り当てを編集する" : "Edit the key binding"));
        }
    }

    private void header(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            String japaneseName,
            String englishName) {
        rows.add(ChiseTweaksSettingRowDefinition.header(
                id,
                japanese ? japaneseName : englishName));
    }

    private void bool(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            fi.dy.masa.malilib.config.IConfigBoolean config,
            String japaneseName,
            String japaneseDescription) {
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                id,
                japanese ? japaneseName : display(config),
                japanese ? japaneseDescription : comment(config),
                config));
    }

    private void boolTarget(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            IConfigBase base,
            String japaneseDescription) {
        if (!(base instanceof fi.dy.masa.malilib.config.IConfigBoolean config)) return;
        rows.add(ChiseTweaksSettingRowDefinition.bool(
                base.getName(),
                compactTargetName(display(base)),
                japanese ? japaneseDescription : comment(base),
                config));
    }

    private void integer(
            ArrayList<ChiseTweaksSettingRowDefinition> rows,
            String id,
            fi.dy.masa.malilib.config.options.ConfigInteger config,
            String japaneseName,
            String japaneseDescription,
            int step) {
        rows.add(ChiseTweaksSettingRowDefinition.integer(
                id,
                japanese ? japaneseName : display(config),
                japanese ? japaneseDescription : comment(config),
                config,
                step));
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
            case HOTKEYS, HELP -> null;
        };
    }

    private static List<IConfigBase> targets(String prefix) {
        ArrayList<IConfigBase> result = new ArrayList<>();
        for (IConfigBase option : VisualTargetSettings.ALL_OPTIONS) {
            if (option.getName() != null && option.getName().startsWith(prefix)) result.add(option);
        }
        return List.copyOf(result);
    }

    private String display(IConfigBase config) {
        String value = config.getConfigGuiDisplayName();
        return value == null || value.isBlank() ? config.getName() : value;
    }

    private static String comment(IConfigBase config) {
        String value = config.getComment();
        return value == null ? "" : value;
    }

    private String compactTargetName(String value) {
        if (!japanese || value == null) return value == null ? "" : value;
        int colon = value.indexOf('：');
        return colon >= 0 && colon + 1 < value.length() ? value.substring(colon + 1) : value;
    }

    private static String targetDescription(IConfigBase option) {
        String name = option.getName();
        if (name == null) return "見つけやすくする";
        if (name.endsWith("BlueIce")) return "見えにくい青氷を確認";
        if (name.endsWith("DeadCoral")) return "サンゴ系ブロックを確認";
        if (name.endsWith("PowderSnow")) return "粉雪を視認しやすくする";
        if (name.endsWith("SculkCatalyst")) return "周辺作業時に確認しやすくする";
        return "見つけやすくする";
    }
}
