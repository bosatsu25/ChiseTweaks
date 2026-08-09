package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.core.vision.OreHighlightLightingPolicy;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Applies full-bright lighting only to Chise's slightly expanded ore-highlight overlay quads.
 *
 * <p>The wrapped model still owns the vanilla/resource-pack base geometry. Quads whose vertices
 * remain inside the normal block cube are left untouched and therefore keep ordinary world
 * lighting. Expanded Chise overlay quads are marked emissive and skip diffuse/AO darkening, so
 * they remain readable at Minecraft's maximum visual light level without changing world light,
 * block light emission, packets, or server state.</p>
 */
final class FullbrightOreHighlightModel extends WrapperBlockStateModel {
    FullbrightOreHighlightModel(BlockStateModel wrapped) {
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
        emitter.pushTransform(quad -> {
            if (isHighlightOverlay(quad)) {
                quad.emissive(true);
                quad.diffuseShade(false);
                quad.ambientOcclusion(TriState.FALSE);
            }
            return true;
        });
        try {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    private static boolean isHighlightOverlay(QuadView quad) {
        for (int vertex = 0; vertex < 4; vertex++) {
            if (OreHighlightLightingPolicy.isOverlayVertex(
                    quad.x(vertex), quad.y(vertex), quad.z(vertex))) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        Object wrappedKey = wrapped.createGeometryKey(level, pos, state, random);
        return wrappedKey == null ? null : new FullbrightGeometryKey(wrappedKey);
    }

    /** Distinguishes transformed Chise geometry from the wrapped model in renderer caches. */
    private record FullbrightGeometryKey(Object wrappedKey) {}
}
