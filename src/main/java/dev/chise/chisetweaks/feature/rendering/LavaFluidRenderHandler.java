package dev.chise.chisetweaks.feature.rendering;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;

/**
 * Custom FluidRenderHandler for lava that applies highlight colors.
 * Uses Fabric API for stable, crash-free fluid rendering customization.
 */
public class LavaFluidRenderHandler implements BlockTintSource {
    private final LavaHighlightConfig config;

    public LavaFluidRenderHandler(LavaHighlightConfig config) {
        this.config = config;
    }

    @Override
    public int color(BlockState state) {
        return LavaVisionPalettePolicy.color(
                config.isEnabled(),
                state.getFluidState().isSource(),
                config.isHighlightSource(),
                config.isHighlightFlowing(),
                config.getSourceColor(),
                config.getFlowingColor());
    }
}
