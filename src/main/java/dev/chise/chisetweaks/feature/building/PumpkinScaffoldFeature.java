package dev.chise.chisetweaks.feature.building;

import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.PumpkinScaffoldPolicy;
import dev.chise.chisetweaks.feature.Feature;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One-click pumpkin placement aid.
 *
 * <p>This feature does not replace Minecraft's use-item pipeline. It only opens
 * a short-lived input session that selects a hotbar pumpkin and, for an air
 * target, substitutes one bounded block hit. Vanilla then performs the actual
 * use operation. The mixin closes the session when vanilla returns.</p>
 */
public final class PumpkinScaffoldFeature implements Feature {
    @Override
    public String getId() {
        return FeatureDefinition.PUMPKIN_SCAFFOLD.id();
    }

    @Override
    public String getName() {
        return FeatureDefinition.PUMPKIN_SCAFFOLD.englishName();
    }

    @Override
    public boolean isEnabled() {
        return FeatureSwitches.PUMPKIN_SCAFFOLD.getBooleanValue();
    }

    @Override
    public void setEnabled(boolean enabled) {
        FeatureSwitches.PUMPKIN_SCAFFOLD.setBooleanValue(enabled);
    }

    /**
     * Applies the minimal temporary state needed for one vanilla right-click.
     *
     * @return a session to restore after vanilla processing, or {@code null}
     * when Pumpkin Scaffold intentionally leaves the click untouched.
     */
    public UseSession beginUse(Minecraft client) {
        if (!canHandle(client)) return null;

        Inventory inventory = client.player.getInventory();
        int pumpkinSlot = findPumpkinSlot(inventory);
        if (pumpkinSlot < 0) return null;

        HitResult originalHit = client.hitResult;
        BlockHitResult effectiveHit = resolvePlacementHit(client, originalHit);
        if (effectiveHit == null) return null;

        int originalSlot = inventory.getSelectedSlot();
        inventory.setSelectedSlot(pumpkinSlot);
        if (originalHit.getType() == HitResult.Type.MISS) {
            client.hitResult = effectiveHit;
        }
        return new UseSession(originalSlot, originalHit);
    }

    /** Restores only the client state changed by {@link #beginUse(Minecraft)}. */
    public void endUse(Minecraft client, UseSession session) {
        if (session != null) session.restore(client);
    }

    private boolean canHandle(Minecraft client) {
        boolean playerPresent = client.player != null;
        return PumpkinScaffoldPolicy.canHandleClick(
                isEnabled(),
                playerPresent,
                client.level != null,
                client.gameMode != null,
                client.screen != null,
                playerPresent && client.player.isSpectator(),
                client.hitResult != null);
    }

    private static int findPumpkinSlot(Inventory inventory) {
        int selectedSlot = inventory.getSelectedSlot();
        if (inventory.getItem(selectedSlot).is(Items.PUMPKIN)) return selectedSlot;

        for (int slot = 0; slot < PumpkinScaffoldPolicy.HOTBAR_SIZE; slot++) {
            if (inventory.getItem(slot).is(Items.PUMPKIN)) return slot;
        }
        return -1;
    }

    private static BlockHitResult resolvePlacementHit(Minecraft client, HitResult hit) {
        return switch (hit.getType()) {
            case BLOCK -> (BlockHitResult) hit;
            case MISS -> createAirPlacementHit(client);
            case ENTITY -> null;
        };
    }

    private static BlockHitResult createAirPlacementHit(Minecraft client) {
        int range = PumpkinScaffoldPolicy.clampPlacementRange(
                LocalFeatureConfig.getInstance().pumpkinScaffoldPlacementRange);
        Vec3 eye = client.player.getEyePosition();
        Vec3 look = client.player.getLookAngle();
        Vec3 targetPoint = eye.add(look.scale(range));
        BlockPos targetPos = BlockPos.containing(targetPoint.x, targetPoint.y, targetPoint.z);

        boolean chunkLoaded = client.level.hasChunkAt(targetPos);
        boolean targetIsAir = chunkLoaded && client.level.getBlockState(targetPos).isAir();
        if (!PumpkinScaffoldPolicy.canAttemptAirPlacement(chunkLoaded, targetIsAir)) return null;

        return new BlockHitResult(
                Vec3.atCenterOf(targetPos),
                oppositeDominantDirection(look),
                targetPos,
                false);
    }

    private static Direction oppositeDominantDirection(Vec3 look) {
        double x = Math.abs(look.x);
        double y = Math.abs(look.y);
        double z = Math.abs(look.z);
        if (y >= x && y >= z) return look.y >= 0.0 ? Direction.DOWN : Direction.UP;
        if (x >= z) return look.x >= 0.0 ? Direction.WEST : Direction.EAST;
        return look.z >= 0.0 ? Direction.NORTH : Direction.SOUTH;
    }

    /** State owned by exactly one invocation of Minecraft's use-item method. */
    public record UseSession(int originalSlot, HitResult originalHit) {
        public void restore(Minecraft client) {
            if (client == null) return;
            client.hitResult = originalHit;
            if (client.player != null) {
                client.player.getInventory().setSelectedSlot(originalSlot);
            }
        }
    }
}
