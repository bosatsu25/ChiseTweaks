package dev.chise.chisetweaks.mixin;

import dev.chise.chisetweaks.integration.masa.MasaModAvailability;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies optional external-mod mixins only when their owning mod is locally installed. */
public final class IntegrationMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Map<String, String> MIXIN_MODS = Map.of(
            "dev.chise.chisetweaks.mixin.masa.MaLiLibTranslationMixin", MasaModAvailability.MALILIB,
            "dev.chise.chisetweaks.mixin.masa.LitematicaMaterialCacheMixin", MasaModAvailability.LITEMATICA,
            "dev.chise.chisetweaks.mixin.masa.TweakerooToolSwitchMixin", MasaModAvailability.TWEAKEROO,
            "dev.chise.chisetweaks.mixin.masa.TweakerMoreAutoPickMixin", MasaModAvailability.TWEAKERMORE,
            "dev.chise.chisetweaks.mixin.masa.TweakerMoreMaterialRefreshMixin", MasaModAvailability.TWEAKERMORE,
            "dev.chise.chisetweaks.mixin.masa.SyncmaticaRemoveListenerMixin", MasaModAvailability.SYNCMATICA);

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String modId = MIXIN_MODS.get(mixinClassName);
        return modId != null && MasaModAvailability.isLoaded(modId);
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
