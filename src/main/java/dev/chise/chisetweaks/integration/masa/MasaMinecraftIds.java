package dev.chise.chisetweaks.integration.masa;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class MasaMinecraftIds {
    private MasaMinecraftIds() {}

    static String blockId(BlockState state) {
        return state == null ? "" : blockId(state.getBlock());
    }

    static String blockId(Block block) {
        var id = block == null ? null : BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? "" : id.toString();
    }

    static String itemId(ItemStack stack) {
        var id = stack == null || stack.isEmpty()
                ? null
                : BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    static String blockItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return "";
        return blockId(item.getBlock());
    }
}
