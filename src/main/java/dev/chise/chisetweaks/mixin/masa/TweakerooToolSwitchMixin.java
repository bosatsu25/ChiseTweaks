package dev.chise.chisetweaks.mixin.masa;

import dev.chise.chisetweaks.integration.masa.MasaGuardRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.tweakeroo.util.InventoryUtils", remap = false)
public abstract class TweakerooToolSwitchMixin {
    @Inject(
            method = "trySwitchToEffectiveTool",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void chiseTweaks$guardToolSwitch(BlockPos pos, CallbackInfo callbackInfo) {
        Minecraft client = Minecraft.getInstance();
        if (pos == null || client.level == null || !client.level.isLoaded(pos)) return;
        if (!MasaGuardRuntime.allowTweakerooToolSwitch(client.level.getBlockState(pos))) {
            callbackInfo.cancel();
        }
    }
}
