package dev.chise.chisetweaks.mixin.sodium;

import dev.chise.chisetweaks.runtime.FeatureManager;
import dev.chise.chisetweaks.feature.rendering.LavaHighlightConfig;
import dev.chise.chisetweaks.feature.rendering.LavaHighlightFeature;
import dev.chise.chisetweaks.runtime.ExternalHookCircuitBreaker;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies ChiseTweaks lava highlight colors after Sodium 0.9.1 lights a quad. */
@Mixin(value = DefaultFluidRenderer.class, remap = false)
public abstract class LavaHighlightRendererMixin {
    @Shadow @Final private int[] quadColors;

    @Inject(method = "updateQuad", at = @At("TAIL"))
    private void chisetweaks$highlightLava(
            ModelQuadViewMutable quad, LevelSlice level, BlockPos pos, LightPipeline lighter,
            Direction direction, ModelQuadFacing facing, float brightness,
            ColorProvider<FluidState> colorProvider, FluidState fluidState, CallbackInfo ci) {
        ExternalHookCircuitBreaker.Hook hook =
                ExternalHookCircuitBreaker.Hook.SODIUM_LAVA_HIGHLIGHT;
        if (ExternalHookCircuitBreaker.isOpen(hook)) {
            return;
        }
        try {
            if (fluidState.getType() != Fluids.LAVA
                    && fluidState.getType() != Fluids.FLOWING_LAVA) {
                return;
            }
            LavaHighlightFeature feature = FeatureManager.getInstance().getLavaHighlightFeature();
            if (feature == null || !feature.isEnabled()) {
                return;
            }
            LavaHighlightConfig config = feature.getConfig();
            boolean applyHighlight = false;
            int requestedColor = 0;
            if (fluidState.isSource() && config.isHighlightSource()) {
                requestedColor = config.getSourceColor();
                applyHighlight = true;
            } else if (!fluidState.isSource() && config.isHighlightFlowing()) {
                requestedColor = config.getFlowingColor();
                applyHighlight = true;
            }
            // ARGB 0xFFFFFFFF is a valid user color. Never reuse it as a "no highlight" sentinel.
            if (!applyHighlight) {
                return;
            }

            int red = requestedColor >> 16 & 0xFF;
            int green = requestedColor >> 8 & 0xFF;
            int blue = requestedColor & 0xFF;
            for (int vertex = 0; vertex < 4; vertex++) {
                int existing = this.quadColors[vertex];
                float light = (existing >> 16 & 0xFF) / 255.0F;
                int finalRed = (int) (red * light);
                int finalGreen = (int) (green * light);
                int finalBlue = (int) (blue * light);
                this.quadColors[vertex] = 0xFF000000
                        | finalRed << 16 | finalGreen << 8 | finalBlue;
            }
        } catch (RuntimeException | LinkageError failure) {
            ExternalHookCircuitBreaker.trip(hook, failure);
        }
    }
}
