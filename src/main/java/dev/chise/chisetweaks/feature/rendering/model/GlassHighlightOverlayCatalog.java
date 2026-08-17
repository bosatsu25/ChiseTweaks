package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.core.vision.GlassHighlightTargetPolicy;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

/** Preloaded Chise-owned overlay models used by Glass Highlight. */
final class GlassHighlightOverlayCatalog {
    static final int BLOCK_CYAN_ARGB = 0xFF5EEBFF;
    static final int PANE_AMBER_ARGB = 0xFFFFD166;

    static final Identifier BLOCK_MODEL = Identifier.fromNamespaceAndPath(
            ChiseTweaksMetadata.MOD_ID,
            "block/visual/glass/block_overlay");
    static final Identifier PANE_MODEL = Identifier.fromNamespaceAndPath(
            ChiseTweaksMetadata.MOD_ID,
            "block/visual/glass/pane_overlay");

    static final ExtraModelKey<BlockStateModel> BLOCK_KEY =
            ExtraModelKey.create(BLOCK_MODEL::toString);
    static final ExtraModelKey<BlockStateModel> PANE_KEY =
            ExtraModelKey.create(PANE_MODEL::toString);

    private GlassHighlightOverlayCatalog() {}

    static ExtraModelKey<BlockStateModel> keyFor(GlassHighlightTargetPolicy.Shape shape) {
        return switch (shape) {
            case BLOCK -> BLOCK_KEY;
            case PANE -> PANE_KEY;
            case NONE -> throw new IllegalArgumentException("No Glass Highlight overlay for NONE");
        };
    }
}
