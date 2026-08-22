package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

 
final class KelpHighlightOverlayCatalog {
    static final int MAGENTA_ARGB = 0xFFFF4FD8;
    static final int ORANGE_ARGB = 0xFFFF8A00;

    static final Identifier MODEL = Identifier.fromNamespaceAndPath(
            ChiseTweaksMetadata.MOD_ID,
            "block/visual/kelp/party_overlay");
    static final ExtraModelKey<BlockStateModel> KEY = ExtraModelKey.create(MODEL::toString);

    private KelpHighlightOverlayCatalog() {}
}
