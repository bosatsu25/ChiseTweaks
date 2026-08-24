package dev.chise.chisetweaks.mixin.rendering;

import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block Filterを個別rendererではなく、全BlockEntity共通のstate抽出境界へ適用する。 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BuilderFocusBlockEntityMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState>
            void chiseTweaks$hideFilteredBlockEntity(
                    E blockEntity,
                    float partialTick,
                    ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                    CallbackInfoReturnable<S> result) {
        if (blockEntity != null
                && BuilderFocusVisibility.shouldHide(blockEntity.getBlockState().getBlock())) {
            result.setReturnValue(null);
        }
    }
}
