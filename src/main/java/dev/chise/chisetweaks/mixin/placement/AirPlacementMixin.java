package dev.chise.chisetweaks.mixin.placement;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.chise.chisetweaks.feature.placement.AirPlacementTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Temporarily turns a normal MISS into a bounded air-block hit for one vanilla use action. */
@Mixin(Minecraft.class)
public abstract class AirPlacementMixin {
    @Shadow public @Nullable LocalPlayer player;
    @Shadow public @Nullable ClientLevel level;
    @Shadow public @Nullable HitResult hitResult;

    @WrapMethod(method = "startUseItem")
    private void chiseTweaks$airPlacement(Operation<Void> original) {
        HitResult previous = hitResult;
        BlockHitResult airHit = AirPlacementTarget.resolve(player, level, previous);
        if (airHit != null) hitResult = airHit;
        try {
            original.call();
        } finally {
            hitResult = previous;
        }
    }
}
