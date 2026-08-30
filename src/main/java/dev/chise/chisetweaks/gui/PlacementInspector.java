package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.SchematicPlacementComparisonPolicy;
import dev.chise.chisetweaks.integration.masa.LitematicaSchematicAccess;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Vanilla placement prediction, short-lived actual-placement comparison and optional
 * Litematica schematic comparison share one bounded runtime component.
 */
public final class PlacementInspector
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    static final long EXPIRY_TICKS = 100L;
    static final long SETTLE_TICKS = 2L;

    static final int NONE = -1;
    static final int UNAVAILABLE = 0;
    static final int MATCH = 1;
    static final int ADJUSTED = 2;
    static final int DIFFERENT = 3;

    private static final SchematicSnapshot NO_SCHEMATIC =
            new SchematicSnapshot(null, 0, "", "");
    private static volatile PlacementInspector active;
    private static volatile SchematicSnapshot schematicSnapshot = NO_SCHEMATIC;

    private Level pendingLevel;
    private BlockPos targetPos;
    BlockState predictedState;
    BlockState actualState;
    private long creationTick;
    private boolean acceptingCallbacks;

    @Override
    public String getId() {
        return "placement-inspector";
    }

    @Override
    public void init() {
        active = this;
        acceptingCallbacks = true;
        UseBlockCallback.EVENT.register(this::onUseBlock);
    }

    @Override
    public void tick(Minecraft client) {
        if (client == null || client.level == null) {
            clear();
            schematicSnapshot = NO_SCHEMATIC;
            return;
        }
        observe(client.level, client.level.getGameTime());
        refreshSchematic(client);
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
        schematicSnapshot = NO_SCHEMATIC;
    }

    @Override
    public void onQuarantined(Minecraft client) {
        acceptingCallbacks = false;
        clear();
        schematicSnapshot = NO_SCHEMATIC;
        if (active == this) active = null;
    }

    private InteractionResult onUseBlock(
            Player player,
            Level level,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!acceptingCallbacks
                || !level.isClientSide()
                || hand != InteractionHand.MAIN_HAND
                || player != Minecraft.getInstance().player) return InteractionResult.PASS;
        try {
            capture(level, player, hand, hit, level.getGameTime());
        } catch (RuntimeException | LinkageError ignored) {
            clear();
        }
        return InteractionResult.PASS;
    }

    boolean capture(
            Level level,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            long tick) {
        if (level == null || player == null || hand != InteractionHand.MAIN_HAND || hit == null) return false;
        CrosshairInspector.PlacementProbe probe = CrosshairInspector.placementProbe(
                level,
                player,
                hand,
                player.getItemInHand(hand),
                hit,
                true);
        if (probe == null || probe.predictedState() == null || !level.isLoaded(probe.targetPos())) {
            return false;
        }

        pendingLevel = level;
        targetPos = probe.targetPos();
        predictedState = probe.predictedState();
        actualState = null;
        creationTick = tick;
        return true;
    }

    void observe(Level level, long tick) {
        if (targetPos == null) return;
        if (level == null
                || pendingLevel != level
                || tick < creationTick
                || tick - creationTick > EXPIRY_TICKS) {
            clear();
            return;
        }
        if (actualState != null || tick - creationTick < SETTLE_TICKS || !level.isLoaded(targetPos)) return;
        BlockState observed = level.getBlockState(targetPos);
        int observedResult = compare(predictedState, observed);
        if (observedResult == DIFFERENT || observedResult == UNAVAILABLE) return;
        actualState = observed;
        InteractionHistory.recordPlacement(targetPos, predictedState, actualState);
    }

    private void refreshSchematic(Minecraft client) {
        if (!LocalFeatureConfig.getInstance().schematicPlacementInspectorEnabled
                || client.player == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            schematicSnapshot = NO_SCHEMATIC;
            return;
        }

        CrosshairInspector.PlacementProbe probe = CrosshairInspector.placementProbe(
                client.level,
                client.player,
                InteractionHand.MAIN_HAND,
                client.player.getMainHandItem(),
                hit,
                true);
        if (probe == null || probe.predictedState() == null || !client.level.isLoaded(probe.targetPos())) {
            schematicSnapshot = NO_SCHEMATIC;
            return;
        }

        BlockState expected = LitematicaSchematicAccess.expectedState(probe.targetPos());
        int result = SchematicPlacementComparisonPolicy.compare(
                expected,
                probe.predictedState(),
                MasaIntegrationConfig.getInstance().pickRedirectMap);
        if (result == SchematicPlacementComparisonPolicy.NONE) {
            schematicSnapshot = NO_SCHEMATIC;
            return;
        }

        schematicSnapshot = new SchematicSnapshot(
                probe.targetPos(),
                result,
                blockId(expected),
                blockId(probe.predictedState()));
    }

    static int compare(BlockState predicted, BlockState actual) {
        if (predicted == null || actual == null) return UNAVAILABLE;
        if (predicted.getBlock() != actual.getBlock()) return DIFFERENT;
        return predicted.equals(actual) ? MATCH : ADJUSTED;
    }

    static PlacementInspector activeAt(Level level, BlockPos pos) {
        PlacementInspector inspector = active;
        return inspector != null
                && inspector.targetPos != null
                && inspector.pendingLevel == level
                && inspector.targetPos.equals(pos) ? inspector : null;
    }

    static SchematicSnapshot schematicSnapshot() {
        return schematicSnapshot;
    }

    static String schematicResultLabelAt(BlockPos pos) {
        SchematicSnapshot current = schematicSnapshot;
        return pos != null && pos.equals(current.targetPos())
                ? SchematicPlacementComparisonPolicy.label(current.result())
                : "";
    }

    void clear() {
        if (targetPos == null) return;
        pendingLevel = null;
        targetPos = null;
        predictedState = null;
        actualState = null;
        creationTick = 0L;
    }

    private static String blockId(BlockState state) {
        return state == null ? "" : String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    record SchematicSnapshot(
            BlockPos targetPos,
            int result,
            String expectedId,
            String predictedId) {

        boolean available() {
            return targetPos != null && result != SchematicPlacementComparisonPolicy.NONE;
        }

        String resultLabel() {
            return SchematicPlacementComparisonPolicy.label(result);
        }
    }
}
