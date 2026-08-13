package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Preserves the final baked Minecraft/resource-pack model and appends Chise-owned overlay geometry.
 *
 * <p>The wrapped base model is emitted first with no Chise transform at all. Only while the separate
 * Chise extra model is emitted do we apply emissive, diffuse-off and AO-off material semantics. This
 * explicit ownership boundary prevents custom resource-pack geometry outside the normal block cube
 * from being mistaken for a Chise overlay.
 *
 * <p>Feature state, per-family targets and reduced-motion preference are evaluated at render time,
 * so settings never require a model/resource reload. Shader state is deliberately ignored: the same
 * Chise overlay material is submitted whether shaders are enabled or disabled.</p>
 */
final class FullbrightOreHighlightModel extends WrapperBlockStateModel {
    private final Target target;
    private final ExtraModelKey<BlockStateModel> staticOverlayKey;
    private final ExtraModelKey<BlockStateModel> animatedOverlayKey;
    private volatile @Nullable BlockStateModel staticOverlay;
    private volatile @Nullable BlockStateModel animatedOverlay;

    FullbrightOreHighlightModel(
            BlockStateModel wrapped,
            Target target,
            ExtraModelKey<BlockStateModel> staticOverlayKey,
            ExtraModelKey<BlockStateModel> animatedOverlayKey) {
        super(wrapped);
        this.target = target;
        this.staticOverlayKey = staticOverlayKey;
        this.animatedOverlayKey = animatedOverlayKey;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        // The active vanilla/resource-pack model is always emitted untouched.
        super.emitQuads(emitter, level, pos, state, random, cullTest);

        if (!highlightEnabled()) return;
        boolean animated = LocalFeatureConfig.getInstance().oreHighlightAnimationEnabled;
        BlockStateModel overlay = overlayModel(animated);
        if (overlay == null) return;

        emitter.pushTransform(quad -> {
            applyShaderInvariantHighlightLighting(quad);
            return true;
        });
        try {
            overlay.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    /** Applies full-bright semantics only to quads emitted by the Chise-owned extra model. */
    private static void applyShaderInvariantHighlightLighting(MutableQuadView quad) {
        quad.emissive(true);
        quad.diffuseShade(false);
        quad.ambientOcclusion(TriState.FALSE);
    }

    private boolean highlightEnabled() {
        if (!FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue()) return false;
        return VisualTargetSelectionPolicy.isEnabled(
                LocalFeatureConfig.getInstance().visualTargetMask,
                target);
    }

    private @Nullable BlockStateModel overlayModel(boolean animated) {
        BlockStateModel cached = animated ? animatedOverlay : staticOverlay;
        if (cached != null) return cached;

        BlockStateModel loaded = Minecraft.getInstance().getModelManager().getModel(
                animated ? animatedOverlayKey : staticOverlayKey);
        if (loaded == null) return null;
        if (animated) animatedOverlay = loaded;
        else staticOverlay = loaded;
        return loaded;
    }

    @Override
    @Nullable
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        Object wrappedKey = wrapped.createGeometryKey(level, pos, state, random);
        if (wrappedKey == null) return null;
        boolean enabled = highlightEnabled();
        boolean animated = enabled && LocalFeatureConfig.getInstance().oreHighlightAnimationEnabled;
        return new FullbrightGeometryKey(wrappedKey, target, enabled, animated);
    }

    /** Prevents renderer cache reuse across runtime Ore Highlight state/style changes. */
    private record FullbrightGeometryKey(
            Object wrappedKey,
            Target target,
            boolean enabled,
            boolean animated) {}
}
