package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public final class ChiseVisualModelPlugin {
    public static final String REVISION = "ore-highlight-composed-overlay-6-modded-data-driven";

    private ChiseVisualModelPlugin() {}

    public static void register() {
        OreHighlightRenderInvalidation.register();
        PreparableModelLoadingPlugin.register(
                OreHighlightResourceCompatibility::load,
                (resourceMappings, pluginContext) -> {
                    OreHighlightExternalRegistry.replaceResourceBlocks(resourceMappings);
                    OreHighlightOverlayCatalog.clearBakedCache();
                    for (OreHighlightOverlayCatalog.OverlayModels overlay : OreHighlightOverlayCatalog.values()) {
                        pluginContext.addModel(
                                overlay.staticKey(),
                                SimpleUnbakedExtraModel.blockStateModel(overlay.staticModel()));
                        pluginContext.addModel(
                                overlay.animatedKey(),
                                SimpleUnbakedExtraModel.blockStateModel(overlay.animatedModel()));
                    }
                    pluginContext.modifyBlockModelAfterBake().register(
                            ModelModifier.WRAP_PHASE,
                            (model, context) -> shouldWrap(context.state())
                                    ? new FullbrightOreHighlightModel(model, context.state())
                                    : model);
                    ChiseTweaksClient.LOGGER.info(
                            "Visual model {} active in ChiseTweaks {}; compatible ore overlays enabled",
                            REVISION,
                            ChiseTweaksMetadata.MOD_VERSION);
                });
    }

    private static boolean shouldWrap(BlockState state) {
        if (state == null) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return !"minecraft".equals(id.getNamespace()) || OreHighlightResolver.isCandidate(state);
    }
}
