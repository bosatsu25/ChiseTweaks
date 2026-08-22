package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.vision.GlassHighlightTargetPolicy;
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

final class FullbrightGlassHighlightModel extends WrapperBlockStateModel {
    private static final int MAX_LOOKUP_ATTEMPTS = 3;
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private final GlassHighlightTargetPolicy.Shape shape;
    private final ExtraModelKey<BlockStateModel> overlayKey;

    private volatile @Nullable BlockStateModel overlay;
    private volatile int overlayLookupFailures;
    private volatile boolean emissionQuarantined;

    FullbrightGlassHighlightModel(
            BlockStateModel wrapped,
            GlassHighlightTargetPolicy.Shape shape,
            ExtraModelKey<BlockStateModel> overlayKey) {
        super(wrapped);
        if (shape == GlassHighlightTargetPolicy.Shape.NONE) {
            throw new IllegalArgumentException("Glass Highlight wrapper requires a glass shape");
        }
        this.shape = shape;
        this.overlayKey = overlayKey;
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
        if (!VisualRenderState.current().glassEnabled() || emissionQuarantined) return;

        BlockStateModel highlightOverlay = overlayModel();
        if (highlightOverlay == null) return;

        Throwable failure = FullbrightOverlayEmission.emit(
                emitter, highlightOverlay, level, pos, state, random, cullTest);
        if (failure != null) {
            emissionQuarantined = true;
            warnOnce(EMIT_FAILURE_LOGGED, "Glass Highlight overlay emission", failure);
        }
    }

    private @Nullable BlockStateModel overlayModel() {
        BlockStateModel cached = overlay;
        if (cached != null) return cached;
        if (overlayLookupFailures >= MAX_LOOKUP_ATTEMPTS) return null;

        synchronized (this) {
            cached = overlay;
            if (cached != null) return cached;
            if (overlayLookupFailures >= MAX_LOOKUP_ATTEMPTS) return null;

            BlockStateModel loaded = lookup(overlayKey);
            if (loaded != null) {
                overlay = loaded;
                return loaded;
            }
            overlayLookupFailures++;
            return null;
        }
    }

    private static @Nullable BlockStateModel lookup(ExtraModelKey<BlockStateModel> key) {
        try {
            return Minecraft.getInstance().getModelManager().getModel(key);
        } catch (RuntimeException | LinkageError failure) {
            warnOnce(LOOKUP_FAILURE_LOGGED, "Glass Highlight extra-model lookup", failure);
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
        if (wrappedKey == null
                || !VisualRenderState.current().glassEnabled()
                || emissionQuarantined) {
            return wrappedKey;
        }
        return new GlassGeometryKey(wrappedKey, shape);
    }

    private record GlassGeometryKey(Object wrappedKey, GlassHighlightTargetPolicy.Shape shape) {}

    private static void warnOnce(AtomicBoolean gate, String operation, Throwable failure) {
        if (!gate.compareAndSet(false, true)) return;
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}; keeping the resource-pack base glass model without the Chise overlay",
                operation,
                failure.getClass().getSimpleName());
    }
}
