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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Non-destructive Chise-owned Ore Highlights model composition.
 *
 * <p>The final vanilla/resource-pack block model is always preserved. Chise appends only its own
 * overlay model after bake. Vanilla ore targets retain their existing per-family switches; modded
 * blocks are wrapped cheaply so conventional {@code c:ores} tags, resource-pack compatibility JSON,
 * user overrides, and API registrations can become active without replacing base models.</p>
 */
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
                            (model, context) -> shouldWrap(context.state())
                                    ? new FullbrightOreHighlightModel(model)
                                    : model);

                    ChiseTweaksClient.LOGGER.info(
                            "Visual model {} active in ChiseTweaks {}; {} resource-pack ore mapping(s) loaded",
                            REVISION,
                            ChiseTweaksMetadata.MOD_VERSION,
                            resourceEntries.size());
                });
    }

    private static boolean shouldWrap(BlockState state) {
        if (state == null) return false;
        String namespace = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        // Every non-vanilla block receives only the lightweight conditional wrapper. This is what
        // allows a server-synchronized conventional ore tag or a later user override to work without
        // a full resource-pack reload. Vanilla remains limited to Chise's explicit retained targets.
        return !"minecraft".equals(namespace) || OreHighlightResolver.resolve(state) != null;
    }
}
