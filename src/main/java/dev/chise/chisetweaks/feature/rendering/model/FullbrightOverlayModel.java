package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.vision.GlassHighlightTargetPolicy;
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

/**
 * Ore / Glass / Kelp が共有する fullbright overlay lifecycle。
 * feature 固有差分は対象判定・overlay key・geometry token だけに限定する。
 */
final class FullbrightOverlayModel extends WrapperBlockStateModel {
    private static final int KIND_ORE = 0;
    private static final int KIND_GLASS = 1;
    private static final int KIND_KELP = 2;
    private static final int MAX_LOOKUP_ATTEMPTS = 3;

    private static final AtomicBoolean ORE_LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean GLASS_LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean KELP_LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean ORE_EMIT_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean GLASS_EMIT_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean KELP_EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private final int kind;
    private final @Nullable Target target;
    private final GlassHighlightTargetPolicy.Shape glassShape;
    private final ExtraModelKey<BlockStateModel> staticKey;
    private final @Nullable ExtraModelKey<BlockStateModel> animatedKey;

    private volatile @Nullable BlockStateModel staticOverlay;
    private volatile @Nullable BlockStateModel animatedOverlay;
    private volatile int staticLookupFailures;
    private volatile int animatedLookupFailures;
    private volatile boolean staticEmissionQuarantined;
    private volatile boolean animatedEmissionQuarantined;

    private FullbrightOverlayModel(
            BlockStateModel wrapped,
            int kind,
            @Nullable Target target,
            GlassHighlightTargetPolicy.Shape glassShape,
            ExtraModelKey<BlockStateModel> staticKey,
            @Nullable ExtraModelKey<BlockStateModel> animatedKey) {
        super(wrapped);
        this.kind = kind;
        this.target = target;
        this.glassShape = glassShape;
        this.staticKey = staticKey;
        this.animatedKey = animatedKey;
    }

    static FullbrightOverlayModel ore(
            BlockStateModel wrapped,
            @Nullable Target target,
            ExtraModelKey<BlockStateModel> staticKey,
            ExtraModelKey<BlockStateModel> animatedKey) {
        return new FullbrightOverlayModel(
                wrapped, KIND_ORE, target, null, staticKey, animatedKey);
    }

    static FullbrightOverlayModel glass(
            BlockStateModel wrapped,
            GlassHighlightTargetPolicy.Shape shape,
            ExtraModelKey<BlockStateModel> overlayKey) {
        if (shape == null || shape == GlassHighlightTargetPolicy.Shape.NONE) {
            throw new IllegalArgumentException("Glass Highlight wrapper requires a glass shape");
        }
        return new FullbrightOverlayModel(
                wrapped, KIND_GLASS, null, shape, overlayKey, null);
    }

    static FullbrightOverlayModel kelp(BlockStateModel wrapped) {
        return new FullbrightOverlayModel(
                wrapped, KIND_KELP, null, null, KelpHighlightOverlayCatalog.KEY, null);
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
        VisualRenderState.Snapshot renderState = VisualRenderState.current();
        if (!shouldRender(renderState)) return;

        boolean animated = usesAnimatedOverlay(renderState);
        if (emissionQuarantined(animated)) return;
        BlockStateModel overlay = overlayModel(animated);
        if (overlay == null) return;

        Throwable failure = FullbrightOverlayEmission.emit(
                emitter, overlay, level, pos, state, random, cullTest);
        if (failure != null) {
            quarantineEmission(animated);
            warnOnce(emissionWarningGate(), featureName() + " overlay emission", failure);
        }
    }

    private boolean shouldRender(VisualRenderState.Snapshot renderState) {
        if (kind == KIND_ORE) return renderState.shouldRenderOre(target);
        if (kind == KIND_GLASS) return renderState.glassEnabled();
        return renderState.kelpEnabled();
    }

    private boolean usesAnimatedOverlay(VisualRenderState.Snapshot renderState) {
        return kind == KIND_ORE
                && renderState.oreMotion() == OreHighlightRuntimePolicy.Motion.ANIMATED;
    }

    private @Nullable BlockStateModel overlayModel(boolean animated) {
        BlockStateModel cached = animated ? animatedOverlay : staticOverlay;
        if (cached != null) return cached;
        if (lookupFailures(animated) >= MAX_LOOKUP_ATTEMPTS) return null;

        synchronized (this) {
            cached = animated ? animatedOverlay : staticOverlay;
            if (cached != null) return cached;
            if (lookupFailures(animated) >= MAX_LOOKUP_ATTEMPTS) return null;

            ExtraModelKey<BlockStateModel> key = animated ? animatedKey : staticKey;
            if (key == null) return null;
            BlockStateModel loaded = lookup(key);
            if (loaded != null) {
                if (animated) animatedOverlay = loaded;
                else staticOverlay = loaded;
                return loaded;
            }
            recordLookupFailure(animated);
            return null;
        }
    }

    private @Nullable BlockStateModel lookup(ExtraModelKey<BlockStateModel> key) {
        try {
            return Minecraft.getInstance().getModelManager().getModel(key);
        } catch (RuntimeException | LinkageError failure) {
            warnOnce(lookupWarningGate(), featureName() + " extra-model lookup", failure);
            return null;
        }
    }

    private int lookupFailures(boolean animated) {
        return animated ? animatedLookupFailures : staticLookupFailures;
    }

    private void recordLookupFailure(boolean animated) {
        if (animated) animatedLookupFailures++;
        else staticLookupFailures++;
    }

    private boolean emissionQuarantined(boolean animated) {
        return animated ? animatedEmissionQuarantined : staticEmissionQuarantined;
    }

    private void quarantineEmission(boolean animated) {
        if (animated) animatedEmissionQuarantined = true;
        else staticEmissionQuarantined = true;
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

        VisualRenderState.Snapshot renderState = VisualRenderState.current();
        if (!shouldRender(renderState)) return wrappedKey;
        boolean animated = usesAnimatedOverlay(renderState);
        if (emissionQuarantined(animated)) return wrappedKey;

        Object token = kind == KIND_ORE ? target : glassShape;
        OreHighlightRuntimePolicy.Motion motion = kind == KIND_ORE ? renderState.oreMotion() : null;
        return new GeometryKey(wrappedKey, kind, token, motion);
    }

    private String featureName() {
        if (kind == KIND_ORE) return "Ore Highlight";
        if (kind == KIND_GLASS) return "Glass Highlight";
        return "Kelp Highlight";
    }

    private AtomicBoolean lookupWarningGate() {
        if (kind == KIND_ORE) return ORE_LOOKUP_FAILURE_LOGGED;
        if (kind == KIND_GLASS) return GLASS_LOOKUP_FAILURE_LOGGED;
        return KELP_LOOKUP_FAILURE_LOGGED;
    }

    private AtomicBoolean emissionWarningGate() {
        if (kind == KIND_ORE) return ORE_EMIT_FAILURE_LOGGED;
        if (kind == KIND_GLASS) return GLASS_EMIT_FAILURE_LOGGED;
        return KELP_EMIT_FAILURE_LOGGED;
    }

    private record GeometryKey(
            Object wrappedKey,
            int kind,
            @Nullable Object token,
            OreHighlightRuntimePolicy.Motion motion) {}

    private static void warnOnce(AtomicBoolean gate, String operation, Throwable failure) {
        if (!gate.compareAndSet(false, true)) return;
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}; keeping the resource-pack base model without the Chise overlay",
                operation,
                failure.getClass().getSimpleName());
    }
}
