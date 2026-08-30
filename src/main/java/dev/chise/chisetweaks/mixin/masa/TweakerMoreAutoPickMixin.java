package dev.chise.chisetweaks.mixin.masa;

import dev.chise.chisetweaks.integration.masa.MasaGuardRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.fallenbreath.tweakermore.impl.features.schematicProPlace.SchematicBlockPicker", remap = false)
public abstract class TweakerMoreAutoPickMixin {
    @Inject(
            method = "doSchematicWorldPickBlock",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void chiseTweaks$guardAutoPick(
            Minecraft client,
            BlockPos pos,
            InteractionHand hand,
            CallbackInfo callbackInfo) {
        if (client == null || client.player == null || hand == null) return;
        if (!MasaGuardRuntime.allowTweakerMoreAutoPick(client.player.getItemInHand(hand))) {
            callbackInfo.cancel();
        }
    }
}
