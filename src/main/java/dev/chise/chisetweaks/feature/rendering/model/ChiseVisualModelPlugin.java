package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Non-destructive Chise-owned Ore Highlights model composition.
 *
 * <p>The active Minecraft/resource-pack block model is never replaced. Fabric first bakes the final
 * block model selected by vanilla and every active resource pack, then Chise wraps that baked model
 * and appends a separate Chise-owned overlay extra model at render time. This preserves third-party
 * base geometry, UVs, particles and texture choices instead of rebuilding a substitute cube model.
 *
 * <p>Both static and animated overlay assets are loaded once with Fabric's extra-model API. Runtime
 * settings only decide whether an already-loaded Chise overlay is emitted; they never trigger a
 * resource-pack reload. Shader state also does not select a different visual language.
 */
public final class ChiseVisualModelPlugin {
    public static final String REVISION = "ore-highlight-composed-overlay-5-nondestructive";

    private static final Map<Target, OverlayModels> OVERLAY_MODELS = Map.ofEntries(
            overlays(Target.MATERIAL_COAL_ORE, "coal"),
            overlays(Target.MATERIAL_IRON_ORE, "iron"),
            overlays(Target.MATERIAL_COPPER_ORE, "copper"),
            overlays(Target.MATERIAL_GOLD_ORE, "gold"),
            overlays(Target.MATERIAL_LAPIS_ORE, "lapis"),
            overlays(Target.MATERIAL_REDSTONE_ORE, "redstone"),
            overlays(Target.MATERIAL_DIAMOND_ORE, "diamond"),
            overlays(Target.MATERIAL_EMERALD_ORE, "emerald"),
            overlays(Target.MATERIAL_NETHER_GOLD_ORE, "nether_gold"),
            overlays(Target.MATERIAL_NETHER_QUARTZ_ORE, "nether_quartz"),
            overlays(Target.MATERIAL_ANCIENT_DEBRIS, "ancient_debris"),
            overlays(Target.MATERIAL_OBSIDIAN, "obsidian"),
            overlays(Target.MATERIAL_CRYING_OBSIDIAN, "crying_obsidian"));

    private static final Map<Block, Target> BLOCK_TARGETS = Map.ofEntries(
            Map.entry(Blocks.COAL_ORE, Target.MATERIAL_COAL_ORE),
            Map.entry(Blocks.DEEPSLATE_COAL_ORE, Target.MATERIAL_COAL_ORE),
            Map.entry(Blocks.IRON_ORE, Target.MATERIAL_IRON_ORE),
            Map.entry(Blocks.DEEPSLATE_IRON_ORE, Target.MATERIAL_IRON_ORE),
            Map.entry(Blocks.COPPER_ORE, Target.MATERIAL_COPPER_ORE),
            Map.entry(Blocks.DEEPSLATE_COPPER_ORE, Target.MATERIAL_COPPER_ORE),
            Map.entry(Blocks.GOLD_ORE, Target.MATERIAL_GOLD_ORE),
            Map.entry(Blocks.DEEPSLATE_GOLD_ORE, Target.MATERIAL_GOLD_ORE),
            Map.entry(Blocks.LAPIS_ORE, Target.MATERIAL_LAPIS_ORE),
            Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, Target.MATERIAL_LAPIS_ORE),
            Map.entry(Blocks.REDSTONE_ORE, Target.MATERIAL_REDSTONE_ORE),
            Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, Target.MATERIAL_REDSTONE_ORE),
            Map.entry(Blocks.DIAMOND_ORE, Target.MATERIAL_DIAMOND_ORE),
            Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, Target.MATERIAL_DIAMOND_ORE),
            Map.entry(Blocks.EMERALD_ORE, Target.MATERIAL_EMERALD_ORE),
            Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, Target.MATERIAL_EMERALD_ORE),
            Map.entry(Blocks.NETHER_GOLD_ORE, Target.MATERIAL_NETHER_GOLD_ORE),
            Map.entry(Blocks.NETHER_QUARTZ_ORE, Target.MATERIAL_NETHER_QUARTZ_ORE),
            Map.entry(Blocks.ANCIENT_DEBRIS, Target.MATERIAL_ANCIENT_DEBRIS),
            Map.entry(Blocks.OBSIDIAN, Target.MATERIAL_OBSIDIAN),
            Map.entry(Blocks.CRYING_OBSIDIAN, Target.MATERIAL_CRYING_OBSIDIAN));

    private ChiseVisualModelPlugin() {}

    public static void register() {
        OreHighlightRenderInvalidation.register();

        ModelLoadingPlugin.register(pluginContext -> {
            for (OverlayModels overlay : OVERLAY_MODELS.values()) {
                pluginContext.addModel(
                        overlay.staticKey(),
                        SimpleUnbakedExtraModel.blockStateModel(overlay.staticModel()));
                pluginContext.addModel(
                        overlay.animatedKey(),
                        SimpleUnbakedExtraModel.blockStateModel(overlay.animatedModel()));
            }

            pluginContext.modifyBlockModelAfterBake().register(
                    ModelModifier.WRAP_PHASE,
                    (model, context) -> {
                        Target target = targetFor(context.state());
                        if (target == null) return model;
                        OverlayModels overlay = OVERLAY_MODELS.get(target);
                        if (overlay == null) return model;
                        return new FullbrightOreHighlightModel(
                                model,
                                target,
                                overlay.staticKey(),
                                overlay.animatedKey());
                    });

            ChiseTweaksClient.LOGGER.info(
                    "Visual model {} active in ChiseTweaks {}; {} material families use non-destructive shader-invariant overlays",
                    REVISION,
                    ChiseTweaksMetadata.MOD_VERSION,
                    OVERLAY_MODELS.size());
        });
    }

    private static Target targetFor(BlockState state) {
        return state == null ? null : BLOCK_TARGETS.get(state.getBlock());
    }

    private static Map.Entry<Target, OverlayModels> overlays(Target target, String highlightKey) {
        Identifier staticModel = Identifier.fromNamespaceAndPath(
                ChiseTweaksMetadata.MOD_ID,
                "block/visual/overlay/" + highlightKey + "_static");
        Identifier animatedModel = Identifier.fromNamespaceAndPath(
                ChiseTweaksMetadata.MOD_ID,
                "block/visual/overlay/" + highlightKey + "_animated");
        ExtraModelKey<BlockStateModel> staticKey = ExtraModelKey.create(staticModel::toString);
        ExtraModelKey<BlockStateModel> animatedKey = ExtraModelKey.create(animatedModel::toString);
        return Map.entry(target, new OverlayModels(staticModel, animatedModel, staticKey, animatedKey));
    }

    private record OverlayModels(
            Identifier staticModel,
            Identifier animatedModel,
            ExtraModelKey<BlockStateModel> staticKey,
            ExtraModelKey<BlockStateModel> animatedKey) {}
}
