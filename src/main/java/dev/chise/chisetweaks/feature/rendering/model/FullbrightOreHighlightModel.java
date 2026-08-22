package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
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

final class FullbrightOreHighlightModel extends WrapperBlockStateModel {
    private static final int MAX_LOOKUP_ATTEMPTS = 3;
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private final @Nullable Target target;
    private final ExtraModelKey<BlockStateModel> staticKey;
    private final ExtraModelKey<BlockStateModel> animatedKey;

    private volatile @Nullable BlockStateModel staticOverlay;
    private volatile @Nullable BlockStateModel animatedOverlay;
    private volatile int staticLookupFailures;
    private volatile int animatedLookupFailures;
    private volatile boolean staticEmissionQuarantined;
    private volatile boolean animatedEmissionQuarantined;

    FullbrightOreHighlightModel(
            BlockStateModel wrapped,
            @Nullable Target target,
            ExtraModelKey<BlockStateModel> staticKey,
            ExtraModelKey<BlockStateModel> animatedKey) {
        super(wrapped);
        this.target = target;
        this.staticKey = staticKey;
        this.animatedKey = animatedKey;
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
        if (!renderState.shouldRenderOre(target)) return;

        boolean animated = renderState.oreMotion() == OreHighlightRuntimePolicy.Motion.ANIMATED;
        if (emissionQuarantined(animated)) return;
        BlockStateModel overlay = overlayModel(animated);
        if (overlay == null) return;

        Throwable failure = FullbrightOverlayEmission.emit(
                emitter, overlay, level, pos, state, random, cullTest);
        if (failure != null) {
            quarantineEmission(animated);
            warnOnce(EMIT_FAILURE_LOGGED, "Ore Highlight overlay emission", failure);
        }
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

    private static @Nullable BlockStateModel lookup(ExtraModelKey<BlockStateModel> key) {
        try {
            return Minecraft.getInstance().getModelManager().getModel(key);
        } catch (RuntimeException | LinkageError failure) {
            warnOnce(LOOKUP_FAILURE_LOGGED, "Ore Highlight extra-model lookup", failure);
            return null;
        }
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
        if (!renderState.shouldRenderOre(target)) return wrappedKey;
        OreHighlightRuntimePolicy.Motion currentMotion = renderState.oreMotion();
        boolean animated = currentMotion == OreHighlightRuntimePolicy.Motion.ANIMATED;
        if (emissionQuarantined(animated)) return wrappedKey;
        return new GeometryKey(wrappedKey, target, currentMotion);
    }

    private record GeometryKey(
            Object wrappedKey,
            @Nullable Target target,
            OreHighlightRuntimePolicy.Motion motion) {}

    private static void warnOnce(AtomicBoolean gate, String operation, Throwable failure) {
        if (!gate.compareAndSet(false, true)) return;
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}; keeping the resource-pack base model without the Chise overlay",
                operation,
                failure.getClass().getSimpleName());
    }
}
