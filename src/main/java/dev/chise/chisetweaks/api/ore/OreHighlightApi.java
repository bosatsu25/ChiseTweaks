package dev.chise.chisetweaks.api.ore;

import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Public registration surface for mods that want Chise Ore Highlights without a hard integration.
 *
 * <p>Registrations should be made during client initialization so the target model is known before
 * Minecraft bakes block models. Only registry identifiers/tags and a Chise-owned style are shared;
 * no third-party texture or model asset is copied.</p>
 */
public final class OreHighlightApi {
    private OreHighlightApi() {}

    public static void registerBlock(Identifier blockId, OreHighlightStyle style) {
        if (blockId == null) throw new IllegalArgumentException("blockId is required");
        OreHighlightExternalRegistry.registerApiBlock(blockId.toString(), style);
    }

    public static void registerBlock(Block block, OreHighlightStyle style) {
        if (block == null) throw new IllegalArgumentException("block is required");
        registerBlock(BuiltInRegistries.BLOCK.getKey(block), style);
    }

    public static void registerTag(TagKey<Block> tag, OreHighlightStyle style) {
        OreHighlightExternalRegistry.registerApiTag(tag, style);
    }
}
