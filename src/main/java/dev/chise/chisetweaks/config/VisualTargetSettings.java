package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.List;

/**
 * Fine-grained target switches shown only in the Target Lists tab.
 *
 * <p>Parent features remain in Features & Keybinds. These switches only narrow the target families
 * handled by Placement Guide, Material Highlights and Hidden Surface Trace.</p>
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

            entry(Target.MATERIAL_OBSIDIAN, "visualTargetMaterialObsidian",
                    "Material: Obsidian", "素材：黒曜石",
                    "Allow Material Highlights to mark visible obsidian.", "素材ハイライトで見えている黒曜石を表示対象にします。"),
            entry(Target.MATERIAL_ANCIENT_DEBRIS, "visualTargetMaterialAncientDebris",
                    "Material: Ancient Debris", "素材：古代の残骸",
                    "Allow Material Highlights to mark visible ancient debris.", "素材ハイライトで見えている古代の残骸を表示対象にします。"),
            entry(Target.MATERIAL_DIAMOND_ORE, "visualTargetMaterialDiamondOre",
                    "Material: Diamond Ore", "素材：ダイヤモンド鉱石",
                    "Highlight both normal and deepslate diamond ore.", "通常版と深層岩版のダイヤモンド鉱石をまとめて切り替えます。"),
            entry(Target.MATERIAL_GOLD_ORE, "visualTargetMaterialGoldOre",
                    "Material: Gold Ore", "素材：金鉱石",
                    "Highlight both normal and deepslate gold ore.", "通常版と深層岩版の金鉱石をまとめて切り替えます。"),
            entry(Target.MATERIAL_EMERALD_ORE, "visualTargetMaterialEmeraldOre",
                    "Material: Emerald Ore", "素材：エメラルド鉱石",
                    "Highlight both normal and deepslate emerald ore.", "通常版と深層岩版のエメラルド鉱石をまとめて切り替えます。"),
            entry(Target.MATERIAL_COAL_ORE, "visualTargetMaterialCoalOre",
                    "Material: Coal Ore", "素材：石炭鉱石",
                    "Highlight both normal and deepslate coal ore.", "通常版と深層岩版の石炭鉱石をまとめて切り替えます。"),
            entry(Target.MATERIAL_IRON_ORE, "visualTargetMaterialIronOre",
                    "Material: Iron Ore", "素材：鉄鉱石",
                    "Highlight both normal and deepslate iron ore.", "通常版と深層岩版の鉄鉱石をまとめて切り替えます。"),
            entry(Target.MATERIAL_COPPER_ORE, "visualTargetMaterialCopperOre",
                    "Material: Copper Ore", "素材：銅鉱石",
                    "Highlight both normal and deepslate copper ore.", "通常版と深層岩版の銅鉱石をまとめて切り替えます。"),
            entry(Target.MATERIAL_LAPIS_ORE, "visualTargetMaterialLapisOre",
                    "Material: Lapis Ore", "素材：ラピスラズリ鉱石",
                    "Highlight both normal and deepslate lapis ore.", "通常版と深層岩版のラピスラズリ鉱石をまとめて切り替えます。"),
            entry(Target.MATERIAL_REDSTONE_ORE, "visualTargetMaterialRedstoneOre",
                    "Material: Redstone Ore", "素材：レッドストーン鉱石",
                    "Highlight both normal and deepslate redstone ore.", "通常版と深層岩版のレッドストーン鉱石をまとめて切り替えます。"),

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

    public static final List<IConfigBase> ALL_OPTIONS = ENTRIES.stream()
            .map(entry -> (IConfigBase) entry.option())
            .toList();

    private static boolean initialized;
    private static boolean syncing;

    private VisualTargetSettings() {}

    public static synchronized void init() {
        if (initialized) {
            refreshTranslations();
            return;
        }
        syncFromStorage();
        bindCallbacks();
        initialized = true;
        refreshTranslations();
    }

    public static void refreshTranslations() {
        boolean japanese = "ja".equals(StringUtils.getTranslatedOrFallback(
                "screen.chisetweaks.help.language.probe", "en"));
        for (Entry entry : ENTRIES) {
            String base = "config.option." + entry.option().getName().toLowerCase();
            String fallbackName = japanese ? entry.japaneseName() : entry.englishName();
            String fallbackComment = japanese ? entry.japaneseComment() : entry.englishComment();
            entry.option().setPrettyName(StringUtils.getTranslatedOrFallback(
                    base + ".name", fallbackName));
            entry.option().setComment(StringUtils.getTranslatedOrFallback(
                    base + ".comment", fallbackComment));
        }
    }

    private static void syncFromStorage() {
        syncing = true;
        try {
            int mask = LocalFeatureConfig.getInstance().visualTargetMask;
            for (Entry entry : ENTRIES) {
                entry.option().setBooleanValue(
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
                new ConfigBoolean(configName, true, englishComment),
                englishName,
                japaneseName,
                englishComment,
                japaneseComment);
    }

    private record Entry(
            Target target,
            ConfigBoolean option,
            String englishName,
            String japaneseName,
            String englishComment,
            String japaneseComment) {}
}
