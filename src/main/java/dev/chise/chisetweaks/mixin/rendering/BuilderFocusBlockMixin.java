package dev.chise.chisetweaks.mixin.rendering;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BuilderFocusBlockMixin {
    @Shadow protected abstract BlockState asState();

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void chiseTweaks$hideFilteredBlock(CallbackInfoReturnable<RenderShape> result) {
        if (!FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue()) return;
        if (BuilderFocusVisibility.shouldHide(asState().getBlock())) {
            result.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
