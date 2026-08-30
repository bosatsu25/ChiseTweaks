package dev.chise.chisetweaks.mixin.masa;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.chise.chisetweaks.integration.masa.LitematicaPickRedirect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.materials.MaterialCache", remap = false)
public abstract class LitematicaMaterialCacheMixin {
    @ModifyReturnValue(
            method = "getRequiredBuildItemForState",
            at = @At("RETURN"),
            require = 0)
    private ItemStack chiseTweaks$redirectPick(
            ItemStack original,
            @Local(argsOnly = true, ordinal = 0) BlockState schematicState) {
        return LitematicaPickRedirect.redirect(schematicState, original);
    }
}
