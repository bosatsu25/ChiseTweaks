package dev.chise.chisetweaks.mixin.rendering;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.chise.chisetweaks.config.FeatureSwitches;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Bright Chestは専用textureを持たず、vanilla White ConcreteのspriteをChest modelへ再利用する。
 * BlockEntity state側のfull-bright lightCoordsと組み合わせ、resource reloadなしで白く見せる。
 */
@Mixin(ChestRenderer.class)
public abstract class ChestVisibilityMixin {
    @Unique
    private static final SpriteId CHISETWEAKS$WHITE_CHEST =
            Sheets.BLOCKS_MAPPER.apply(Identifier.fromNamespaceAndPath("minecraft", "white_concrete"));

    @WrapOperation(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;chooseSprite(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState$ChestMaterialType;Lnet/minecraft/world/level/block/state/properties/ChestType;)Lnet/minecraft/client/resources/model/sprite/SpriteId;"))
    private SpriteId chiseTweaks$chooseChestSprite(
            ChestRenderState.ChestMaterialType materialType,
            ChestType chestType,
            Operation<SpriteId> original) {
        SpriteId vanilla = original.call(materialType, chestType);
        if (!FeatureSwitches.BRIGHT_CHEST.getBooleanValue()
                || materialType != ChestRenderState.ChestMaterialType.REGULAR) {
            return vanilla;
        }
        return CHISETWEAKS$WHITE_CHEST;
    }
}
