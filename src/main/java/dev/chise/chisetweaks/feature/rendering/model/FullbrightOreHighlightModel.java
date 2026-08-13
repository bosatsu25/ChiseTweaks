package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.OreHighlightRuntimePolicy;
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

import java.util.concurrent.atomic.AtomicBoolean;
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
 * Chise overlay material is submitted whether shaders are enabled or disabled. Overlay lookup and
 * emission are fail-soft: the already-emitted base model remains valid if Chise's extra layer fails.</p>
 */
final class FullbrightOreHighlightModel extends WrapperBlockStateModel {
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private final Target target;
    private final ExtraModelKey<BlockStateModel> staticOverlayKey;
    private final ExtraModelKey<BlockStateModel> animatedOverlayKey;
    private volatile @Nullable BlockStateModel staticOverlay;
    private volatile @Nullable BlockStateModel animatedOverlay;
    private volatile boolean staticOverlayResolved;
    private volatile boolean animatedOverlayResolved;

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
        // The active vanilla/resource-pack model is always emitted untouched and cannot be lost if
        // Chise's optional overlay lookup or emission later fails.
        super.emitQuads(emitter, level, pos, state, random, cullTest);

        if (!highlightEnabled()) return;
        boolean animated = motion() == OreHighlightRuntimePolicy.Motion.ANIMATED;
        BlockStateModel overlay = overlayModel(animated);
        if (overlay == null) return;

        emitter.pushTransform(quad -> {
            applyShaderInvariantHighlightLighting(quad);
            return true;
        });
        try {
            overlay.emitQuads(emitter, level, pos, state, random, cullTest);
        } catch (RuntimeException failure) {
            warnOnce(EMIT_FAILURE_LOGGED, "Ore Highlight overlay emission", failure);
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
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        return OreHighlightRuntimePolicy.shouldRender(
                FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue(),
                config.visualTargetMask,
                target);
    }

    private static OreHighlightRuntimePolicy.Motion motion() {
        return OreHighlightRuntimePolicy.motion(
                LocalFeatureConfig.getInstance().oreHighlightAnimationEnabled);
    }

    private @Nullable BlockStateModel overlayModel(boolean animated) {
        if (animated ? animatedOverlayResolved : staticOverlayResolved) {
            return animated ? animatedOverlay : staticOverlay;
        }

        BlockStateModel loaded = null;
        try {
            loaded = Minecraft.getInstance().getModelManager().getModel(
                    animated ? animatedOverlayKey : staticOverlayKey);
        } catch (RuntimeException failure) {
            warnOnce(LOOKUP_FAILURE_LOGGED, "Ore Highlight extra-model lookup", failure);
        }

        if (animated) {
            animatedOverlay = loaded;
            animatedOverlayResolved = true;
        } else {
            staticOverlay = loaded;
            staticOverlayResolved = true;
        }
        return loaded;
    }

    private static void warnOnce(AtomicBoolean gate, String operation, RuntimeException failure) {
        if (!gate.compareAndSet(false, true)) return;
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}; keeping the resource-pack base model without the Chise overlay",
                operation,
                failure.getClass().getSimpleName());
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
        OreHighlightRuntimePolicy.Motion motion = enabled
                ? motion()
                : OreHighlightRuntimePolicy.Motion.STATIC;
        return new FullbrightGeometryKey(wrappedKey, target, enabled, motion);
    }

    /** Prevents renderer cache reuse across runtime Ore Highlight state/style changes. */
    private record FullbrightGeometryKey(
            Object wrappedKey,
            Target target,
            boolean enabled,
            OreHighlightRuntimePolicy.Motion motion) {}
}
