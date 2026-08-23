package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

import java.util.List;

/** {@link LocalFeatureConfig} のvisual target maskへ直接bindingするUI向けメタデータ。 */
public final class VisualTargetSettings {
    private static final Runnable NOOP = () -> {};
    private static Runnable materialTargetsChangedCallback = NOOP;

    private static final List<Entry> ENTRIES = List.of(
            entry(Target.MATERIAL_COAL_ORE, "visualTargetMaterialCoalOre",
                    "Ore: Coal", "鉱石：石炭",
                    "Toggle normal and deepslate Coal Ore together.", "通常版と深層岩版の石炭鉱石を1つのスイッチで切り替えます。"),
            entry(Target.MATERIAL_IRON_ORE, "visualTargetMaterialIronOre",
                    "Ore: Iron", "鉱石：鉄",
                    "Toggle normal and deepslate Iron Ore together.", "通常版と深層岩版の鉄鉱石を1つのスイッチで切り替えます。"),
            entry(Target.MATERIAL_COPPER_ORE, "visualTargetMaterialCopperOre",
                    "Ore: Copper", "鉱石：銅",
                    "Toggle normal and deepslate Copper Ore together.", "通常版と深層岩版の銅鉱石を1つのスイッチで切り替えます。"),
            entry(Target.MATERIAL_GOLD_ORE, "visualTargetMaterialGoldOre",
                    "Ore: Gold", "鉱石：金",
                    "Toggle normal and deepslate Gold Ore together.", "通常版と深層岩版の金鉱石を1つのスイッチで切り替えます。"),
            entry(Target.MATERIAL_LAPIS_ORE, "visualTargetMaterialLapisOre",
                    "Ore: Lapis", "鉱石：ラピスラズリ",
                    "Toggle normal and deepslate Lapis Ore together.", "通常版と深層岩版のラピスラズリ鉱石を1つのスイッチで切り替えます。"),
            entry(Target.MATERIAL_REDSTONE_ORE, "visualTargetMaterialRedstoneOre",
                    "Ore: Redstone", "鉱石：レッドストーン",
                    "Toggle normal and deepslate Redstone Ore together.", "通常版と深層岩版のレッドストーン鉱石を1つのスイッチで切り替えます。"),
            entry(Target.MATERIAL_DIAMOND_ORE, "visualTargetMaterialDiamondOre",
                    "Ore: Diamond", "鉱石：ダイヤモンド",
                    "Toggle normal and deepslate Diamond Ore together.", "通常版と深層岩版のダイヤモンド鉱石を1つのスイッチで切り替えます。"),
            entry(Target.MATERIAL_EMERALD_ORE, "visualTargetMaterialEmeraldOre",
                    "Ore: Emerald", "鉱石：エメラルド",
                    "Toggle normal and deepslate Emerald Ore together.", "通常版と深層岩版のエメラルド鉱石を1つのスイッチで切り替えます。"),
            entry(Target.MATERIAL_NETHER_GOLD_ORE, "visualTargetMaterialNetherGoldOre",
                    "Nether Resource: Gold Ore", "ネザー資源：金鉱石",
                    "Highlight Nether Gold Ore without changing Netherrack.", "ネザーラックは変更せず、ネザー金鉱石だけを強調します。"),
            entry(Target.MATERIAL_NETHER_QUARTZ_ORE, "visualTargetMaterialNetherQuartzOre",
                    "Nether Resource: Quartz Ore", "ネザー資源：クォーツ鉱石",
                    "Highlight Nether Quartz Ore without changing Netherrack.", "ネザーラックは変更せず、ネザークォーツ鉱石だけを強調します。"),
            entry(Target.MATERIAL_ANCIENT_DEBRIS, "visualTargetMaterialAncientDebris",
                    "Nether Resource: Ancient Debris", "ネザー資源：古代の残骸",
                    "Highlight Ancient Debris with its own muted whorl identity.", "古代の残骸を独立した表現で強調します。"),
            entry(Target.MATERIAL_OBSIDIAN, "visualTargetMaterialObsidian",
                    "Special Material: Obsidian", "特殊資材：黒曜石",
                    "Highlight Obsidian independently from Crying Obsidian.", "黒曜石を泣く黒曜石とは別に切り替えます。"),
            entry(Target.MATERIAL_CRYING_OBSIDIAN, "visualTargetMaterialCryingObsidian",
                    "Special Material: Crying Obsidian", "特殊資材：泣く黒曜石",
                    "Highlight Crying Obsidian independently from normal Obsidian.", "泣く黒曜石を通常の黒曜石とは別に切り替えます。"),
            entry(Target.HIDDEN_BLUE_ICE, "visualTargetHiddenBlueIce",
                    "Hidden Surface: Blue Ice", "隠面：青氷",
                    "Allow Hidden Surface Trace to mark visible blue ice.", "隠面トレースで見えている青氷を表示対象にします。"),
            entry(Target.HIDDEN_DEAD_CORAL, "visualTargetHiddenDeadCoral",
                    "Hidden Surface: Dead Coral", "隠面：死んだサンゴ",
                    "Allow Hidden Surface Trace to mark dead coral variants.", "隠面トレースで死んだサンゴ系ブロックを表示対象にします。"),
            entry(Target.HIDDEN_POWDER_SNOW, "visualTargetHiddenPowderSnow",
                    "Hidden Surface: Powder Snow", "隠面：粉雪",
                    "Allow Hidden Surface Trace to mark visible powder snow.", "隠面トレースで見えている粉雪を表示対象にします。"),
            entry(Target.HIDDEN_SCULK_CATALYST, "visualTargetHiddenSculkCatalyst",
                    "Hidden Surface: Sculk Catalyst", "隠面：スカルクカタリスト",
                    "Allow Hidden Surface Trace to mark sculk catalysts.", "隠面トレースでスカルクカタリストを表示対象にします。"));

    public static final List<ChiseBooleanSetting> ALL_OPTIONS = ENTRIES.stream()
            .map(entry -> (ChiseBooleanSetting) entry.option())
            .toList();

    private VisualTargetSettings() {}

    /** 起動処理の境界を明示するため残している。直接bindingのため状態同期処理は不要。 */
    public static void init() {}

    public static synchronized void setAllOreHighlightTargets(boolean enabled) {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        int previous = config.visualTargetMask;
        config.visualTargetMask = VisualTargetSelectionPolicy.withAllOreHighlightTargets(
                config.visualTargetMask,
                enabled);
        if (config.visualTargetMask != previous) materialTargetsChangedCallback.run();
    }

    public static void setMaterialTargetsChangedCallback(Runnable callback) {
        materialTargetsChangedCallback = callback == null ? NOOP : callback;
    }

    private static boolean isMaterialTarget(Target target) {
        return target != null
                && (target.bitMask() & VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK) != 0;
    }

    private static Entry entry(
            Target target,
            String configName,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {
        SimpleBooleanSetting option = new SimpleBooleanSetting(
                configName,
                true,
                englishName,
                japaneseName,
                englishComment,
                japaneseComment,
                () -> VisualTargetSelectionPolicy.isEnabled(
                        LocalFeatureConfig.getInstance().visualTargetMask,
                        target),
                enabled -> {
                    LocalFeatureConfig config = LocalFeatureConfig.getInstance();
                    config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                            config.visualTargetMask,
                            target,
                            enabled);
                });
        if (isMaterialTarget(target)) {
            option.setValueChangeCallback(ignored -> materialTargetsChangedCallback.run());
        }
        return new Entry(option);
    }

    private record Entry(SimpleBooleanSetting option) {}
}
