package dev.chise.chisetweaks.mixin.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.core.vision.HandheldSizePolicy;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scales only the submitted first-person item model.
 *
 * <p>Vanilla hand animation, item model, active resource packs, use animation and input remain
 * owned by Minecraft. No model JSON or texture is replaced.</p>
 */
@Mixin(ItemInHandRenderer.class)
public abstract class HandheldSizeMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void chiseTweaks$scaleFirstPersonItem(
            LivingEntity mob,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo callbackInfo) {
        float scale = currentScale(displayContext, itemStack);
        if (scale >= 0.999F) return;
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void chiseTweaks$restoreFirstPersonItem(
            LivingEntity mob,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo callbackInfo) {
        if (currentScale(displayContext, itemStack) < 0.999F) poseStack.popPose();
    }

    private static float currentScale(ItemDisplayContext displayContext, ItemStack stack) {
        return HandheldSizePolicy.scaleFactor(
                FeatureSwitches.HANDHELD_SIZE.getBooleanValue(),
                displayContext,
                stack,
                LocalFeatureSettings.HANDHELD_BLOCK_SCALE.getIntegerValue(),
                LocalFeatureSettings.HANDHELD_ITEM_SCALE.getIntegerValue(),
                LocalFeatureSettings.HANDHELD_TOOL_SCALE.getIntegerValue());
    }
}
