package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.feature.Feature;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;

/**
 * Feature to highlight lava source blocks and flowing lava.
 * Uses Fabric API's FluidRenderHandler for stable fluid color customization.
 */
public class LavaHighlightFeature implements Feature {
    private final LavaHighlightConfig config;

    public LavaHighlightFeature() {
        this.config = new LavaHighlightConfig();
    }

    @Override
    public String getId() {
        return FeatureDefinition.LAVA_HIGHLIGHT.id();
    }

    @Override
    public String getName() {
        return FeatureDefinition.LAVA_HIGHLIGHT.englishName();
    }

    @Override
    public void init() {
        registerFluidHandler();
        ChiseTweaksClient.LOGGER.info("LavaHighlight feature initialized");
    }

    private void registerFluidHandler() {
        LavaFluidRenderHandler tintSource = new LavaFluidRenderHandler(config);
        FluidModel.Unbaked model = new FluidModel.Unbaked(
                new Material(Identifier.withDefaultNamespace("block/lava_still")),
                new Material(Identifier.withDefaultNamespace("block/lava_flow")),
                null,
                tintSource);
        FluidRenderingRegistry.register(Fluids.LAVA, Fluids.FLOWING_LAVA, model);

        ChiseTweaksClient.LOGGER.info("Registered custom lava render handler");
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    public LavaHighlightConfig getConfig() {
        return config;
    }
}
