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

/** Owns only world-space rendering for worksite targets. */
final class WorksiteOverlayRenderer {
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
                        for (WorksiteVisibleTarget target : snapshot) {
                            drawTarget(vertices, pose, target);
                        }
                    });
        } finally {
            poseStack.popPose();
        }
    }

    private static void drawTarget(
            com.mojang.blaze3d.vertex.VertexConsumer vertices,
            PoseStack.Pose pose,
            WorksiteVisibleTarget target) {
        VisualAssistanceStylePolicy.OverlayStyle style = target.style();
        int color = style.argb();
        float lineWidth = style.priority() >= 80 ? 2.6f : 2.0f;
        WorldLineGeometry.drawBox(vertices, pose, target.position(), color, lineWidth);
        switch (style.marker()) {
            case CROSS -> WorldLineGeometry.drawCross(vertices, pose, target.position(), color, lineWidth);
            case DIAGONAL -> WorldLineGeometry.drawDiagonal(vertices, pose, target.position(), color, lineWidth);
            case ORIENTATION -> WorldLineGeometry.drawOrientation(
                    vertices, pose, target.position(), target.orientation(), color, lineWidth);
            case BOX, NONE -> { }
        }
    }
}
