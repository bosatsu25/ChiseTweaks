package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
import dev.chise.chisetweaks.core.policy.VillagerWorkstationPolicy;
import dev.chise.chisetweaks.feature.TickingFeature;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Shows the relationship between nearby villagers and their known/fallback workstation. */
public final class VillagerAnalyzerFeature implements TickingFeature, SessionAwareRuntimeComponent {
    private static final int HORIZONTAL_RADIUS = 16;
    private static final int VERTICAL_RADIUS = 8;
    private static final int FALLBACK_WORKSTATION_RADIUS = 8;
    private static final int MAX_VILLAGERS = 12;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int PRIMARY_COLOR = 0xFF72FF9F;
    private static final int FALLBACK_COLOR = 0xFFFFC857;

    private final BlockPos.MutableBlockPos workstationCursor = new BlockPos.MutableBlockPos();
    private volatile List<Link> links = List.of();
    private int ticksUntilScan;
    private ClientLevel lastLevel;

    @Override
    public String getId() {
        return FeatureDefinition.VILLAGER_ANALYZER.id();
    }

    @Override
    public String getName() {
        return FeatureDefinition.VILLAGER_ANALYZER.englishName();
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
            links = List.of();
        }
        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }
        ticksUntilScan = SCAN_INTERVAL_TICKS - 1;
        links = scan(client);
    }

    private List<Link> scan(Minecraft client) {
        Vec3 player = client.player.position();
        AABB bounds = new AABB(
                player.x - HORIZONTAL_RADIUS,
                player.y - VERTICAL_RADIUS,
                player.z - HORIZONTAL_RADIUS,
                player.x + HORIZONTAL_RADIUS,
                player.y + VERTICAL_RADIUS,
                player.z + HORIZONTAL_RADIUS);

        ArrayList<Villager> villagers = new ArrayList<>(
                client.level.getEntitiesOfClass(Villager.class, bounds, villager -> !villager.isRemoved()));
        villagers.sort(Comparator.comparingDouble(villager -> villager.distanceToSqr(client.player)));
        if (villagers.size() > MAX_VILLAGERS) villagers.subList(MAX_VILLAGERS, villagers.size()).clear();

        ArrayList<Link> result = new ArrayList<>(villagers.size());
        for (Villager villager : villagers) {
            Optional<GlobalPos> memory = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            if (memory.isPresent() && memory.get().dimension().equals(client.level.dimension())) {
                result.add(new Link(
                        villager.getX(),
                        villager.getY() + villager.getBbHeight() * 0.65,
                        villager.getZ(),
                        memory.get().pos(),
                        true));
                continue;
            }

            Block workstation = VillagerWorkstationPolicy.workstation(
                    villager.getVillagerData().profession().getRegisteredName());
            BlockPos fallback = workstation == null
                    ? null
                    : findNearestLoadedWorkstation(client.level, villager.blockPosition(), workstation);
            if (fallback != null) {
                result.add(new Link(
                        villager.getX(),
                        villager.getY() + villager.getBbHeight() * 0.65,
                        villager.getZ(),
                        fallback,
                        false));
            }
        }
        return List.copyOf(result);
    }

    private BlockPos findNearestLoadedWorkstation(
            ClientLevel level,
            BlockPos origin,
            Block workstation) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int radius = FALLBACK_WORKSTATION_RADIUS;
        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();
        for (int z = originZ - radius; z <= originZ + radius; z++) {
            for (int x = originX - radius; x <= originX + radius; x++) {
                if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) continue;
                int dx = x - originX;
                int dz = z - originZ;
                for (int y = originY - radius; y <= originY + radius; y++) {
                    workstationCursor.set(x, y, z);
                    if (!level.getBlockState(workstationCursor).is(workstation)) continue;
                    int dy = y - originY;
                    double distance = (double) dx * dx + (double) dy * dy + (double) dz * dz;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new BlockPos(x, y, z);
                    }
                }
            }
        }
        return best;
    }

    private void render(LevelRenderContext context) {
        if (!isEnabled() || links.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null || client.level != lastLevel) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        if (camera == null) return;
        List<Link> snapshot = links;

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.lines(),
                    (pose, vertices) -> {
                        for (Link link : snapshot) {
                            int color = link.claimed() ? PRIMARY_COLOR : FALLBACK_COLOR;
                            float endX = link.jobSite().getX() + 0.5f;
                            float endY = link.jobSite().getY() + 0.5f;
                            float endZ = link.jobSite().getZ() + 0.5f;
                            SurfaceLinePrimitives.line(
                                    vertices, pose,
                                    (float) link.villagerX(), (float) link.villagerY(), (float) link.villagerZ(),
                                    endX, endY, endZ,
                                    color, 2.4f);
                            SurfaceLinePrimitives.drawFaceFrame(
                                    vertices, pose, link.jobSite(), color, 2.0f);
                        }
                    });
        } finally {
            poseStack.popPose();
        }
    }

    private boolean isEnabled() {
        return FeatureAvailabilityPolicy.isAvailable(FeatureDefinition.VILLAGER_ANALYZER)
                && LocalFeatureConfig.getInstance().villagerAnalyzerEnabled;
    }

    private void clear() {
        links = List.of();
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

    public record Link(
            double villagerX,
            double villagerY,
            double villagerZ,
            BlockPos jobSite,
            boolean claimed) {}
}
