package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;

import java.util.List;

/** {@link LocalFeatureConfig} のvisual target maskへ直接bindingするUI向けsetting。表示文言はlang側を正本とする。 */
public final class VisualTargetSettings {
    private static final Runnable NOOP = () -> {};
    private static Runnable materialTargetsChangedCallback = NOOP;

    public static final List<ChiseBooleanSetting> ALL_OPTIONS = List.of(
            entry(Target.MATERIAL_COAL_ORE, "visualTargetMaterialCoalOre"),
            entry(Target.MATERIAL_IRON_ORE, "visualTargetMaterialIronOre"),
            entry(Target.MATERIAL_COPPER_ORE, "visualTargetMaterialCopperOre"),
            entry(Target.MATERIAL_GOLD_ORE, "visualTargetMaterialGoldOre"),
            entry(Target.MATERIAL_LAPIS_ORE, "visualTargetMaterialLapisOre"),
            entry(Target.MATERIAL_REDSTONE_ORE, "visualTargetMaterialRedstoneOre"),
            entry(Target.MATERIAL_DIAMOND_ORE, "visualTargetMaterialDiamondOre"),
            entry(Target.MATERIAL_EMERALD_ORE, "visualTargetMaterialEmeraldOre"),
            entry(Target.MATERIAL_NETHER_GOLD_ORE, "visualTargetMaterialNetherGoldOre"),
            entry(Target.MATERIAL_NETHER_QUARTZ_ORE, "visualTargetMaterialNetherQuartzOre"),
            entry(Target.MATERIAL_ANCIENT_DEBRIS, "visualTargetMaterialAncientDebris"),
            entry(Target.MATERIAL_OBSIDIAN, "visualTargetMaterialObsidian"),
            entry(Target.MATERIAL_CRYING_OBSIDIAN, "visualTargetMaterialCryingObsidian"),
            entry(Target.TECHNICAL_TRIPWIRE, "visualTargetTechnicalTripwire"),
            entry(Target.TECHNICAL_TRIPWIRE_HOOK, "visualTargetTechnicalTripwireHook"),
            entry(Target.HIDDEN_BLUE_ICE, "visualTargetHiddenBlueIce"),
            entry(Target.HIDDEN_DEAD_CORAL, "visualTargetHiddenDeadCoral"),
            entry(Target.HIDDEN_POWDER_SNOW, "visualTargetHiddenPowderSnow"),
            entry(Target.HIDDEN_SCULK_CATALYST, "visualTargetHiddenSculkCatalyst"));

    private VisualTargetSettings() {}

    public static synchronized void setAllOreHighlightTargets(boolean enabled) {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        int previous = config.visualTargetMask;
        config.visualTargetMask = VisualTargetSelectionPolicy.withAllOreHighlightTargets(
                config.visualTargetMask,
                enabled);
        if (config.visualTargetMask != previous) {
            SettingChangeDispatcher.markChanged();
            materialTargetsChangedCallback.run();
        }
    }

    public static void setMaterialTargetsChangedCallback(Runnable callback) {
        materialTargetsChangedCallback = callback == null ? NOOP : callback;
    }

    private static boolean isMaterialTarget(Target target) {
        return target != null
                && (target.bitMask() & VisualTargetSelectionPolicy.ORE_HIGHLIGHT_TARGETS_MASK) != 0;
    }

    private static ChiseBooleanSetting entry(Target target, String configName) {
        ChiseBooleanSetting option = new ChiseBooleanSetting(
                configName,
                true,
                () -> VisualTargetSelectionPolicy.isEnabled(
                        LocalFeatureConfig.getInstance().visualTargetMask,
                        target),
                enabled -> {
                    LocalFeatureConfig config = LocalFeatureConfig.getInstance();
                    config.visualTargetMask = VisualTargetSelectionPolicy.withEnabled(
                            config.visualTargetMask,
                            target,
                            enabled);
                },
                SettingPersistence.LOCAL_CONFIG);
        if (isMaterialTarget(target)) {
            option.setValueChangeCallback(ignored -> materialTargetsChangedCallback.run());
        }
        return option;
    }
}
