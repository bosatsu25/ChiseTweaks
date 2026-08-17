package dev.chise.chisetweaks.feature.rendering.model;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.util.TriState;

/** Shared lighting transform for Chise-owned baked-model visual overlays. */
final class FullbrightOverlayLighting {
    private FullbrightOverlayLighting() {}

    static void apply(MutableQuadView quad) {
        quad.emissive(true);
        quad.diffuseShade(false);
        quad.ambientOcclusion(TriState.FALSE);
    }
}
