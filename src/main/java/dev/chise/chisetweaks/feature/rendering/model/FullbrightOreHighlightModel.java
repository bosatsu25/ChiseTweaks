package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import dev.chise.chisetweaks.core.vision.OreHighlightRuntimePolicy;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

final class FullbrightOreHighlightModel extends WrapperBlockStateModel {
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private final BlockState representativeState;
    private volatile long cachedResolverRevision = Long.MIN_VALUE;
    private volatile @Nullable OreHighlightResolver.Resolved cachedResolved;

    FullbrightOreHighlightModel(BlockStateModel wrapped, BlockState representativeState) {
        super(wrapped);
        this.representativeState = representativeState;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        super.emitQuads(emitter, level, pos, state, random, cullTest);
        OreHighlightResolver.Resolved resolved = resolvedFor(state);
        if (resolved == null || resolved.style() == null || !highlightEnabled(resolved)) return;
        boolean animated = motion() == OreHighlightRuntimePolicy.Motion.ANIMATED;
        BlockStateModel overlay = overlayModel(resolved.style(), animated);
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

    private static void applyShaderInvariantHighlightLighting(MutableQuadView quad) {
        quad.emissive(true);
        quad.diffuseShade(false);
        quad.ambientOcclusion(TriState.FALSE);
    }

    private static boolean highlightEnabled(OreHighlightResolver.Resolved resolved) {
        boolean master = FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue();
        if (!master) return false;
        if (resolved.target() == null) return true;
        return OreHighlightRuntimePolicy.shouldRender(
                true,
                LocalFeatureConfig.getInstance().visualTargetMask,
                resolved.target());
    }

    private static OreHighlightRuntimePolicy.Motion motion() {
        return OreHighlightRuntimePolicy.motion(
                LocalFeatureConfig.getInstance().oreHighlightAnimationEnabled);
    }

    private @Nullable OreHighlightResolver.Resolved resolvedFor(BlockState state) {
        long revision = OreHighlightResolver.revision();
        if (cachedResolverRevision == revision) return cachedResolved;
        OreHighlightResolver.Resolved resolved = OreHighlightResolver.resolve(
                state == null ? representativeState : state);
        cachedResolved = resolved;
        cachedResolverRevision = revision;
        return resolved;
    }

    private static @Nullable BlockStateModel overlayModel(OreHighlightStyle style, boolean animated) {
        try {
            return OreHighlightOverlayCatalog.baked(style, animated);
        } catch (RuntimeException failure) {
            OreHighlightOverlayCatalog.markUnavailable(style, animated);
            warnOnce(LOOKUP_FAILURE_LOGGED, "Ore Highlight extra-model lookup", failure);
            return null;
        }
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
        OreHighlightResolver.Resolved resolved = resolvedFor(state);
        if (resolved == null || resolved.style() == null) return wrappedKey;
        boolean enabled = highlightEnabled(resolved);
        OreHighlightRuntimePolicy.Motion motion = enabled
                ? motion()
                : OreHighlightRuntimePolicy.Motion.STATIC;
        return new FullbrightGeometryKey(
                wrappedKey, resolved.target(), resolved.style(), enabled, motion);
    }

    private record FullbrightGeometryKey(
            Object wrappedKey,
            @Nullable dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target target,
            OreHighlightStyle style,
            boolean enabled,
            OreHighlightRuntimePolicy.Motion motion) {}
}
