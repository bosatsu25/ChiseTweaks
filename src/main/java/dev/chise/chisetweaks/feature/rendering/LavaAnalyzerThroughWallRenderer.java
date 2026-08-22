package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import dev.chise.chisetweaks.core.performance.WorksiteVisibilityBudgetPolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

final class LavaAnalyzerThroughWallRenderer implements AutoCloseable {
    private static final RenderPipeline THROUGH_WALL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "chisetweaks", "pipeline/lava_analyzer_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());
    private static final float BOX_INSET = 0.018f;

    private final LavaSourceSnapshot.Capture capture = new LavaSourceSnapshot.Capture(
            WorksiteVisibilityBudgetPolicy.MAX_OVERLAY_RESULTS);
    private final RetainedThroughWallBuffer retainedBuffer = new RetainedThroughWallBuffer(
            "ChiseTweaks Lava Source Highlight retained buffer",
            "ChiseTweaks Lava Source Highlight retained rendering");

    private long uploadedRevision = Long.MIN_VALUE;
    private boolean closed;

    void render(LevelRenderContext context, LavaSourceSnapshot sources) {
        if (closed || context == null || sources == null || sources.isEmpty()) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        if (camera == null) return;

        if (sources.renderRevision() != uploadedRevision) {
            sources.captureInto(capture);
            if (capture.count() == 0) {
                retainedBuffer.clearDrawData();
                uploadedRevision = capture.revision();
                return;
            }
            rebuildAndUpload(capture);
            uploadedRevision = capture.revision();
        }

        retainedBuffer.draw(Minecraft.getInstance(), THROUGH_WALL_PIPELINE, camera);
    }

    private void rebuildAndUpload(LavaSourceSnapshot.Capture state) {
        long anchor = state.positionAt(0);
        int anchorX = BlockPos.getX(anchor);
        int anchorY = BlockPos.getY(anchor);
        int anchorZ = BlockPos.getZ(anchor);

        BufferBuilder buffer = retainedBuffer.newBufferBuilder(THROUGH_WALL_PIPELINE);
        for (int index = 0; index < state.count(); index++) {
            long packed = state.positionAt(index);
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);
            double dx = x + 0.5 - state.eyeX();
            double dy = y + 0.5 - state.eyeY();
            double dz = z + 0.5 - state.eyeZ();
            int argb = LavaVisionPalettePolicy.colorForDistance(
                    Math.sqrt(dx * dx + dy * dy + dz * dz));
            ThroughWallWireBoxGeometry.drawWireBox(
                    buffer,
                    x - anchorX,
                    y - anchorY,
                    z - anchorZ,
                    argb,
                    LavaVisionPalettePolicy.ANALYZER_EDGE_THICKNESS,
                    BOX_INSET);
        }

        MeshData builtBuffer = buffer.buildOrThrow();
        try {
            retainedBuffer.upload(THROUGH_WALL_PIPELINE, builtBuffer, anchorX, anchorY, anchorZ);
        } finally {
            builtBuffer.close();
        }
    }

    /** 一時的なCPU/GPU作業領域を破棄し、後続セッションへ失敗フレームの状態を持ち越さない。 */
    void resetAfterFailure() {
        if (closed) return;
        uploadedRevision = Long.MIN_VALUE;
        retainedBuffer.resetAfterFailure();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        uploadedRevision = Long.MIN_VALUE;
        retainedBuffer.close();
    }
}
