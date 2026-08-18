package dev.chise.chisetweaks.feature.rendering.model;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/** Executes one optional fullbright overlay emission without leaking transform-stack failures. */
final class FullbrightOverlayEmission {
    private FullbrightOverlayEmission() {}

    static @Nullable Throwable emit(
            QuadEmitter emitter,
            BlockStateModel overlay,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        boolean pushed = false;
        Throwable failure = null;
        try {
            emitter.pushTransform(quad -> {
                FullbrightOverlayLighting.apply(quad);
                return true;
            });
            pushed = true;
            overlay.emitQuads(emitter, level, pos, state, random, cullTest);
        } catch (RuntimeException | LinkageError emissionFailure) {
            failure = emissionFailure;
        } finally {
            if (pushed) {
                try {
                    emitter.popTransform();
                } catch (RuntimeException | LinkageError cleanupFailure) {
                    if (failure == null) failure = cleanupFailure;
                }
            }
        }
        return failure;
    }
}
