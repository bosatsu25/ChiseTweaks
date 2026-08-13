package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.CompletableFuture;

/** Non-destructive Ore Highlights composition for vanilla and modded blocks. */
public final class ChiseVisualModelPlugin {
    public static final String REVISION = "ore-highlight-composed-overlay-6-modded-compatible";

    private ChiseVisualModelPlugin() {}

    public static void register() {
        OreHighlightRenderInvalidation.register();

        PreparableModelLoadingPlugin.register(
                (sharedState, executor) -> CompletableFuture.supplyAsync(
                        () -> OreHighlightResourceCompatibilityLoader.load(sharedState.resourceManager()),
                        executor),
                (resourceEntries, pluginContext) -> {
                    OreHighlightExternalRegistry.replaceResourceBlocks(resourceEntries);

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
                            (model, context) -> wrap(model, context.state()));

                    ChiseTweaksClient.LOGGER.info(
                            "Visual model {} active in ChiseTweaks {}; {} resource ore mapping(s) loaded",
                            REVISION,
                            ChiseTweaksMetadata.MOD_VERSION,
                            resourceEntries.size());
                });
    }

    private static net.minecraft.client.renderer.block.dispatch.BlockStateModel wrap(
            net.minecraft.client.renderer.block.dispatch.BlockStateModel model,
            BlockState state) {
        if (state == null) return model;
        OreHighlightResolver.Resolved resolved = OreHighlightResolver.resolve(state);
        if (resolved != null && resolved.target() != null && resolved.style() != null) {
            OreHighlightOverlayCatalog.OverlayModels overlay =
                    OreHighlightOverlayCatalog.forStyle(resolved.style());
            if (overlay != null) {
                return new FullbrightOreHighlightModel(
                        model,
                        resolved.target(),
                        overlay.staticKey(),
                        overlay.animatedKey());
            }
        }

        String namespace = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        return "minecraft".equals(namespace) ? model : new FullbrightOreHighlightModel(model);
    }
}
