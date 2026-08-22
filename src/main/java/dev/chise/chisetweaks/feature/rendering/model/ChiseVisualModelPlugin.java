package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import dev.chise.chisetweaks.core.vision.GlassHighlightTargetPolicy;
import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ChiseVisualModelPlugin {
    public static final String REVISION = "visual-model-overlay-10-classification-cache";

    private static volatile boolean modelPipelineReady;

    private ChiseVisualModelPlugin() {}

    public static void register() {
        OreHighlightCompatibilityConfig.load();

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

                    // 1回のリソースモデル再読み込み中は分類結果を固定し、再読み込みをまたいで古い分類を持ち越さない。

                    ConcurrentHashMap<Block, VisualModelClassification> classificationCache =
                            new ConcurrentHashMap<>();
                    pluginContext.modifyBlockModelAfterBake().register(
                            ModelModifier.WRAP_PHASE,
                            (model, context) -> wrap(model, context.state(), classificationCache));

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
            BlockState state,
            ConcurrentHashMap<Block, VisualModelClassification> classificationCache) {
        if (state == null) return model;
        VisualModelClassification classification = classificationCache.computeIfAbsent(
                state.getBlock(),
                ignored -> classify(state));

        return switch (classification.kind()) {
            case NONE -> model;
            case KELP -> new FullbrightKelpHighlightModel(model);
            case GLASS -> new FullbrightGlassHighlightModel(
                    model,
                    classification.glassShape(),
                    GlassHighlightOverlayCatalog.keyFor(classification.glassShape()));
            case ORE -> new FullbrightOreHighlightModel(
                    model,
                    classification.target(),
                    classification.overlay().staticKey(),
                    classification.overlay().animatedKey());
        };
    }

    private static VisualModelClassification classify(BlockState state) {
        var blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return VisualModelClassification.NONE;
        String namespace = blockId.getNamespace();
        String path = blockId.getPath();

        if ("minecraft".equals(namespace)
                && ("kelp".equals(path) || "kelp_plant".equals(path))) {
            return VisualModelClassification.kelp();
        }

        GlassHighlightTargetPolicy.Shape glassShape =
                GlassHighlightTargetPolicy.classify(namespace, path);
        if (glassShape != GlassHighlightTargetPolicy.Shape.NONE) {
            return VisualModelClassification.glass(glassShape);
        }

        OreHighlightResolver.Resolved resolved = OreHighlightResolver.resolve(state);
        if (resolved == null || resolved.style() == null) return VisualModelClassification.NONE;

        OreHighlightOverlayCatalog.OverlayModels overlay =
                OreHighlightOverlayCatalog.forStyle(resolved.style());
        if (overlay == null) return VisualModelClassification.NONE;
        return VisualModelClassification.ore(resolved.target(), overlay);
    }

    private enum VisualKind {
        NONE,
        KELP,
        GLASS,
        ORE
    }

    private record VisualModelClassification(
            VisualKind kind,
            GlassHighlightTargetPolicy.Shape glassShape,
            Target target,
            OreHighlightOverlayCatalog.OverlayModels overlay) {
        private static final VisualModelClassification NONE = new VisualModelClassification(
                VisualKind.NONE,
                GlassHighlightTargetPolicy.Shape.NONE,
                null,
                null);

        private static VisualModelClassification kelp() {
            return new VisualModelClassification(
                    VisualKind.KELP,
                    GlassHighlightTargetPolicy.Shape.NONE,
                    null,
                    null);
        }

        private static VisualModelClassification glass(GlassHighlightTargetPolicy.Shape shape) {
            return new VisualModelClassification(VisualKind.GLASS, shape, null, null);
        }

        private static VisualModelClassification ore(
                Target target,
                OreHighlightOverlayCatalog.OverlayModels overlay) {
            return new VisualModelClassification(
                    VisualKind.ORE,
                    GlassHighlightTargetPolicy.Shape.NONE,
                    target,
                    overlay);
        }
    }
}
