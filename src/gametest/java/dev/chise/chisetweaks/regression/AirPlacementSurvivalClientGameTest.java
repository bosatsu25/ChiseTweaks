package dev.chise.chisetweaks.regression;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.feature.placement.AirPlacementTarget;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

/** Survivalでもvanilla useItemOn経路で1個だけ消費して空中設置できることを確認する。 */
public final class AirPlacementSurvivalClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        boolean[] original = new boolean[1];
        AtomicReference<BlockPos> placedAt = new AtomicReference<>();

        context.runOnClient(client -> {
            original[0] = FeatureSwitches.AIR_PLACEMENT.getBooleanValue();
            FeatureSwitches.AIR_PLACEMENT.setBooleanValueSilently(true);
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksDownload();
            world.getServer().runOnServer(server -> {
                if (server.getPlayerList().getPlayers().isEmpty()) {
                    throw new AssertionError("singleplayer test player is unavailable");
                }
                var player = server.getPlayerList().getPlayers().getFirst();
                require(!player.isCreative(), "Air Placement regression must run in Survival");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE, 2));

                int targetX = (int) Math.ceil(player.getBoundingBox().maxX);
                int targetY = (int) Math.floor(player.getEyeY());
                int targetZ = (int) Math.floor(player.getZ());
                server.overworld().setBlockAndUpdate(new BlockPos(targetX, targetY, targetZ), Blocks.AIR.defaultBlockState());
            });

            context.waitTicks(10);
            context.runOnClient(client -> {
                require(client.player != null && client.level != null && client.gameMode != null,
                        "client placement context is unavailable");
                client.player.setYRot(-90.0F);
                client.player.setXRot(0.0F);

                BlockHitResult miss = BlockHitResult.miss(
                        client.player.getEyePosition().add(new Vec3(4.0D, 0.0D, 0.0D)),
                        Direction.EAST,
                        client.player.blockPosition());
                BlockHitResult airHit = AirPlacementTarget.resolve(client.player, client.level, miss);
                require(airHit != null, "Air Placement did not resolve a target for a normal MISS");
                placedAt.set(airHit.getBlockPos());

                InteractionResult result = client.gameMode.useItemOn(
                        client.player, InteractionHand.MAIN_HAND, airHit);
                require(result.consumesAction(), "vanilla useItemOn did not accept the Air Placement target");
            });

            context.waitTicks(10);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                BlockPos target = placedAt.get();
                require(target != null, "Air Placement target was not captured");
                require(server.overworld().getBlockState(target).is(Blocks.STONE),
                        "Survival Air Placement did not place the held block");
                require(player.getMainHandItem().getCount() == 1,
                        "Survival Air Placement must consume exactly one held block");
            });
        } finally {
            context.runOnClient(client ->
                    FeatureSwitches.AIR_PLACEMENT.setBooleanValueSilently(original[0]));
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
