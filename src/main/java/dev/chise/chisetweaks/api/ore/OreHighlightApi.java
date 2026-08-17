package dev.chise.chisetweaks.api.ore;

import dev.chise.chisetweaks.core.vision.OreHighlightExternalRegistry;
import dev.chise.chisetweaks.feature.rendering.model.OreHighlightModelReload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Public registration surface for mods that want Chise Ore Highlights without a hard integration.
 *
 * <p>Registrations performed during client initialization are consumed by the first model bake.
 * Later registrations are rare compatibility changes, so Chise coalesces them into a model/resource
 * reload instead of keeping every third-party block on a runtime resolver path.</p>
 */
public final class OreHighlightApi {
    private OreHighlightApi() {}

    public static void registerBlock(Identifier blockId, OreHighlightStyle style) {
        if (blockId == null) throw new IllegalArgumentException("blockId is required");
        OreHighlightExternalRegistry.registerApiBlock(blockId.toString(), style);
        OreHighlightModelReload.request();
    }

    public static void registerBlock(Block block, OreHighlightStyle style) {
        if (block == null) throw new IllegalArgumentException("block is required");
        registerBlock(BuiltInRegistries.BLOCK.getKey(block), style);
    }

    public static void registerTag(TagKey<Block> tag, OreHighlightStyle style) {
        OreHighlightExternalRegistry.registerApiTag(tag, style);
        OreHighlightModelReload.request();
    }
}
