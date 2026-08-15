package dev.chise.chisetweaks.mixin.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.core.vision.FireVisibilityPolicy;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers only Minecraft's first-person fire screen effect.
 *
 * <p>The world fire model and all fire textures remain owned by Minecraft or the active resource
 * pack. With the feature disabled, this mixin performs no pose mutation.</p>
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class FireVisibilityMixin {
    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void chiseTweaks$lowerFireOverlay(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CallbackInfo callbackInfo) {
        float offset = FireVisibilityPolicy.verticalOffset(
                LocalFeatureSwitches.FIRE_VISIBILITY.getBooleanValue());
        if (offset == 0.0F) return;
        poseStack.pushPose();
        poseStack.translate(0.0F, offset, 0.0F);
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void chiseTweaks$restoreFireOverlay(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CallbackInfo callbackInfo) {
        if (!FireVisibilityPolicy.shouldLower(
                LocalFeatureSwitches.FIRE_VISIBILITY.getBooleanValue())) return;
        poseStack.popPose();
    }
}
