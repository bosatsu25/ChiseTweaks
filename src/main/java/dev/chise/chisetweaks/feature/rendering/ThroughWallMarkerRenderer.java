package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.policy.HiddenBlockAnalyzerPalettePolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** 上限付きAnalyzerで共有する、地形越しの輪郭＋半透明面を描画する保持型renderer。 */
final class ThroughWallMarkerRenderer implements AutoCloseable {
    enum Style {
        LAVA_SOURCE,
        HIDDEN_BLOCK
    }

    private static final RenderPipeline THROUGH_WALL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "chisetweaks", "pipeline/analyzer_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());

    private final Style style;
    private final ThroughWallPositionSnapshot.Capture capture;
    private final RetainedThroughWallBuffer retainedBuffer;

    private long uploadedRevision = Long.MIN_VALUE;
    private boolean closed;

    ThroughWallMarkerRenderer(
            Style style,
            int capacity,
            String bufferLabel,
            String renderLabel) {
        if (style == null) throw new IllegalArgumentException("style must not be null");
        this.style = style;
        this.capture = new ThroughWallPositionSnapshot.Capture(capacity);
        this.retainedBuffer = new RetainedThroughWallBuffer(bufferLabel, renderLabel);
    }

    void render(LevelRenderContext context, ThroughWallPositionSnapshot sources) {
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

    private void rebuildAndUpload(ThroughWallPositionSnapshot.Capture state) {
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
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int outlineColor;
            int fillColor;
            float edgeThickness;
            float boxInset;
            if (style == Style.HIDDEN_BLOCK) {
                LocalFeatureConfig local = LocalFeatureConfig.getInstance();
                outlineColor = HiddenBlockAnalyzerPalettePolicy.colorForDistance(
                        distance,
                        local.hiddenSurfaceTraceColorPreset,
                        local.hiddenSurfaceTraceOpacityPercent);
                fillColor = HiddenBlockAnalyzerPalettePolicy.fillColorForDistance(
                        distance,
                        local.hiddenSurfaceTraceColorPreset,
                        local.hiddenSurfaceTraceOpacityPercent);
                edgeThickness = HiddenBlockAnalyzerPalettePolicy.ANALYZER_EDGE_THICKNESS;
                boxInset = HiddenBlockAnalyzerPalettePolicy.ANALYZER_BOX_INSET;
            } else {
                outlineColor = LavaVisionPalettePolicy.colorForDistance(distance);
                fillColor = LavaVisionPalettePolicy.fillColorForDistance(distance);
                edgeThickness = LavaVisionPalettePolicy.ANALYZER_EDGE_THICKNESS;
                boxInset = LavaVisionPalettePolicy.ANALYZER_BOX_INSET;
            }

            ThroughWallWireBoxGeometry.drawFilledBox(
                    buffer,
                    x - anchorX,
                    y - anchorY,
                    z - anchorZ,
                    fillColor,
                    boxInset + edgeThickness);
            ThroughWallWireBoxGeometry.drawWireBox(
                    buffer,
                    x - anchorX,
                    y - anchorY,
                    z - anchorZ,
                    outlineColor,
                    edgeThickness,
                    boxInset);
        }

        MeshData builtBuffer = buffer.buildOrThrow();
        try {
            retainedBuffer.upload(THROUGH_WALL_PIPELINE, builtBuffer, anchorX, anchorY, anchorZ);
        } finally {
            builtBuffer.close();
        }
    }

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
