package dev.chise.chisetweaks.gui;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 実際のvanilla配置結果を、製品側previewのoracleとして比較するclient integration test。 */
public final class TrapdoorPlacementClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksDownload();
            world.getServer().runOnServer(server -> {
                if (server.getPlayerList().getPlayers().isEmpty()) {
                    throw new AssertionError("singleplayer test player is unavailable");
                }
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = server.overworld();
                BlockPos origin = player.blockPosition().offset(8, 2, 8);

                BlockHitResult unsupportedHit = new BlockHitResult(
                        Vec3.atCenterOf(origin), Direction.UP, origin, false);
                require(CrosshairInspector.predictTrapdoorState(
                                level,
                                player,
                                InteractionHand.MAIN_HAND,
                                new ItemStack(Items.STICK),
                                unsupportedHit,
                                true) == null,
                        "non-BlockItem must not produce a preview");
                require(CrosshairInspector.predictTrapdoorState(
                                level,
                                player,
                                InteractionHand.MAIN_HAND,
                                new ItemStack(Blocks.OAK_TRAPDOOR),
                                unsupportedHit,
                                false) == null,
                        "disabled preview must not produce a descriptor");

                assertPlacement(level, player, origin, 0, Blocks.OAK_TRAPDOOR,
                        Direction.NORTH, 0.25D, TargetKind.SOLID, false, Direction.SOUTH, true);
                assertPlacement(level, player, origin, 1, Blocks.OAK_TRAPDOOR,
                        Direction.EAST, 0.25D, TargetKind.SOLID, false, Direction.SOUTH, true);
                assertPlacement(level, player, origin, 2, Blocks.OAK_TRAPDOOR,
                        Direction.SOUTH, 0.25D, TargetKind.SOLID, false, Direction.NORTH, true);
                assertPlacement(level, player, origin, 3, Blocks.OAK_TRAPDOOR,
                        Direction.WEST, 0.25D, TargetKind.SOLID, false, Direction.NORTH, true);
                assertPlacement(level, player, origin, 4, Blocks.OAK_TRAPDOOR,
                        Direction.UP, 1.0D, TargetKind.SOLID, false, Direction.EAST, true);
                assertPlacement(level, player, origin, 5, Blocks.OAK_TRAPDOOR,
                        Direction.DOWN, 0.0D, TargetKind.SOLID, false, Direction.WEST, true);
                assertPlacement(level, player, origin, 6, Blocks.OAK_TRAPDOOR,
                        Direction.NORTH, 0.75D, TargetKind.SOLID, false, Direction.SOUTH, true);
                assertPlacement(level, player, origin, 7, Blocks.OAK_TRAPDOOR,
                        Direction.NORTH, 0.25D, TargetKind.SOLID, true, Direction.SOUTH, true);
                assertPlacement(level, player, origin, 8, Blocks.OAK_TRAPDOOR,
                        Direction.UP, 1.0D, TargetKind.REPLACEABLE, false, Direction.NORTH, true);
                assertPlacement(level, player, origin, 9, Blocks.OAK_TRAPDOOR,
                        Direction.UP, 1.0D, TargetKind.WATER, false, Direction.SOUTH, true);
                assertPlacement(level, player, origin, 10, Blocks.OAK_TRAPDOOR,
                        Direction.EAST, 0.25D, TargetKind.BLOCKED, false, Direction.SOUTH, false);
                assertPlacement(level, player, origin, 11, Blocks.IRON_TRAPDOOR,
                        Direction.WEST, 0.75D, TargetKind.SOLID, false, Direction.NORTH, true);
            });
        }
    }

    private static void assertPlacement(
            ServerLevel level,
            ServerPlayer player,
            BlockPos origin,
            int index,
            Block trapdoor,
            Direction face,
            double clickY,
            TargetKind targetKind,
            boolean sneaking,
            Direction playerFacing,
            boolean expectedPossible) {
        BlockPos clicked = origin.offset(index * 3, 0, 0);
        clear(level, clicked);
        switch (targetKind) {
            case SOLID, BLOCKED -> level.setBlockAndUpdate(clicked, Blocks.STONE.defaultBlockState());
            case REPLACEABLE -> {
                level.setBlockAndUpdate(clicked.below(), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(clicked, Blocks.SHORT_GRASS.defaultBlockState());
            }
            case WATER -> level.setBlockAndUpdate(clicked, Blocks.WATER.defaultBlockState());
        }
        if (targetKind == TargetKind.BLOCKED) {
            level.setBlockAndUpdate(clicked.relative(face), Blocks.STONE.defaultBlockState());
        }

        player.setYRot(playerFacing.toYRot());
        player.setXRot(0.0F);
        player.setShiftKeyDown(sneaking);
        BlockHitResult hit = new BlockHitResult(
                hitLocation(clicked, face, clickY),
                face,
                clicked,
                false);
        ItemStack previewStack = new ItemStack(trapdoor);
        BlockState predicted = CrosshairInspector.predictTrapdoorState(
                level,
                player,
                InteractionHand.MAIN_HAND,
                previewStack,
                hit,
                true);

        ItemStack actualStack = new ItemStack(trapdoor);
        BlockPlaceContext actualContext = new BlockPlaceContext(
                level,
                player,
                InteractionHand.MAIN_HAND,
                actualStack,
                hit);
        BlockPos actualPos = actualContext.getClickedPos();
        ((BlockItem) actualStack.getItem()).place(actualContext);
        BlockState actual = level.getBlockState(actualPos);

        boolean actuallyPlaced = actual.getBlock() == trapdoor;
        require((predicted != null) == expectedPossible,
                "preview possibility mismatch for " + targetKind);
        require(actuallyPlaced == expectedPossible,
                "vanilla placement possibility mismatch for " + targetKind);
        if (expectedPossible) {
            require(predicted.getValue(TrapDoorBlock.FACING) == actual.getValue(TrapDoorBlock.FACING),
                    "facing mismatch");
            require(predicted.getValue(TrapDoorBlock.HALF) == actual.getValue(TrapDoorBlock.HALF),
                    "half mismatch");
            require(predicted.getValue(TrapDoorBlock.OPEN).equals(actual.getValue(TrapDoorBlock.OPEN)),
                    "open mismatch");
            require(predicted.getValue(TrapDoorBlock.WATERLOGGED)
                            .equals(actual.getValue(TrapDoorBlock.WATERLOGGED)),
                    "waterlogged mismatch");
        }
        player.setShiftKeyDown(false);
    }

    private static Vec3 hitLocation(BlockPos pos, Direction face, double clickY) {
        double x = pos.getX() + 0.5D + face.getStepX() * 0.5D;
        double y = pos.getY() + (face == Direction.UP ? 1.0D : face == Direction.DOWN ? 0.0D : clickY);
        double z = pos.getZ() + 0.5D + face.getStepZ() * 0.5D;
        return new Vec3(x, y, z);
    }

    private static void clear(ServerLevel level, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlockAndUpdate(center.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private enum TargetKind {
        SOLID,
        REPLACEABLE,
        WATER,
        BLOCKED
    }
}
