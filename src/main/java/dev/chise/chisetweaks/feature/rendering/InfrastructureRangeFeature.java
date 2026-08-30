package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.core.policy.InfrastructureRangePolicy;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** BeaconとLightning Rodを1回のbounded loaded-world scanで共有検出する内部runtime。 */
public final class InfrastructureRangeFeature
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    private static final String ID = "infrastructure_range";
    private static final int BEACON_MASK = 1;
    private static final int LIGHTNING_ROD_MASK = 1 << 1;
    private static final int BEACON_COLOR = 0xFF55F3FF;
    private static final int LIGHTNING_ROD_COLOR = 0xFFFFD45A;
    private static final int MAX_PRELOADED_CHUNKS = 16;

    private final BlockPos.MutableBlockPos scanCursor = new BlockPos.MutableBlockPos();
    private final LevelChunk[] loadedChunkBuffer = new LevelChunk[MAX_PRELOADED_CHUNKS];
    private volatile List<Target> targets = List.of();
    private int ticksUntilScan;
    private int lastEnabledMask;
    private ClientLevel lastLevel;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
    }

    @Override
    public void tick(Minecraft client) {
        int enabledMask = enabledMask();
        if (enabledMask == 0 || client == null || client.player == null || client.level == null) {
            clear();
            return;
        }
        if (lastLevel != client.level) {
            lastLevel = client.level;
            ticksUntilScan = 0;
            targets = List.of();
        }
        if (lastEnabledMask != enabledMask) {
            lastEnabledMask = enabledMask;
            ticksUntilScan = 0;
        }
        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }
        ticksUntilScan = InfrastructureRangePolicy.SCAN_INTERVAL_TICKS - 1;
        targets = scan(client, enabledMask);
    }

    private List<Target> scan(Minecraft client, int enabledMask) {
        BlockPos origin = client.player.blockPosition();
        int hr = InfrastructureRangePolicy.DISCOVERY_HORIZONTAL_RADIUS;
        int vr = InfrastructureRangePolicy.DISCOVERY_VERTICAL_RADIUS;
        int maxTargets = InfrastructureRangePolicy.MAX_TARGETS;
        int minX = origin.getX() - hr;
        int maxX = origin.getX() + hr;
        int minZ = origin.getZ() - hr;
        int maxZ = origin.getZ() + hr;
        int beaconMargin = (enabledMask & BEACON_MASK) != 0
                ? InfrastructureRangePolicy.MAX_BEACON_LEVEL
                : 0;
        int minChunkX = (minX - beaconMargin) >> 4;
        int maxChunkX = (maxX + beaconMargin) >> 4;
        int minChunkZ = (minZ - beaconMargin) >> 4;
        int maxChunkZ = (maxZ + beaconMargin) >> 4;
        int chunkSpanX = maxChunkX - minChunkX + 1;
        int chunkSpanZ = maxChunkZ - minChunkZ + 1;
        int chunkCount = chunkSpanX * chunkSpanZ;
        if (chunkCount > loadedChunkBuffer.length) return List.of();

        int chunkIndex = 0;
        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                loadedChunkBuffer[chunkIndex++] =
                        client.level.getChunkSource().getChunkNow(chunkX, chunkZ);
            }
        }

        ArrayList<Target> result = new ArrayList<>(maxTargets * Integer.bitCount(enabledMask));
        int beaconCount = 0;
        int lightningRodCount = 0;
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                LevelChunk sourceChunk = loadedChunkAt(
                        x, z, minChunkX, minChunkZ, chunkSpanX, chunkSpanZ);
                if (sourceChunk == null) continue;
                for (int y = origin.getY() - vr; y <= origin.getY() + vr; y++) {
                    scanCursor.set(x, y, z);
                    BlockState state = sourceChunk.getBlockState(scanCursor);
                    if ((enabledMask & BEACON_MASK) != 0
                            && beaconCount < maxTargets
                            && state.is(Blocks.BEACON)) {
                        int range = InfrastructureRangePolicy.beaconRadius(
                                beaconLevel(x, y, z, minChunkX, minChunkZ, chunkSpanX, chunkSpanZ));
                        if (range > 0) {
                            result.add(new Target(new BlockPos(x, y, z), range, BEACON_COLOR));
                            beaconCount++;
                        }
                    } else if ((enabledMask & LIGHTNING_ROD_MASK) != 0
                            && lightningRodCount < maxTargets
                            && state.is(Blocks.LIGHTNING_ROD)) {
                        result.add(new Target(
                                new BlockPos(x, y, z),
                                LightningRodBlock.RANGE,
                                LIGHTNING_ROD_COLOR));
                        lightningRodCount++;
                    }
                    if (((enabledMask & BEACON_MASK) == 0 || beaconCount >= maxTargets)
                            && ((enabledMask & LIGHTNING_ROD_MASK) == 0
                            || lightningRodCount >= maxTargets)) {
                        return List.copyOf(result);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private int beaconLevel(
            int beaconX,
            int beaconY,
            int beaconZ,
            int minChunkX,
            int minChunkZ,
            int chunkSpanX,
            int chunkSpanZ) {
        int complete = 0;
        for (int layer = 1; layer <= InfrastructureRangePolicy.MAX_BEACON_LEVEL; layer++) {
            int y = beaconY - layer;
            boolean valid = true;
            for (int z = beaconZ - layer; z <= beaconZ + layer && valid; z++) {
                for (int x = beaconX - layer; x <= beaconX + layer; x++) {
                    LevelChunk sourceChunk = loadedChunkAt(
                            x, z, minChunkX, minChunkZ, chunkSpanX, chunkSpanZ);
                    if (sourceChunk == null) {
                        valid = false;
                        break;
                    }
                    scanCursor.set(x, y, z);
                    if (!sourceChunk.getBlockState(scanCursor).is(BlockTags.BEACON_BASE_BLOCKS)) {
                        valid = false;
                        break;
                    }
                }
            }
            if (!valid) break;
            complete = layer;
        }
        return complete;
    }

    private LevelChunk loadedChunkAt(
            int blockX,
            int blockZ,
            int minChunkX,
            int minChunkZ,
            int chunkSpanX,
            int chunkSpanZ) {
        int offsetX = (blockX >> 4) - minChunkX;
        int offsetZ = (blockZ >> 4) - minChunkZ;
        if (offsetX < 0 || offsetX >= chunkSpanX || offsetZ < 0 || offsetZ >= chunkSpanZ) {
            return null;
        }
        return loadedChunkBuffer[offsetZ * chunkSpanX + offsetX];
    }

    private void render(LevelRenderContext context) {
        int enabledMask = enabledMask();
        if (enabledMask == 0 || targets.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null || client.level != lastLevel) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        if (camera == null) return;

        List<Target> snapshot = targets;
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.lines(),
                    (pose, vertices) -> {
                        for (Target target : snapshot) {
                            if (!targetEnabled(target, enabledMask)) continue;
                            drawHorizontalRange(vertices, pose, target);
                            SurfaceLinePrimitives.drawFaceFrame(
                                    vertices, pose, target.position(), target.argb(), 2.0f);
                        }
                    });
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawHorizontalRange(
            com.mojang.blaze3d.vertex.VertexConsumer vertices,
            PoseStack.Pose pose,
            Target target) {
        float minX = target.position().getX() + 0.5f - target.range();
        float maxX = target.position().getX() + 0.5f + target.range();
        float minZ = target.position().getZ() + 0.5f - target.range();
        float maxZ = target.position().getZ() + 0.5f + target.range();
        float y = target.position().getY() + 1.05f;
        float width = target.range() >= 100 ? 2.6f : 2.0f;
        int argb = target.argb();
        SurfaceLinePrimitives.line(vertices, pose, minX, y, minZ, maxX, y, minZ, argb, width);
        SurfaceLinePrimitives.line(vertices, pose, maxX, y, minZ, maxX, y, maxZ, argb, width);
        SurfaceLinePrimitives.line(vertices, pose, maxX, y, maxZ, minX, y, maxZ, argb, width);
        SurfaceLinePrimitives.line(vertices, pose, minX, y, maxZ, minX, y, minZ, argb, width);
    }

    private static boolean targetEnabled(Target target, int enabledMask) {
        return target.argb() == BEACON_COLOR
                ? (enabledMask & BEACON_MASK) != 0
                : (enabledMask & LIGHTNING_ROD_MASK) != 0;
    }

    private static int enabledMask() {
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        int mask = 0;
        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.BEACON_RANGE)
                && config.beaconRangeEnabled) {
            mask |= BEACON_MASK;
        }
        if (FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.LIGHTNING_ROD_RANGE)
                && config.lightningRodRangeEnabled) {
            mask |= LIGHTNING_ROD_MASK;
        }
        return mask;
    }

    private void clear() {
        targets = List.of();
        ticksUntilScan = 0;
        lastEnabledMask = 0;
        lastLevel = null;
        for (int index = 0; index < loadedChunkBuffer.length; index++) {
            loadedChunkBuffer[index] = null;
        }
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        clear();
    }

    private record Target(BlockPos position, int range, int argb) {}
}
