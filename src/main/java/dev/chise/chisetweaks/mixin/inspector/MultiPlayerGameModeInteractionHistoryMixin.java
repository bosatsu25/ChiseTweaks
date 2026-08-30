package dev.chise.chisetweaks.mixin.inspector;

import dev.chise.chisetweaks.gui.InteractionHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeInteractionHistoryMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "destroyBlock", at = @At("HEAD"), require = 0)
    private void chiseTweaks$recordBreak(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (minecraft.level != null && pos != null && minecraft.level.isLoaded(pos)) {
            InteractionHistory.recordBreak(pos, minecraft.level.getBlockState(pos));
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), require = 0)
    private void chiseTweaks$recordUseBlock(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (minecraft.level != null && hit != null && minecraft.level.isLoaded(hit.getBlockPos())) {
            InteractionHistory.recordUseBlock(
                    hit.getBlockPos(),
                    minecraft.level.getBlockState(hit.getBlockPos()));
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), require = 0)
    private void chiseTweaks$recordUseItem(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callbackInfo) {
        if (player != null && hand != null) {
            InteractionHistory.recordUseItem(player.getItemInHand(hand));
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), require = 0)
    private void chiseTweaks$recordAttack(
            Player player,
            Entity entity,
            CallbackInfo callbackInfo) {
        InteractionHistory.recordAttackEntity(entity);
    }

    @Inject(method = "interact", at = @At("HEAD"), require = 0)
    private void chiseTweaks$recordInteract(
            Player player,
            Entity entity,
            EntityHitResult hitResult,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> callbackInfo) {
        InteractionHistory.recordInteractEntity(entity);
    }
}
