package dev.chise.chisetweaks.mixin.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.core.vision.FireVisibilityPolicy;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MinecraftがrenderFireへ渡す現在のfire spriteをそのまま再利用し、一人称overlayの高さだけを調整する。
 * 通常炎・魂の炎を区別して複製せず、world fire・texture・resource reloadには触れない。
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class FireVisibilityMixin {
    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void chiseTweaks$lowerFireOverlay(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            TextureAtlasSprite sprite,
            CallbackInfo callbackInfo) {
        boolean enabled = FeatureSwitches.FIRE_VISIBILITY.getBooleanValue();
        if (!FireVisibilityPolicy.shouldLower(enabled)) return;

        int preset = LocalFeatureSettings.FIRE_VISIBILITY_SIZE.getIntegerValue();
        poseStack.pushPose();
        poseStack.translate(0.0F, FireVisibilityPolicy.verticalOffset(enabled, preset), 0.0F);
        poseStack.scale(1.0F, FireVisibilityPolicy.heightScale(enabled, preset), 1.0F);
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void chiseTweaks$restoreFireOverlay(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            TextureAtlasSprite sprite,
            CallbackInfo callbackInfo) {
        if (!FireVisibilityPolicy.shouldLower(
                FeatureSwitches.FIRE_VISIBILITY.getBooleanValue())) return;
        poseStack.popPose();
    }
}
