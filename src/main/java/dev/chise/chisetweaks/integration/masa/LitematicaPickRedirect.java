package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.LitematicaPickRedirectPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Chise-owned redirect decision; Litematica remains the owner of pick-block execution. */
public final class LitematicaPickRedirect {
    private LitematicaPickRedirect() {}

    public static ItemStack redirect(BlockState schematicState, ItemStack original) {
        MasaIntegrationConfig config = MasaIntegrationConfig.getInstance();
        if (!config.litematicaPickRedirect || schematicState == null || original == null) return original;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return original;

        if (containsEquivalent(client.player.getInventory(), original)) return original;

        String replacementId = LitematicaPickRedirectPolicy.replacementFor(
                MasaMinecraftIds.blockId(schematicState), config.pickRedirectMap);
        if (replacementId.isEmpty()) return original;

        var inventory = client.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (replacementId.equals(MasaMinecraftIds.blockItemId(candidate))) return candidate;
        }
        return original;
    }

    private static boolean containsEquivalent(net.minecraft.world.entity.player.Inventory inventory, ItemStack expected) {
        if (expected.isEmpty()) return false;
        String expectedId = MasaMinecraftIds.itemId(expected);
        if (expectedId.isEmpty()) return false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (expectedId.equals(MasaMinecraftIds.itemId(candidate))) return true;
        }
        return false;
    }
}
