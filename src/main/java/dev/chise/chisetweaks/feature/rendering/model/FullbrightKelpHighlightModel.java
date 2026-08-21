package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksClient;
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
 * Preserves vanilla/resource-pack kelp geometry and appends the Chise party overlay as emissive
 * crossed planes in the same baked-model rendering pipeline used by Ore Highlights.
 */
final class FullbrightKelpHighlightModel extends WrapperBlockStateModel {
    private static final int MAX_LOOKUP_ATTEMPTS = 3;
    private static final AtomicBoolean LOOKUP_FAILURE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean EMIT_FAILURE_LOGGED = new AtomicBoolean();

    private volatile @Nullable BlockStateModel overlay;
    private volatile int overlayLookupFailures;
    private volatile boolean emissionQuarantined;

    FullbrightKelpHighlightModel(BlockStateModel wrapped) {
        super(wrapped);
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
        if (!VisualRenderState.current().kelpEnabled() || emissionQuarantined) return;

        BlockStateModel partyOverlay = overlayModel();
        if (partyOverlay == null) return;

        Throwable failure = FullbrightOverlayEmission.emit(
                emitter, partyOverlay, level, pos, state, random, cullTest);
        if (failure != null) {
            emissionQuarantined = true;
            warnOnce(EMIT_FAILURE_LOGGED, "Kelp Highlight overlay emission", failure);
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

            BlockStateModel loaded = lookup();
            if (loaded != null) {
                overlay = loaded;
                return loaded;
            }
            overlayLookupFailures++;
            return null;
        }
    }

    private static @Nullable BlockStateModel lookup() {
        try {
            return Minecraft.getInstance().getModelManager().getModel(KelpHighlightOverlayCatalog.KEY);
        } catch (RuntimeException | LinkageError failure) {
            warnOnce(LOOKUP_FAILURE_LOGGED, "Kelp Highlight extra-model lookup", failure);
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
                || !VisualRenderState.current().kelpEnabled()
                || emissionQuarantined) {
            return wrappedKey;
        }
        return new KelpGeometryKey(wrappedKey);
    }

    private record KelpGeometryKey(Object wrappedKey) {}

    private static void warnOnce(AtomicBoolean gate, String operation, Throwable failure) {
        if (!gate.compareAndSet(false, true)) return;
        ChiseTweaksClient.LOGGER.warn(
                "{} failed after {}; keeping the resource-pack base kelp model without the Chise overlay",
                operation,
                failure.getClass().getSimpleName());
    }
}
