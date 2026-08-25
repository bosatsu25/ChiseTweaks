package dev.chise.chisetweaks.mixin.rendering;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block Entityの表示可否と視認性補助を同じstate抽出境界で処理する。 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityVisualStateMixin {
    private static final SpriteId BRIGHT_CHEST_SINGLE = brightChestSprite("normal");
    private static final SpriteId BRIGHT_CHEST_LEFT = brightChestSprite("normal_left");
    private static final SpriteId BRIGHT_CHEST_RIGHT = brightChestSprite("normal_right");

    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void chiseTweaks$filterBlockEntity(
            E blockEntity,
            float partialTick,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            CallbackInfoReturnable<S> callbackInfo) {
        if (blockEntity != null && BuilderFocusVisibility.shouldHide(blockEntity.getBlockState().getBlock())) {
            callbackInfo.setReturnValue(null);
        }
    }

    @Inject(method = "tryExtractRenderState", at = @At("RETURN"))
    private <E extends BlockEntity, S extends BlockEntityRenderState> void chiseTweaks$applyVisualState(
            E blockEntity,
            float partialTick,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            CallbackInfoReturnable<S> callbackInfo) {
        if (blockEntity == null
                || !blockEntity.getBlockState().is(Blocks.CHEST)
                || !FeatureSwitches.BRIGHT_CHEST.getBooleanValue()) return;
        S state = callbackInfo.getReturnValue();
        if (!(state instanceof ChestRenderState chest)) return;
        chest.customSprite = chest.type == ChestType.LEFT
                ? BRIGHT_CHEST_LEFT
                : chest.type == ChestType.RIGHT ? BRIGHT_CHEST_RIGHT : BRIGHT_CHEST_SINGLE;
    }

    private static SpriteId brightChestSprite(String name) {
        return Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath(
                ChiseTweaksMetadata.MOD_ID,
                "entity/chest/" + name));
    }
}
