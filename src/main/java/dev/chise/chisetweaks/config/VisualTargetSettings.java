package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

import java.util.List;

/**
 * Fine-grained target switches shown in the Chise category settings UI.
 *
 * <p>The target model is fully Chise-owned. Normal/deepslate ore variants intentionally share one
 * resource switch because the user selects a resource family, not a host-stone implementation.</p>
 */
public final class VisualTargetSettings {
    private static final List<Entry> ENTRIES = List.of(
            entry(Target.PLACEMENT_ANVIL, "visualTargetPlacementAnvil",
                    "Placement: Anvil", "設置方向：金床",
                    "Allow Placement Guide to mark anvils.", "設置方向ガイドで金床を表示対象にします。"),
            entry(Target.PLACEMENT_BEEHIVE, "visualTargetPlacementBeehive",
                    "Placement: Beehive", "設置方向：養蜂箱",
                    "Allow Placement Guide to mark beehives.", "設置方向ガイドで養蜂箱を表示対象にします。"),
            entry(Target.PLACEMENT_CAMPFIRE, "visualTargetPlacementCampfire",
                    "Placement: Campfire", "設置方向：焚き火",
                    "Allow Placement Guide to mark campfires.", "設置方向ガイドで焚き火を表示対象にします。"),
            entry(Target.PLACEMENT_GLAZED_TERRACOTTA, "visualTargetPlacementGlazedTerracotta",
                    "Placement: Glazed Terracotta", "設置方向：彩釉テラコッタ",
                    "Allow Placement Guide to mark glazed terracotta.", "設置方向ガイドで彩釉テラコッタを表示対象にします。"),
            entry(Target.PLACEMENT_GRINDSTONE, "visualTargetPlacementGrindstone",
                    "Placement: Grindstone", "設置方向：砥石",
                    "Allow Placement Guide to mark grindstones.", "設置方向ガイドで砥石を表示対象にします。"),
            entry(Target.PLACEMENT_FENCE_GATE, "visualTargetPlacementFenceGate",
                    "Placement: Fence Gate", "設置方向：フェンスゲート",
                    "Allow Placement Guide to mark fence gates.", "設置方向ガイドでフェンスゲートを表示対象にします。"),
            entry(Target.PLACEMENT_FROGLIGHT, "visualTargetPlacementFroglight",
                    "Placement: Froglight", "設置方向：フロッグライト",
                    "Allow Placement Guide to mark froglights.", "設置方向ガイドでフロッグライトを表示対象にします。"),
            entry(Target.PLACEMENT_SLAB, "visualTargetPlacementSlab",
                    "Placement: Slabs", "設置方向：ハーフブロック",
                    "Allow Placement Guide to mark slab placement state.", "設置方向ガイドでハーフブロックの上下状態を表示対象にします。"),
            entry(Target.PLACEMENT_STAIRS, "visualTargetPlacementStairs",
                    "Placement: Stairs", "設置方向：階段",
                    "Allow Placement Guide to mark stair placement state.", "設置方向ガイドで階段の向き・上下・形状を表示対象にします。"),
            entry(Target.PLACEMENT_TRAPDOOR, "visualTargetPlacementTrapdoor",
                    "Placement: Trapdoors", "設置方向：トラップドア",
                    "Allow Placement Guide to mark trapdoor placement state.", "設置方向ガイドでトラップドアの設置状態を表示対象にします。"),
            entry(Target.PLACEMENT_LOG_WOOD, "visualTargetPlacementLogWood",
                    "Placement: Logs & Wood", "設置方向：原木・木材",
                    "Allow Placement Guide to mark log, wood, stem and hyphae axes.", "設置方向ガイドで原木・木・幹・菌糸の軸を表示対象にします。"),

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

    private static boolean initialized;
    private static boolean syncing;

    private VisualTargetSettings() {}

    public static synchronized void init() {
        syncFromStorage();
        if (initialized) return;
        bindCallbacks();
        initialized = true;
    }

    /** Retained as a source-compatible no-op; labels are now owned by each setting. */
    public static void refreshTranslations() {}

    public static synchronized void setAllOreHighlightTargets(boolean enabled) {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask = VisualTargetSelectionPolicy.withAllOreHighlightTargets(
                config.visualTargetMask,
                enabled);
        config.save();
        syncFromStorage();
    }

    private static void syncFromStorage() {
        syncing = true;
        try {
            int mask = LocalFeatureConfig.getInstance().visualTargetMask;
            for (Entry entry : ENTRIES) {
                entry.option().setBooleanValueSilently(
                        VisualTargetSelectionPolicy.isEnabled(mask, entry.target()));
            }
        } finally {
            syncing = false;
        }
    }

    private static void bindCallbacks() {
        for (Entry entry : ENTRIES) {
            entry.option().setValueChangeCallback(ignored -> save(entry));
        }
    }

    private static void save(Entry entry) {
        if (syncing) return;
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                config.visualTargetMask,
                entry.target(),
                entry.option().getBooleanValue());
        config.save();
    }

    private static Entry entry(
            Target target,
            String configName,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {
        return new Entry(
                target,
                new SimpleBooleanSetting(
                        configName,
                        true,
                        englishName,
                        japaneseName,
                        englishComment,
                        japaneseComment));
    }

    private record Entry(Target target, SimpleBooleanSetting option) {}
}
