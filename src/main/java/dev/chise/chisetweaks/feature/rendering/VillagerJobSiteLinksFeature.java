package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.policy.FeatureAvailabilityPolicy;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Visualizes only villager Job Site relations already known by Minecraft.
 *
 * <p>No workstation guessing or surrounding-block scan is performed. Unknown relations stay
 * unknown instead of being inferred.</p>
 */
public final class VillagerJobSiteLinksFeature implements TickingFeature, SessionAwareRuntimeComponent {
    private static final int HORIZONTAL_RADIUS = 16;
    private static final int VERTICAL_RADIUS = 8;
    private static final int MAX_VILLAGERS = 12;
    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static final int LINK_COLOR = 0xFF72FF9F;

    private volatile List<Link> links = List.of();
    private int ticksUntilUpdate;
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
            ticksUntilUpdate = 0;
            links = List.of();
        }
        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
            return;
        }
        ticksUntilUpdate = UPDATE_INTERVAL_TICKS - 1;
        links = knownLinks(client);
    }

    private List<Link> knownLinks(Minecraft client) {
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
            if (memory.isEmpty() || !memory.get().dimension().equals(client.level.dimension())) continue;
            result.add(new Link(
                    villager.getX(),
                    villager.getY() + villager.getBbHeight() * 0.65,
                    villager.getZ(),
                    memory.get().pos()));
        }
        return List.copyOf(result);
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
                            float endX = link.jobSite().getX() + 0.5f;
                            float endY = link.jobSite().getY() + 0.5f;
                            float endZ = link.jobSite().getZ() + 0.5f;
                            SurfaceLinePrimitives.line(
                                    vertices, pose,
                                    (float) link.villagerX(), (float) link.villagerY(), (float) link.villagerZ(),
                                    endX, endY, endZ,
                                    LINK_COLOR, 2.4f);
                            SurfaceLinePrimitives.drawFaceFrame(
                                    vertices, pose, link.jobSite(), LINK_COLOR, 2.0f);
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
        ticksUntilUpdate = 0;
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
            BlockPos jobSite) {}
}
