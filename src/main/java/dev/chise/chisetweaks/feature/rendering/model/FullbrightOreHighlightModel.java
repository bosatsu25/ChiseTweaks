package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import dev.chise.chisetweaks.core.vision.OreHighlightRuntimePolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
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

/** Preserves the final base model and appends only Chise-owned ore overlay geometry. */
final class FullbrightOreHighlightModel extends WrapperBlockStateModel {
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private final @Nullable Target fixedTarget;
    private final @Nullable ExtraModelKey<BlockStateModel> fixedStaticKey;
    private final @Nullable ExtraModelKey<BlockStateModel> fixedAnimatedKey;
    private final boolean dynamicModded;
    private volatile @Nullable BlockStateModel staticOverlay;
    private volatile @Nullable BlockStateModel animatedOverlay;
    private volatile boolean staticOverlayResolved;
    private volatile boolean animatedOverlayResolved;

    private volatile long dynamicResolverRevision = Long.MIN_VALUE;
    private volatile @Nullable OreHighlightStyle dynamicStyle;
    private volatile @Nullable BlockStateModel dynamicStaticOverlay;
    private volatile @Nullable BlockStateModel dynamicAnimatedOverlay;
    private volatile boolean dynamicStaticOverlayResolved;
    private volatile boolean dynamicAnimatedOverlayResolved;

    FullbrightOreHighlightModel(
            BlockStateModel wrapped,
            Target target,
            ExtraModelKey<BlockStateModel> staticOverlayKey,
            ExtraModelKey<BlockStateModel> animatedOverlayKey) {
        super(wrapped);
        this.fixedTarget = target;
        this.fixedStaticKey = staticOverlayKey;
        this.fixedAnimatedKey = animatedOverlayKey;
        this.dynamicModded = false;
    }

    FullbrightOreHighlightModel(BlockStateModel wrapped) {
        super(wrapped);
        this.fixedTarget = null;
        this.fixedStaticKey = null;
        this.fixedAnimatedKey = null;
        this.dynamicModded = true;
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

        BlockStateModel overlay;
        boolean animated;
        if (dynamicModded) {
            if (!FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue()) return;
            OreHighlightStyle style = resolveDynamicStyle(state);
            if (style == null) return;
            animated = motion() == OreHighlightRuntimePolicy.Motion.ANIMATED;
            overlay = dynamicOverlayModel(style, animated);
        } else {
            if (!fixedHighlightEnabled()) return;
            animated = motion() == OreHighlightRuntimePolicy.Motion.ANIMATED;
            overlay = fixedOverlayModel(animated);
        }
        if (overlay == null) return;

        emitter.pushTransform(quad -> {
            FullbrightOverlayLighting.apply(quad);
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

    private boolean fixedHighlightEnabled() {
        if (fixedTarget == null) return false;
        return OreHighlightRuntimePolicy.shouldRender(
                FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue(),
                LocalFeatureConfig.getInstance().visualTargetMask,
                fixedTarget);
    }

    private static OreHighlightRuntimePolicy.Motion motion() {
        return OreHighlightRuntimePolicy.motion(
                LocalFeatureConfig.getInstance().oreHighlightAnimationEnabled);
    }

    private @Nullable BlockStateModel fixedOverlayModel(boolean animated) {
        if (animated ? animatedOverlayResolved : staticOverlayResolved) {
            return animated ? animatedOverlay : staticOverlay;
        }
        ExtraModelKey<BlockStateModel> key = animated ? fixedAnimatedKey : fixedStaticKey;
        BlockStateModel loaded = lookup(key);
        if (animated) {
            animatedOverlay = loaded;
            animatedOverlayResolved = true;
        } else {
            staticOverlay = loaded;
            staticOverlayResolved = true;
        }
        return loaded;
    }

    private @Nullable OreHighlightStyle resolveDynamicStyle(BlockState state) {
        long revision = OreHighlightResolver.revision();
        if (dynamicResolverRevision == revision) return dynamicStyle;
        synchronized (this) {
            if (dynamicResolverRevision == revision) return dynamicStyle;
            OreHighlightResolver.Resolved resolved = OreHighlightResolver.resolve(state);
            dynamicStyle = resolved != null && resolved.target() == null ? resolved.style() : null;
            dynamicResolverRevision = revision;
            dynamicStaticOverlay = null;
            dynamicAnimatedOverlay = null;
            dynamicStaticOverlayResolved = false;
            dynamicAnimatedOverlayResolved = false;
            return dynamicStyle;
        }
    }

    private @Nullable BlockStateModel dynamicOverlayModel(OreHighlightStyle style, boolean animated) {
        if (animated ? dynamicAnimatedOverlayResolved : dynamicStaticOverlayResolved) {
            return animated ? dynamicAnimatedOverlay : dynamicStaticOverlay;
        }
        OreHighlightOverlayCatalog.OverlayModels models = OreHighlightOverlayCatalog.forStyle(style);
        if (models == null) return null;
        BlockStateModel loaded = lookup(animated ? models.animatedKey() : models.staticKey());
        if (animated) {
            dynamicAnimatedOverlay = loaded;
            dynamicAnimatedOverlayResolved = true;
        } else {
            dynamicStaticOverlay = loaded;
            dynamicStaticOverlayResolved = true;
        }
        return loaded;
    }

    private static @Nullable BlockStateModel lookup(@Nullable ExtraModelKey<BlockStateModel> key) {
        if (key == null) return null;
        try {
            return Minecraft.getInstance().getModelManager().getModel(key);
        } catch (RuntimeException failure) {
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
        if (!dynamicModded) {
            if (!fixedHighlightEnabled()) return wrappedKey;
            return new FixedGeometryKey(wrappedKey, fixedTarget, motion());
        }

        if (!FeatureSwitches.MATERIAL_HIGHLIGHTS.getBooleanValue()) return wrappedKey;
        OreHighlightStyle style = resolveDynamicStyle(state);
        if (style == null) return wrappedKey;
        return new DynamicGeometryKey(
                wrappedKey,
                style,
                motion(),
                dynamicResolverRevision);
    }

    private record FixedGeometryKey(
            Object wrappedKey,
            @Nullable Target target,
            OreHighlightRuntimePolicy.Motion motion) {}

    private record DynamicGeometryKey(
            Object wrappedKey,
            OreHighlightStyle style,
            OreHighlightRuntimePolicy.Motion motion,
            long resolverRevision) {}
}
