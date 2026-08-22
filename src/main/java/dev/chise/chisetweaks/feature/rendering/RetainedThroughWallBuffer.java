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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/** 壁越し解析Rendererで共有する保持型GPUバッファのライフサイクルを管理する。 */
final class RetainedThroughWallBuffer implements AutoCloseable {
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

    RetainedThroughWallBuffer(String bufferLabel, String renderLabel) {
        this.bufferLabel = bufferLabel;
        this.renderLabel = renderLabel;
    }

    BufferBuilder newBufferBuilder(RenderPipeline pipeline) {
        ensureOpen();
        return new BufferBuilder(
                allocator,
                pipeline.getVertexFormatMode(),
                pipeline.getVertexFormat());
    }

    void upload(RenderPipeline pipeline, MeshData builtBuffer, int anchorX, int anchorY, int anchorZ) {
        ensureOpen();
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(
                    () -> bufferLabel,
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    vertexBufferSize);
            drawVertexBuffer = null;
        }

        GpuBuffer uploadTarget = vertexBuffer.currentBuffer();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
                uploadTarget.slice(0, builtBuffer.vertexBuffer().remaining()),
                false,
                true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }

        // このリビジョンを保持するバッファを確定してからリングを進め、変化のないフレームでは再アップロードしない。
        vertexBuffer.rotate();
        drawVertexBuffer = uploadTarget;
        drawIndexCount = drawParameters.indexCount();
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
    }

    boolean hasDrawData() {
        return !closed && drawVertexBuffer != null && drawIndexCount > 0;
    }

    void clearDrawData() {
        if (closed) return;
        drawVertexBuffer = null;
        drawIndexCount = 0;
    }

    void draw(Minecraft client, RenderPipeline pipeline, Vec3 camera) {
        if (!hasDrawData() || client == null || camera == null) return;

        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
                RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
        GpuBuffer indices = shapeIndexBuffer.getBuffer(drawIndexCount);
        VertexFormat.IndexType indexType = shapeIndexBuffer.type();

        dynamicModelView.set(RenderSystem.getModelViewMatrix());
        dynamicModelView.translate(
                (float) (anchorX - camera.x),
                (float) (anchorY - camera.y),
                (float) (anchorZ - camera.z));
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(dynamicModelView, COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> renderLabel,
                        client.getMainRenderTarget().getColorTextureView(),
                        OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(),
                        OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, drawVertexBuffer);
            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0, 0, drawIndexCount, 1);
        }
    }

    /** 失敗したフレームのCPU/GPU作業状態を破棄し、次回描画を新しいバッファから再開する。 */
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
        if (closed) throw new IllegalStateException("Retained through-wall buffer is closed");
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        allocator.close();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        drawVertexBuffer = null;
        drawIndexCount = 0;
    }
}
