package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.SchematicPlacementComparisonPolicy;
import dev.chise.chisetweaks.integration.masa.LitematicaSchematicAccess;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class SchematicPlacementInspector
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    private static final Snapshot NONE = new Snapshot(null, null, null, 0, "", "");
    private static volatile Snapshot snapshot = NONE;

    @Override
    public String getId() {
        return "schematic-placement-inspector";
    }

    @Override
    public void init() {}

    @Override
    public void tick(Minecraft client) {
        if (!LocalFeatureConfig.getInstance().schematicPlacementInspectorEnabled
                || client == null
                || client.player == null
                || client.level == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(client.player.getMainHandItem().getItem() instanceof BlockItem)) {
            snapshot = NONE;
            return;
        }

        BlockState predicted = CrosshairInspector.predictPlacementState(
                client.level,
                client.player,
                InteractionHand.MAIN_HAND,
                client.player.getMainHandItem(),
                hit,
                true);
        if (predicted == null) {
            snapshot = NONE;
            return;
        }

        BlockPlaceContext context = new BlockPlaceContext(
                client.level,
                client.player,
                InteractionHand.MAIN_HAND,
                client.player.getMainHandItem(),
                hit);
        BlockPos target = context.getClickedPos();
        if (!client.level.isLoaded(target)) {
            snapshot = NONE;
            return;
        }

        BlockState expected = LitematicaSchematicAccess.expectedState(target);
        int result = SchematicPlacementComparisonPolicy.compare(
                expected,
                predicted,
                MasaIntegrationConfig.getInstance().pickRedirectMap);
        if (result == SchematicPlacementComparisonPolicy.NONE) {
            snapshot = NONE;
            return;
        }

        snapshot = new Snapshot(
                target.immutable(),
                expected,
                predicted,
                result,
                blockId(expected),
                blockId(predicted));
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static String resultLabelAt(BlockPos pos) {
        Snapshot current = snapshot;
        return pos != null && pos.equals(current.targetPos())
                ? SchematicPlacementComparisonPolicy.label(current.result())
                : "";
    }

    @Override
    public void resetSession(Minecraft client) {
        snapshot = NONE;
    }

    @Override
    public void onQuarantined(Minecraft client) {
        snapshot = NONE;
    }

    private static String blockId(BlockState state) {
        return state == null ? "" : String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public record Snapshot(
            BlockPos targetPos,
            BlockState expected,
            BlockState predicted,
            int result,
            String expectedId,
            String predictedId) {

        public boolean available() {
            return targetPos != null && result != SchematicPlacementComparisonPolicy.NONE;
        }

        public String resultLabel() {
            return SchematicPlacementComparisonPolicy.label(result);
        }
    }
}
