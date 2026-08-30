package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.policy.HiddenBlockAnalyzerPalettePolicy;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

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
    private final BlockPos.MutableBlockPos paletteCursor = new BlockPos.MutableBlockPos();
    private final ThroughWallPositionSnapshot.Capture capture;
    private final RetainedBuffer retainedBuffer;

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
        this.retainedBuffer = new RetainedBuffer(bufferLabel, renderLabel);
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
        LocalFeatureConfig hiddenConfig =
                style == Style.HIDDEN_BLOCK ? LocalFeatureConfig.getInstance() : null;
        Minecraft client = style == Style.HIDDEN_BLOCK ? Minecraft.getInstance() : null;
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
                String blockId = hiddenBlockIdAt(client, x, y, z);
                outlineColor = HiddenBlockAnalyzerPalettePolicy.colorForDistance(
                        distance,
                        blockId,
                        hiddenConfig.hiddenSurfaceTraceColorPreset,
                        hiddenConfig.hiddenSurfaceTraceOpacityPercent);
                fillColor = HiddenBlockAnalyzerPalettePolicy.fillColorForDistance(
                        distance,
                        blockId,
                        hiddenConfig.hiddenSurfaceTraceColorPreset,
                        hiddenConfig.hiddenSurfaceTraceOpacityPercent);
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

    private String hiddenBlockIdAt(Minecraft client, int x, int y, int z) {
        if (client == null || client.level == null) return "";
        LevelChunk sourceChunk = client.level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (sourceChunk == null) return "";
        paletteCursor.set(x, y, z);
        Identifier id = BuiltInRegistries.BLOCK.getKey(
                sourceChunk.getBlockState(paletteCursor).getBlock());
        return id == null ? "" : id.toString();
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
    /** Retained GPU upload/draw state owned by this renderer. */
    private static final class RetainedBuffer implements AutoCloseable {
        private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
        private static final Vector3f MODEL_OFFSET = new Vector3f();
        private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

        private final String bufferLabel;
        private final String renderLabel;
        private final Matrix4f dynamicModelView = new Matrix4f();

        private ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
        private MappableRingBuffer vertexBuffer;
        private GpuBuffer drawVertexBuffer;
        private int drawIndexCount;
        private int anchorX;
        private int anchorY;
        private int anchorZ;
        private boolean closed;

        RetainedBuffer(String bufferLabel, String renderLabel) {
            this.bufferLabel = bufferLabel;
            this.renderLabel = renderLabel;
        }

        BufferBuilder newBufferBuilder(RenderPipeline pipeline) {
            ensureOpen();
            return new BufferBuilder(allocator, pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        }

        void upload(RenderPipeline pipeline, MeshData builtBuffer, int x, int y, int z) {
            ensureOpen();
            MeshData.DrawState draw = builtBuffer.drawState();
            VertexFormat format = draw.format();
            int required = draw.vertexCount() * format.getVertexSize();
            if (vertexBuffer == null || vertexBuffer.size() < required) {
                if (vertexBuffer != null) vertexBuffer.close();
                vertexBuffer = new MappableRingBuffer(
                        () -> bufferLabel,
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                        required);
                drawVertexBuffer = null;
            }

            GpuBuffer uploadTarget = vertexBuffer.currentBuffer();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            try (GpuBuffer.MappedView mapped = encoder.mapBuffer(
                    uploadTarget.slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
                MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mapped.data());
            }
            vertexBuffer.rotate();
            drawVertexBuffer = uploadTarget;
            drawIndexCount = draw.indexCount();
            anchorX = x;
            anchorY = y;
            anchorZ = z;
        }

        void clearDrawData() {
            if (closed) return;
            drawVertexBuffer = null;
            drawIndexCount = 0;
        }

        void draw(Minecraft client, RenderPipeline pipeline, Vec3 camera) {
            if (closed || drawVertexBuffer == null || drawIndexCount <= 0 || client == null || camera == null) {
                return;
            }
            RenderSystem.AutoStorageIndexBuffer sequential =
                    RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            GpuBuffer indices = sequential.getBuffer(drawIndexCount);
            VertexFormat.IndexType indexType = sequential.type();

            dynamicModelView.set(RenderSystem.getModelViewMatrix());
            dynamicModelView.translate(
                    (float) (anchorX - camera.x),
                    (float) (anchorY - camera.y),
                    (float) (anchorZ - camera.z));
            GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                    .writeTransform(dynamicModelView, COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> renderLabel,
                    client.getMainRenderTarget().getColorTextureView(),
                    OptionalInt.empty(),
                    client.getMainRenderTarget().getDepthTextureView(),
                    OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.setVertexBuffer(0, drawVertexBuffer);
                pass.setIndexBuffer(indices, indexType);
                pass.drawIndexed(0, 0, drawIndexCount, 1);
            }
        }

        void resetAfterFailure() {
            if (closed) return;
            clearDrawData();
            if (vertexBuffer != null) {
                vertexBuffer.close();
                vertexBuffer = null;
            }
            allocator.close();
            allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
        }

        private void ensureOpen() {
            if (closed) throw new IllegalStateException("retained occluded-highlight buffer is closed");
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            allocator.close();
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = null;
            drawVertexBuffer = null;
            drawIndexCount = 0;
        }
    }

}
