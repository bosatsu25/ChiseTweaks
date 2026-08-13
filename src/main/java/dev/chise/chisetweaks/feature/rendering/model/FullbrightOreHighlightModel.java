package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.core.vision.OreHighlightLightingPolicy;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
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
 * Applies shader-invariant full-bright lighting to Chise's slightly expanded ore-highlight overlay quads.
 *
 * <p>The wrapped model still owns the vanilla/resource-pack base geometry. Quads whose vertices
 * remain inside the normal block cube are left untouched and therefore keep ordinary world
 * lighting. Expanded Chise overlay quads are always marked emissive and skip diffuse/AO darkening.
 * Chise deliberately does not branch on Iris, Sodium, or shader-pack state here: when Ore Highlights
 * is enabled, the exact same color/pattern/animation overlay is submitted to the active renderer
 * whether shaders are disabled or enabled. Shader packs may still post-process the final pixels,
 * but they do not change Chise's feature state or visual-language selection.</p>
 *
 * <p>No world light, block emission, packets, server state, shader-pack files, or resource-pack files
 * are modified.</p>
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
                applyShaderInvariantHighlightLighting(quad);
            }
            return true;
        });
        try {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    /**
     * The highlight material is intentionally identical for standard and shader-backed renderers.
     * Fabric's emissive flag is preferred over a hard-coded lightmap because advanced renderers may
     * use non-standard lighting pipelines while still honoring emissive material semantics.
     */
    private static void applyShaderInvariantHighlightLighting(MutableQuadView quad) {
        quad.emissive(true);
        quad.diffuseShade(false);
        quad.ambientOcclusion(TriState.FALSE);
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
