package dev.chise.chisetweaks.mixin.masa;

import dev.chise.chisetweaks.integration.masa.MasaReflectionSupport;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(
        targets = "me.fallenbreath.tweakermore.impl.features.autoContainerProcess.processors.ContainerMaterialListItemCollector",
        remap = false)
public abstract class TweakerMoreMaterialRefreshMixin {
    @Inject(
            method = "process",
            at = @At("RETURN"),
            require = 0,
            remap = false)
    private void chiseTweaks$refreshMaterialList(
            LocalPlayer player,
            AbstractContainerScreen<?> screen,
            List<Slot> allSlots,
            List<Slot> playerSlots,
            List<Slot> containerSlots,
            CallbackInfoReturnable<Object> callbackInfo) {
        MasaReflectionSupport.refreshTweakerMoreMaterialListIfConfigured();
    }
}
