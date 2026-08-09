package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.VisualModelSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Chise-owned ore-highlight block-model path.
 *
 * <p>The vanilla/resource-pack block remains the normally lit base layer. Chise replaces only the
 * selected ore model with a generated base-plus-overlay model, then wraps that baked model so the
 * slightly expanded overlay quads render at full brightness. No world light, block emission,
 * server state, or packets are modified.</p>
 */
public final class ChiseVisualModelPlugin {
    public static final String REVISION = "ore-highlight-emissive-overlay-2";

    private static final Map<Block, ModelSpec> MODEL_REPLACEMENTS = Map.ofEntries(
            replacement(Blocks.OBSIDIAN, Target.MATERIAL_OBSIDIAN, "obsidian"),
            replacement(Blocks.ANCIENT_DEBRIS, Target.MATERIAL_ANCIENT_DEBRIS, "ancient_debris"),
            replacement(Blocks.DIAMOND_ORE, Target.MATERIAL_DIAMOND_ORE, "diamond_ore"),
            replacement(Blocks.DEEPSLATE_DIAMOND_ORE, Target.MATERIAL_DIAMOND_ORE, "deepslate_diamond_ore"),
            replacement(Blocks.GOLD_ORE, Target.MATERIAL_GOLD_ORE, "gold_ore"),
            replacement(Blocks.DEEPSLATE_GOLD_ORE, Target.MATERIAL_GOLD_ORE, "deepslate_gold_ore"),
            replacement(Blocks.EMERALD_ORE, Target.MATERIAL_EMERALD_ORE, "emerald_ore"),
            replacement(Blocks.DEEPSLATE_EMERALD_ORE, Target.MATERIAL_EMERALD_ORE, "deepslate_emerald_ore"),
            replacement(Blocks.COAL_ORE, Target.MATERIAL_COAL_ORE, "coal_ore"),
            replacement(Blocks.DEEPSLATE_COAL_ORE, Target.MATERIAL_COAL_ORE, "deepslate_coal_ore"),
            replacement(Blocks.IRON_ORE, Target.MATERIAL_IRON_ORE, "iron_ore"),
            replacement(Blocks.DEEPSLATE_IRON_ORE, Target.MATERIAL_IRON_ORE, "deepslate_iron_ore"),
            replacement(Blocks.COPPER_ORE, Target.MATERIAL_COPPER_ORE, "copper_ore"),
            replacement(Blocks.DEEPSLATE_COPPER_ORE, Target.MATERIAL_COPPER_ORE, "deepslate_copper_ore"),
            replacement(Blocks.LAPIS_ORE, Target.MATERIAL_LAPIS_ORE, "lapis_ore"),
            replacement(Blocks.DEEPSLATE_LAPIS_ORE, Target.MATERIAL_LAPIS_ORE, "deepslate_lapis_ore"),
            replacement(Blocks.REDSTONE_ORE, Target.MATERIAL_REDSTONE_ORE, "redstone_ore"),
            replacement(Blocks.DEEPSLATE_REDSTONE_ORE, Target.MATERIAL_REDSTONE_ORE, "deepslate_redstone_ore"));

    private ChiseVisualModelPlugin() {}

    public static void register() {
        ModelLoadingPlugin.register(pluginContext -> {
            int activeMaterialMask = desiredMaterialModelMask();
            VisualModelReloadCoordinator.markAppliedMaterialModelMask(activeMaterialMask);

            if (activeMaterialMask == 0) return;

            pluginContext.modifyBlockModelOnLoad().register(
                    ModelModifier.OVERRIDE_PHASE,
                    (model, context) -> {
                        Identifier replacement = replacementModel(context.state(), activeMaterialMask);
                        if (replacement == null) return model;
                        return new SingleVariant.Unbaked(new Variant(replacement)).asRoot();
                    });

            pluginContext.modifyBlockModelAfterBake().register(
                    ModelModifier.WRAP_PHASE,
                    (model, context) -> replacementModel(context.state(), activeMaterialMask) == null
                            ? model
                            : new FullbrightOreHighlightModel(model));

            ChiseTweaksClient.LOGGER.info(
                    "Visual model {} active in ChiseTweaks {}; {} ore target family/families use full-bright Chise overlays",
                    REVISION,
                    ChiseTweaksMetadata.MOD_VERSION,
                    Integer.bitCount(activeMaterialMask));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client ->
                VisualModelReloadCoordinator.observe(client, desiredMaterialModelMask()));
    }

    static int desiredMaterialModelMask() {
        return VisualModelSelectionPolicy.activeMaterialModelMask(
                FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue(),
                LocalFeatureConfig.getInstance().visualTargetMask);
    }

    private static Identifier replacementModel(BlockState state, int activeMaterialMask) {
        if (state == null) return null;
        ModelSpec spec = MODEL_REPLACEMENTS.get(state.getBlock());
        if (spec == null || !VisualModelSelectionPolicy.useMaterialTarget(activeMaterialMask, spec.target())) {
            return null;
        }
        return spec.model();
    }

    private static Map.Entry<Block, ModelSpec> replacement(Block block, Target target, String modelName) {
        Identifier model = Identifier.fromNamespaceAndPath(
                ChiseTweaksMetadata.MOD_ID,
                "block/visual/material/" + modelName);
        return Map.entry(block, new ModelSpec(target, model));
    }

    private record ModelSpec(Target target, Identifier model) {}
}
