package dev.chise.chisetweaks.mixin.masa;

import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.SyncmaticaRemoveGuardPolicy;
import dev.chise.chisetweaks.integration.masa.MasaReflectionSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "ch.endte.syncmatica.litematica.gui.WidgetSyncmaticaServerPlacementEntry$ButtonListener",
        remap = false)
public abstract class SyncmaticaRemoveListenerMixin {
    @Inject(
            method = "actionPerformedWithButton",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false)
    private void chiseTweaks$guardRemove(
            @Coerce Object button,
            int mouseButton,
            CallbackInfo callbackInfo) {
        if (!MasaReflectionSupport.isSyncmaticaRemoveListener(this)) return;
        MasaIntegrationConfig config = MasaIntegrationConfig.getInstance();
        SyncmaticaRemoveGuardPolicy.Decision decision = SyncmaticaRemoveGuardPolicy.decide(
                config.syncmaticaRemoveDisabled,
                config.syncmaticaRemoveRequireShift,
                MasaReflectionSupport.isShiftDown());
        if (decision.allowed()) return;
        callbackInfo.cancel();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String message = decision == SyncmaticaRemoveGuardPolicy.Decision.DENY_DISABLED
                    ? "Syncmatica: 削除はChiseTweaksで無効化されています"
                    : "Syncmatica: 削除するにはShiftを押してください";
            client.player.displayClientMessage(Component.literal(message), true);
        }
    }
}
