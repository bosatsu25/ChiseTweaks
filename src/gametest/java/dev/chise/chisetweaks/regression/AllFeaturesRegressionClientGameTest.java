package dev.chise.chisetweaks.regression;

import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.runtime.FeatureManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * 13個のtoggleを同時ONにした実クライアント描画smoke。
 * 共通化後も各Featureが相互排他にならず、runtime componentがquarantineされないことを確認する。
 */
public final class AllFeaturesRegressionClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        List<FeatureSwitch> switches = FeatureSwitches.VALUES;
        require(switches.size() == 13, "expected 13 canonical feature switches");
        boolean[] original = new boolean[switches.size()];

        context.runOnClient(client -> {
            for (int index = 0; index < switches.size(); index++) {
                original[index] = switches.get(index).getBooleanValue();
                switches.get(index).setBooleanValueSilently(true);
            }
            requireAllEnabled(switches);
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksDownload();
            world.getServer().runOnServer(server -> {
                if (server.getPlayerList().getPlayers().isEmpty()) {
                    throw new AssertionError("singleplayer test player is unavailable");
                }
                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                ServerLevel level = server.overworld();
                BlockPos origin = player.blockPosition().offset(6, 1, 6);

                level.setBlockAndUpdate(origin.offset(0, 0, 0), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(1, 0, 0), Blocks.WHITE_CONCRETE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(2, 0, 0), Blocks.GLASS.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(3, 0, 0), Blocks.GLASS_PANE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(4, 0, 0), Blocks.DIAMOND_ORE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(5, 0, 0), Blocks.ANCIENT_DEBRIS.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(6, 0, 0), Blocks.NETHERRACK.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(7, 0, 0), Blocks.TRIPWIRE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(8, 0, 0), Blocks.POWDER_SNOW.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(9, 0, 0), Blocks.LAVA.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(10, 0, 0), Blocks.KELP.defaultBlockState());
            });

            context.waitTicks(40);
            world.getConnection().waitForChunksRender();
            context.waitTicks(20);

            context.runOnClient(client -> {
                requireAllEnabled(switches);
                List<String> quarantined = FeatureManager.getInstance().diagnosticQuarantinedComponentIds();
                require(quarantined.isEmpty(), "runtime component quarantined with all features ON: " + quarantined);
            });
        } finally {
            context.runOnClient(client -> {
                for (int index = 0; index < switches.size(); index++) {
                    switches.get(index).setBooleanValueSilently(original[index]);
                }
            });
        }
    }

    private static void requireAllEnabled(List<FeatureSwitch> switches) {
        for (FeatureSwitch feature : switches) {
            require(feature.getBooleanValue(), "feature unexpectedly disabled: " + feature.definition().id());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
