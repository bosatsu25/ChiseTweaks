package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import dev.chise.chisetweaks.core.vision.GlassHighlightTargetPolicy;
import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.CompletableFuture;

/** Non-destructive Chise model composition for Ore, Kelp and Glass Highlights. */
public final class ChiseVisualModelPlugin {
    public static final String REVISION = "visual-model-overlay-9-glass-zero-scan";

    private static volatile boolean modelPipelineReady;

    private ChiseVisualModelPlugin() {}

    public static void register() {
        OreHighlightCompatibilityConfig.load();
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
                    pluginContext.addModel(
                            KelpHighlightOverlayCatalog.KEY,
                            SimpleUnbakedExtraModel.blockStateModel(KelpHighlightOverlayCatalog.MODEL));
                    pluginContext.addModel(
                            GlassHighlightOverlayCatalog.BLOCK_KEY,
                            SimpleUnbakedExtraModel.blockStateModel(GlassHighlightOverlayCatalog.BLOCK_MODEL));
                    pluginContext.addModel(
                            GlassHighlightOverlayCatalog.PANE_KEY,
                            SimpleUnbakedExtraModel.blockStateModel(GlassHighlightOverlayCatalog.PANE_MODEL));

                    pluginContext.modifyBlockModelAfterBake().register(
                            ModelModifier.WRAP_PHASE,
                            (model, context) -> wrap(model, context.state()));

                    modelPipelineReady = true;
                    ChiseTweaksClient.LOGGER.info(
                            "Visual model {} active in ChiseTweaks {}; {} resource ore mapping(s) loaded; zero-scan target wrapping ready",
                            REVISION,
                            ChiseTweaksMetadata.MOD_VERSION,
                            resourceEntries.size());
                });
    }

    static boolean isModelPipelineReady() {
        return modelPipelineReady;
    }

    private static net.minecraft.client.renderer.block.dispatch.BlockStateModel wrap(
            net.minecraft.client.renderer.block.dispatch.BlockStateModel model,
            BlockState state) {
        if (state == null) return model;
        var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return model;
        String namespace = blockId.getNamespace();
        String path = blockId.getPath();

        if ("minecraft".equals(namespace)
                && ("kelp".equals(path) || "kelp_plant".equals(path))) {
            return new FullbrightKelpHighlightModel(model);
        }

        GlassHighlightTargetPolicy.Shape glassShape =
                GlassHighlightTargetPolicy.classify(namespace, path);
        if (glassShape != GlassHighlightTargetPolicy.Shape.NONE) {
            return new FullbrightGlassHighlightModel(
                    model,
                    glassShape,
                    GlassHighlightOverlayCatalog.keyFor(glassShape));
        }

        // Resolve once while models are being baked. Non-target blocks keep the original model and
        // never enter Chise's runtime emitQuads/createGeometryKey path.
        OreHighlightResolver.Resolved resolved = OreHighlightResolver.resolve(state);
        if (resolved == null || resolved.style() == null) return model;

        OreHighlightOverlayCatalog.OverlayModels overlay =
                OreHighlightOverlayCatalog.forStyle(resolved.style());
        if (overlay == null) return model;
        return new FullbrightOreHighlightModel(
                model,
                resolved.target(),
                overlay.staticKey(),
                overlay.animatedKey());
    }
}
