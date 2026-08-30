package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.MasaIdListPolicy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class MasaGuardRuntime {
    private MasaGuardRuntime() {}

    public static boolean allowTweakerMoreAutoPick(ItemStack heldStack) {
        MasaIntegrationConfig config = MasaIntegrationConfig.getInstance();
        if (!config.tweakermoreAutoPickGuard) return true;
        return MasaIdListPolicy.allows(
                MasaMinecraftIds.itemId(heldStack),
                config.tweakermoreAutoPickListMode,
                config.tweakermoreAutoPickWhitelist,
                config.tweakermoreAutoPickBlacklist);
    }

    public static boolean allowTweakerooToolSwitch(BlockState targetState) {
        MasaIntegrationConfig config = MasaIntegrationConfig.getInstance();
        if (!config.tweakerooToolSwitchGuard) return true;
        return MasaIdListPolicy.allows(
                MasaMinecraftIds.blockId(targetState),
                config.tweakerooToolSwitchListMode,
                config.tweakerooToolSwitchWhitelist,
                config.tweakerooToolSwitchBlacklist);
    }
}
