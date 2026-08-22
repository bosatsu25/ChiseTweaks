package dev.chise.chisetweaks.mixin.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.core.vision.FireVisibilityPolicy;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraftの一人称視点に重なる炎エフェクトだけを下げ、ワールド上の炎モデルやテクスチャは変更しない。
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class FireVisibilityMixin {
    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void chiseTweaks$lowerFireOverlay(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            TextureAtlasSprite sprite,
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
            TextureAtlasSprite sprite,
            CallbackInfo callbackInfo) {
        if (!FireVisibilityPolicy.shouldLower(
                LocalFeatureSwitches.FIRE_VISIBILITY.getBooleanValue())) return;
        poseStack.popPose();
    }
}
