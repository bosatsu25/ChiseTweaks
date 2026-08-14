package dev.chise.chisetweaks.api.ore;

import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import dev.chise.chisetweaks.feature.rendering.model.OreHighlightRenderInvalidation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Public registration surface for mods that want Chise Ore Highlights without a hard integration.
 *
 * <p>Registrations may happen during client initialization or later in the client session. Chise
 * invalidates only chunk geometry after a registration change; it never requires a resource-pack
 * reload and never copies third-party texture/model assets.</p>
 */
public final class OreHighlightApi {
    private OreHighlightApi() {}

    public static void registerBlock(Identifier blockId, OreHighlightStyle style) {
        if (blockId == null) throw new IllegalArgumentException("blockId is required");
        OreHighlightExternalRegistry.registerApiBlock(blockId.toString(), style);
        OreHighlightRenderInvalidation.request();
    }

    public static void registerBlock(Block block, OreHighlightStyle style) {
        if (block == null) throw new IllegalArgumentException("block is required");
        registerBlock(BuiltInRegistries.BLOCK.getKey(block), style);
    }

    public static void registerTag(TagKey<Block> tag, OreHighlightStyle style) {
        OreHighlightExternalRegistry.registerApiTag(tag, style);
        OreHighlightRenderInvalidation.request();
    }
}
