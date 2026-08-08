package dev.chise.chisetweaks.feature.rendering.worksite;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import dev.chise.chisetweaks.feature.rendering.WorldLineGeometry;
import dev.chise.chisetweaks.runtime.ClientCallbackCircuitBreaker;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Owns high-visibility world-space rendering for bounded worksite targets. */
final class WorksiteOverlayRenderer {
    private static final int[] PULSE_COLORS = {
            0xFFFF3B30,
            0xFFFF9500,
            0xFFFFE14A,
            0xFF71E35B,
            0xFF50E3E6,
            0xFF5A7DFF,
            0xFFC66BFF
    };

    private final BooleanSupplier activeSupplier;
    private final Consumer<LevelRenderContext> guardedRender;
    private volatile List<WorksiteVisibleTarget> targets = List.of();

    WorksiteOverlayRenderer(BooleanSupplier activeSupplier) {
        this.activeSupplier = activeSupplier;
        this.guardedRender = this::renderSafely;
    }

    void init() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::render);
    }

    void updateTargets(List<WorksiteVisibleTarget> targets) {
        this.targets = targets == null ? List.of() : targets;
    }

    void clear() {
        targets = List.of();
    }

    private void render(LevelRenderContext context) {
        if (!activeSupplier.getAsBoolean()) return;
        LocalFeatureConfig config = LocalFeatureConfig.getInstance();
        if (!config.worksiteVisibilityWorldOverlay
                || ClientCallbackCircuitBreaker.isOpen(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_WORLD_RENDER)) return;
        ClientCallbackCircuitBreaker.run(
                ClientCallbackCircuitBreaker.Callback.WORKSITE_VISIBILITY_WORLD_RENDER,
                context,
                guardedRender);
    }

    private void renderSafely(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        List<WorksiteVisibleTarget> snapshot = targets;
        if (snapshot.isEmpty() || client.player == null || client.level == null || client.screen != null) return;
        Vec3 cameraPosition = context.levelState().cameraRenderState.pos;
        if (cameraPosition == null) return;

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            context.submitNodeCollector().submitCustomGeometry(
                    poseStack,
                    RenderTypes.lines(),
                    (pose, vertices) -> {
                        long pulseFrame = System.nanoTime() / 140_000_000L;
                        for (WorksiteVisibleTarget target : snapshot) {
                            drawTarget(vertices, pose, target, pulseFrame);
                        }
                    });
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawTarget(
            com.mojang.blaze3d.vertex.VertexConsumer vertices,
            PoseStack.Pose pose,
            WorksiteVisibleTarget target,
            long pulseFrame) {
        VisualAssistanceStylePolicy.OverlayStyle style = target.style();
        int primary = style.argb();
        switch (style.marker()) {
            case THREAD_SIGNAL -> {
                int stateColor = target.presentation().details().contains("powered=true")
                        ? 0xFFFF3B30
                        : 0xFF46FF6A;
                WorldLineGeometry.drawThreadSignal(
                        vertices, pose, target.position(), target.presentation().details(), stateColor, 4.6f);
                if (target.presentation().blockId().endsWith("tripwire_hook")) {
                    WorldLineGeometry.drawBox(vertices, pose, target.position(), stateColor, 2.4f);
                    WorldLineGeometry.drawOrientation(
                            vertices, pose, target.position(), target.orientation(), 0xFFFFFFFF, 2.2f);
                }
            }
            case SURFACE_HATCH -> WorldLineGeometry.drawSurfaceHatch(
                    vertices, pose, target.position(), primary, 0xE6FFFFFF, 3.0f);
            case GLASS_GRID -> WorldLineGeometry.drawGlassGrid(
                    vertices, pose, target.position(), primary, 0xE6FFFFFF, 3.2f);
            case ORIENTATION -> {
                WorldLineGeometry.drawBox(vertices, pose, target.position(), primary, 2.2f);
                WorldLineGeometry.drawOrientation(
                        vertices, pose, target.position(), target.orientation(), 0xFFFFFFFF, 3.0f);
            }
            case MATERIAL_PULSE -> WorldLineGeometry.drawMaterialPulse(
                    vertices, pose, target.position(), primary, pulseColor(target, pulseFrame), 3.4f);
            case NETHER_GRID -> WorldLineGeometry.drawNetherGrid(
                    vertices, pose, target.position(), primary, 0xBFF7E8DC, 2.4f);
            case NONE -> { }
        }
    }

    private static int pulseColor(WorksiteVisibleTarget target, long pulseFrame) {
        int offset = Math.floorMod(target.position().hashCode(), PULSE_COLORS.length);
        int frame = (int) Math.floorMod(pulseFrame, PULSE_COLORS.length);
        return PULSE_COLORS[(frame + offset) % PULSE_COLORS.length];
    }
}
