package dev.chise.chisetweaks.mixin.rendering;

import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 除外対象エンティティは不可視かつ副作用のない描画状態へ置き換える。
 */
@Mixin(LevelRenderer.class)
public abstract class BuilderFocusEntityMixin {
    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private void chiseTweaks$hideFilteredEntity(
            Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> result) {
        if (!FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue()
                || !BuilderFocusVisibility.shouldHide(entity)) return;
        result.setReturnValue(invisibleState(entity, partialTick));
    }

    private static EntityRenderState invisibleState(Entity entity, float partialTick) {
        EntityRenderState state = new EntityRenderState();
        state.entityType = EntityType.INTERACTION;
        state.x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        state.y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        state.z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
        state.isInvisible = true;
        return state;
    }
}
