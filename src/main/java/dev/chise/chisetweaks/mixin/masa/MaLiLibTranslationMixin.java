package dev.chise.chisetweaks.mixin.masa;

import dev.chise.chisetweaks.integration.masa.MasaJapaneseUiRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "fi.dy.masa.malilib.util.StringUtils", remap = false)
public abstract class MaLiLibTranslationMixin {
    @Inject(
            method = "translate(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0,
            remap = false)
    private static void chiseTweaks$applyJapaneseUx(
            String key,
            Object[] args,
            CallbackInfoReturnable<String> callbackInfo) {
        String original = callbackInfo.getReturnValue();
        String translated = MasaJapaneseUiRuntime.translate(key, original);
        if (!translated.equals(original)) callbackInfo.setReturnValue(translated);
    }
}
