package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.VisualModelSelectionPolicy;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chise-owned block-model visual path.
 *
 * <p>Phase 1 intentionally replaces only diamond ore. It proves that Chise can enter the same
 * block-model stage used by resource packs while keeping all texture/model assets original to
 * ChiseTweaks. Later phases can add the remaining visual targets after this path is confirmed in
 * game with Sodium, Iris and Litematica present.</p>
 */
public final class ChiseVisualModelPlugin {
    public static final String REVISION = "model-loading-poc-1";

    private static final Identifier DIAMOND_ORE_MODEL =
            Identifier.fromNamespaceAndPath(ChiseTweaksMetadata.MOD_ID, "block/visual/diamond_ore");
    private static final Identifier DEEPSLATE_DIAMOND_ORE_MODEL =
            Identifier.fromNamespaceAndPath(
                    ChiseTweaksMetadata.MOD_ID,
                    "block/visual/deepslate_diamond_ore");

    private ChiseVisualModelPlugin() {}

    public static void register() {
        ModelLoadingPlugin.register(pluginContext -> {
            boolean useDiamondModel = desiredDiamondModelState();
            VisualModelReloadCoordinator.markAppliedDiamondModelState(useDiamondModel);

            if (!useDiamondModel) return;

            pluginContext.modifyBlockModelOnLoad().register(
                    ModelModifier.OVERRIDE_PHASE,
                    (model, context) -> {
                        Identifier replacement = replacementModel(context.state());
                        if (replacement == null) return model;
                        return new SingleVariant.Unbaked(new Variant(replacement)).asRoot();
                    });

            ChiseTweaksClient.LOGGER.info(
                    "Visual model {} active in ChiseTweaks {}; diamond ore uses Chise block models",
                    REVISION,
                    ChiseTweaksMetadata.MOD_VERSION);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client ->
                VisualModelReloadCoordinator.observe(client, desiredDiamondModelState()));
    }

    static boolean desiredDiamondModelState() {
        return VisualModelSelectionPolicy.useDiamondOreModel(
                FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue(),
                LocalFeatureConfig.getInstance().visualTargetMask);
    }

    private static Identifier replacementModel(BlockState state) {
        if (state.getBlock() == Blocks.DIAMOND_ORE) return DIAMOND_ORE_MODEL;
        if (state.getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE) return DEEPSLATE_DIAMOND_ORE_MODEL;
        return null;
    }
}
