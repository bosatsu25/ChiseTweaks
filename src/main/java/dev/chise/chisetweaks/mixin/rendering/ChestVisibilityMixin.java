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
 * Bright Chestはチェスト形状専用textureをCHEST atlasから選び、vanillaのmodel・開閉animationを維持する。
 * BlockEntity state側のfull-bright lightCoordsと組み合わせ、外部Resource Pack切替なしで白いチェストとして描画する。
 */
@Mixin(ChestRenderer.class)
public abstract class ChestVisibilityMixin {
    @Unique
    private static final SpriteId CHISETWEAKS$BRIGHT_SINGLE =
            Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("chisetweaks", "normal"));
    @Unique
    private static final SpriteId CHISETWEAKS$BRIGHT_LEFT =
            Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("chisetweaks", "normal_left"));
    @Unique
    private static final SpriteId CHISETWEAKS$BRIGHT_RIGHT =
            Sheets.CHEST_MAPPER.apply(Identifier.fromNamespaceAndPath("chisetweaks", "normal_right"));

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
        return switch (chestType) {
            case LEFT -> CHISETWEAKS$BRIGHT_LEFT;
            case RIGHT -> CHISETWEAKS$BRIGHT_RIGHT;
            default -> CHISETWEAKS$BRIGHT_SINGLE;
        };
    }
}
