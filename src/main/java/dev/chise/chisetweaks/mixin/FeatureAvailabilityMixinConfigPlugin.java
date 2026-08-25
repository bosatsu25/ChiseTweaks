package dev.chise.chisetweaks.mixin;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 互換性影響の大きいMixinを、対応する現行featureが利用可能な場合だけ適用する。 */
public final class FeatureAvailabilityMixinConfigPlugin implements IMixinConfigPlugin {
    private static final Map<String, List<FeatureDefinition>> MIXIN_FEATURES = Map.of(
            "dev.chise.chisetweaks.mixin.placement.AirPlacementMixin",
            List.of(FeatureDefinition.AIR_PLACEMENT),
            "dev.chise.chisetweaks.mixin.rendering.BuilderFocusBlockMixin",
            List.of(FeatureDefinition.BUILDER_FOCUS_BLOCKS),
            "dev.chise.chisetweaks.mixin.rendering.BlockEntityVisualStateMixin",
            List.of(FeatureDefinition.BUILDER_FOCUS_BLOCKS, FeatureDefinition.BRIGHT_CHEST),
            "dev.chise.chisetweaks.mixin.rendering.ChestVisibilityMixin",
            List.of(FeatureDefinition.BRIGHT_CHEST),
            "dev.chise.chisetweaks.mixin.rendering.BuilderFocusEntityMixin",
            List.of(FeatureDefinition.BUILDER_FOCUS_ENTITIES),
            "dev.chise.chisetweaks.mixin.rendering.FireVisibilityMixin",
            List.of(FeatureDefinition.FIRE_VISIBILITY));

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        List<FeatureDefinition> features = MIXIN_FEATURES.get(mixinClassName);
        if (features == null) return false;
        for (FeatureDefinition feature : features) {
            if (FeatureAvailabilityPolicy.isAvailable(feature)) return true;
        }
        return false;
    }

    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
