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
                    "Placement: Anvil", "Allow Placement Guide to mark anvils."),
            entry(Target.PLACEMENT_BEEHIVE, "visualTargetPlacementBeehive",
                    "Placement: Beehive", "Allow Placement Guide to mark beehives."),
            entry(Target.PLACEMENT_CAMPFIRE, "visualTargetPlacementCampfire",
                    "Placement: Campfire", "Allow Placement Guide to mark campfires."),
            entry(Target.PLACEMENT_GLAZED_TERRACOTTA, "visualTargetPlacementGlazedTerracotta",
                    "Placement: Glazed Terracotta", "Allow Placement Guide to mark glazed terracotta."),
            entry(Target.PLACEMENT_GRINDSTONE, "visualTargetPlacementGrindstone",
                    "Placement: Grindstone", "Allow Placement Guide to mark grindstones."),
            entry(Target.PLACEMENT_FENCE_GATE, "visualTargetPlacementFenceGate",
                    "Placement: Fence Gate", "Allow Placement Guide to mark fence gates."),
            entry(Target.PLACEMENT_FROGLIGHT, "visualTargetPlacementFroglight",
                    "Placement: Froglight", "Allow Placement Guide to mark froglights."),
            entry(Target.PLACEMENT_SLAB, "visualTargetPlacementSlab",
                    "Placement: Slabs", "Allow Placement Guide to mark slab placement state."),
            entry(Target.PLACEMENT_STAIRS, "visualTargetPlacementStairs",
                    "Placement: Stairs", "Allow Placement Guide to mark stair placement state."),
            entry(Target.PLACEMENT_TRAPDOOR, "visualTargetPlacementTrapdoor",
                    "Placement: Trapdoors", "Allow Placement Guide to mark trapdoor placement state."),
            entry(Target.PLACEMENT_LOG_WOOD, "visualTargetPlacementLogWood",
                    "Placement: Logs & Wood", "Allow Placement Guide to mark log, wood, stem and hyphae axes."),

            entry(Target.MATERIAL_OBSIDIAN, "visualTargetMaterialObsidian",
                    "Material: Obsidian", "Allow Material Highlights to mark visible obsidian."),
            entry(Target.MATERIAL_ANCIENT_DEBRIS, "visualTargetMaterialAncientDebris",
                    "Material: Ancient Debris", "Allow Material Highlights to mark visible ancient debris."),
            entry(Target.MATERIAL_DIAMOND_ORE, "visualTargetMaterialDiamondOre",
                    "Material: Diamond Ore", "Highlight both normal and deepslate diamond ore."),
            entry(Target.MATERIAL_GOLD_ORE, "visualTargetMaterialGoldOre",
                    "Material: Gold Ore", "Highlight both normal and deepslate gold ore."),
            entry(Target.MATERIAL_EMERALD_ORE, "visualTargetMaterialEmeraldOre",
                    "Material: Emerald Ore", "Highlight both normal and deepslate emerald ore."),
            entry(Target.MATERIAL_COAL_ORE, "visualTargetMaterialCoalOre",
                    "Material: Coal Ore", "Highlight both normal and deepslate coal ore."),
            entry(Target.MATERIAL_IRON_ORE, "visualTargetMaterialIronOre",
                    "Material: Iron Ore", "Highlight both normal and deepslate iron ore."),
            entry(Target.MATERIAL_COPPER_ORE, "visualTargetMaterialCopperOre",
                    "Material: Copper Ore", "Highlight both normal and deepslate copper ore."),
            entry(Target.MATERIAL_LAPIS_ORE, "visualTargetMaterialLapisOre",
                    "Material: Lapis Ore", "Highlight both normal and deepslate lapis ore."),
            entry(Target.MATERIAL_REDSTONE_ORE, "visualTargetMaterialRedstoneOre",
                    "Material: Redstone Ore", "Highlight both normal and deepslate redstone ore."),

            entry(Target.HIDDEN_BLUE_ICE, "visualTargetHiddenBlueIce",
                    "Hidden Surface: Blue Ice", "Allow Hidden Surface Trace to mark visible blue ice."),
            entry(Target.HIDDEN_DEAD_CORAL, "visualTargetHiddenDeadCoral",
                    "Hidden Surface: Dead Coral", "Allow Hidden Surface Trace to mark dead coral variants."),
            entry(Target.HIDDEN_POWDER_SNOW, "visualTargetHiddenPowderSnow",
                    "Hidden Surface: Powder Snow", "Allow Hidden Surface Trace to mark visible powder snow."),
            entry(Target.HIDDEN_SCULK_CATALYST, "visualTargetHiddenSculkCatalyst",
                    "Hidden Surface: Sculk Catalyst", "Allow Hidden Surface Trace to mark sculk catalysts."));

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
        for (Entry entry : ENTRIES) {
            String base = "config.option." + entry.option().getName().toLowerCase();
            entry.option().setPrettyName(StringUtils.getTranslatedOrFallback(
                    base + ".name", entry.fallbackName()));
            entry.option().setComment(StringUtils.getTranslatedOrFallback(
                    base + ".comment", entry.fallbackComment()));
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
            String fallbackName,
            String fallbackComment) {
        return new Entry(
                target,
                new ConfigBoolean(configName, true, fallbackComment),
                fallbackName,
                fallbackComment);
    }

    private record Entry(
            Target target,
            ConfigBoolean option,
            String fallbackName,
            String fallbackComment) {}
}
