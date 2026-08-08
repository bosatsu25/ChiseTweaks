package dev.chise.chisetweaks.mixin.building;

import dev.chise.chisetweaks.feature.building.PumpkinScaffoldFeature;
import dev.chise.chisetweaks.runtime.FeatureManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps one vanilla use-item invocation with Pumpkin Scaffold's temporary state.
 *
 * <p>The slightly lower mixin priority lets common default-priority input
 * interceptors make their decision before this feature mutates client state.</p>
 */
@Mixin(value = Minecraft.class, priority = 900)
abstract class PumpkinScaffoldInputMixin {
    @Unique
    private PumpkinScaffoldFeature.UseSession chiseTweaks$pumpkinScaffoldSession;

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void chiseTweaks$beginPumpkinScaffold(CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        chiseTweaks$restorePumpkinScaffoldSession(client);

        PumpkinScaffoldFeature feature = FeatureManager.getInstance().getPumpkinScaffoldFeature();
        if (feature == null) return;
        chiseTweaks$pumpkinScaffoldSession = feature.beginUse(client);
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void chiseTweaks$endPumpkinScaffold(CallbackInfo callback) {
        chiseTweaks$restorePumpkinScaffoldSession((Minecraft) (Object) this);
    }

    /**
     * Recovery guard for a use-item invocation that unwound before the RETURN
     * injection. If another layer catches that failure, temporary input state
     * is restored before the next client tick performs normal work.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void chiseTweaks$recoverPumpkinScaffoldSession(CallbackInfo callback) {
        chiseTweaks$restorePumpkinScaffoldSession((Minecraft) (Object) this);
    }

    @Unique
    private void chiseTweaks$restorePumpkinScaffoldSession(Minecraft client) {
        PumpkinScaffoldFeature.UseSession session = chiseTweaks$pumpkinScaffoldSession;
        chiseTweaks$pumpkinScaffoldSession = null;
        if (session != null) session.restore(client);
    }
}
