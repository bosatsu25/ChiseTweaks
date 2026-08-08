// SPDX-License-Identifier: MIT
package dev.chise.chisetweaks.mixin.sodium;

import dev.chise.chisetweaks.core.policy.ModVersionPolicy;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Fail-closed Sodium API gate for the reviewed release. */
public final class SodiumMixinPlugin implements IMixinConfigPlugin {
    private static final String SUPPORTED_VERSION = "0.9.1";
    private final FabricLoader loader = FabricLoader.getInstance();

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            return loader.getModContainer("sodium")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .filter(version -> ModVersionPolicy.matchesPinnedRelease(version, SUPPORTED_VERSION))
                    .isPresent();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
