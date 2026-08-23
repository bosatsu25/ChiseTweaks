package dev.chise.chisetweaks.mixin;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies compatibility-sensitive mixins only when the owning released feature is available. */
public final class FeatureAvailabilityMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Map<String, FeatureDefinition> MIXIN_FEATURES = Map.of(
            "dev.chise.chisetweaks.mixin.rendering.BuilderFocusBlockMixin",
            FeatureDefinition.BUILDER_FOCUS_BLOCKS,
            "dev.chise.chisetweaks.mixin.rendering.BuilderFocusEntityMixin",
            FeatureDefinition.BUILDER_FOCUS_ENTITIES,
            "dev.chise.chisetweaks.mixin.rendering.FireVisibilityMixin",
            FeatureDefinition.FIRE_VISIBILITY);

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        FeatureDefinition feature = MIXIN_FEATURES.get(mixinClassName);
        return feature != null && FeatureAvailabilityPolicy.isAvailable(feature);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}
}
