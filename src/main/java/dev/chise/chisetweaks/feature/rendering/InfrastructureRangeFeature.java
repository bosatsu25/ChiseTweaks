package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.core.policy.InfrastructureRangePolicy;
import dev.chise.chisetweaks.feature.TickingFeature;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
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

/** Bounded loaded-world scanner for Beacon and Lightning Rod range visualization. */
public final class InfrastructureRangeFeature implements TickingFeature, SessionAwareRuntimeComponent {
    public enum Mode {
        BEACON(FeatureDefinition.BEACON_RANGE, 0xFF55F3FF),
        LIGHTNING_ROD(FeatureDefinition.LIGHTNING_ROD_RANGE, 0xFFFFD45A);

        private final FeatureDefinition definition;
        private final int argb;

        Mode(FeatureDefinition definition, int argb) {
            this.definition = definition;
            this.argb = argb;
        }
    }

    private final Mode mode;
    private final BlockPos.MutableBlockPos scanCursor = new BlockPos.MutableBlockPos();
    private volatile List<Target> targets = List.of();
    private int ticksUntilScan;
    private ClientLevel lastLevel;

    public InfrastructureRangeFeature(Mode mode) {
        this.mode = java.util.Objects.requireNonNull(mode, "mode");
    }

    @Override
    public String getId() {
        return mode.definition.id();
    }

    @Override
    public String getName() {
        return mode.definition.englishName();
    }

    @Override
    public void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
    }

    @Override
    public void tick(Minecraft client) {
        if (!isEnabled() || client == null || client.player == null || client.level == null) {
            clear();
            return;
        }
        if (lastLevel != client.level) {
            lastLevel = client.level;
            ticksUntilScan = 0;
            targets = List.of();
        }
        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }
        ticksUntilScan = InfrastructureRangePolicy.SCAN_INTERVAL_TICKS - 1;
        targets = scan(client);
    }

    private List<Target> scan(Minecraft client) {
        BlockPos origin = client.player.blockPosition();
        int hr = InfrastructureRangePolicy.DISCOVERY_HORIZONTAL_RADIUS;
        int vr = InfrastructureRangePolicy.DISCOVERY_VERTICAL_RADIUS;
        ArrayList<Target> result = new ArrayList<>(InfrastructureRangePolicy.MAX_TARGETS);

        for (int z = origin.getZ() - hr; z <= origin.getZ() + hr; z++) {
            for (int x = origin.getX() - hr; x <= origin.getX() + hr; x++) {
                LevelChunk sourceChunk = client.level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                if (sourceChunk == null) continue;
                for (int y = origin.getY() - vr; y <= origin.getY() + vr; y++) {
                    scanCursor.set(x, y, z);
                    BlockState state = sourceChunk.getBlockState(scanCursor);
                    int range = 0;
                    if (mode == Mode.BEACON && state.is(Blocks.BEACON)) {
                        range = InfrastructureRangePolicy.beaconRadius(beaconLevel(client.level, x, y, z));
                    } else if (mode == Mode.LIGHTNING_ROD && state.is(Blocks.LIGHTNING_ROD)) {
                        range = LightningRodBlock.RANGE;
                    }
                    if (range <= 0) continue;
                    result.add(new Target(new BlockPos(x, y, z), range));
                    if (result.size() >= InfrastructureRangePolicy.MAX_TARGETS) {
                        return List.copyOf(result);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private int beaconLevel(ClientLevel level, int beaconX, int beaconY, int beaconZ) {
        int complete = 0;
        for (int layer = 1; layer <= InfrastructureRangePolicy.MAX_BEACON_LEVEL; layer++) {
            int y = beaconY - layer;
            boolean valid = true;
            for (int z = beaconZ - layer; z <= beaconZ + layer && valid; z++) {
                for (int x = beaconX - layer; x <= beaconX + layer; x++) {
                    scanCursor.set(x, y, z);
                    if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)
                            || !level.getBlockState(scanCursor).is(BlockTags.BEACON_BASE_BLOCKS)) {
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

    private void render(LevelRenderContext context) {
        if (!isEnabled() || targets.isEmpty()) return;
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
                            drawHorizontalRange(vertices, pose, target, mode.argb);
                            SurfaceLinePrimitives.drawFaceFrame(
                                    vertices, pose, target.position(), mode.argb, 2.0f);
                        }
                    });
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawHorizontalRange(
            com.mojang.blaze3d.vertex.VertexConsumer vertices,
            PoseStack.Pose pose,
            Target target,
            int argb) {
        float minX = target.position().getX() + 0.5f - target.range();
        float maxX = target.position().getX() + 0.5f + target.range();
        float minZ = target.position().getZ() + 0.5f - target.range();
        float maxZ = target.position().getZ() + 0.5f + target.range();
        float y = target.position().getY() + 1.05f;
        float width = target.range() >= 100 ? 2.6f : 2.0f;
        SurfaceLinePrimitives.line(vertices, pose, minX, y, minZ, maxX, y, minZ, argb, width);
        SurfaceLinePrimitives.line(vertices, pose, maxX, y, minZ, maxX, y, maxZ, argb, width);
        SurfaceLinePrimitives.line(vertices, pose, maxX, y, maxZ, minX, y, maxZ, argb, width);
        SurfaceLinePrimitives.line(vertices, pose, minX, y, maxZ, minX, y, minZ, argb, width);
    }

    private boolean isEnabled() {
        if (!FeatureAvailabilityPolicy.isAvailable(mode.definition)) return false;
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        return mode == Mode.BEACON ? config.beaconRangeEnabled : config.lightningRodRangeEnabled;
    }

    private void clear() {
        targets = List.of();
        ticksUntilScan = 0;
        lastLevel = null;
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        clear();
    }

    private record Target(BlockPos position, int range) {}
}
