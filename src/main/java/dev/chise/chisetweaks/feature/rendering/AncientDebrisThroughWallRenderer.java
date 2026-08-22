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
import dev.chise.chisetweaks.core.policy.AncientDebrisAnalyzerPolicy;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

final class AncientDebrisThroughWallRenderer implements AutoCloseable {
    private static final RenderPipeline THROUGH_WALL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "chisetweaks", "pipeline/ancient_debris_analyzer_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final Matrix4fc IDENTITY_POSE = new Matrix4f();

    private final AncientDebrisSnapshot.Capture capture = new AncientDebrisSnapshot.Capture(
            AncientDebrisAnalyzerPolicy.MAX_MAX_MARKERS);
    private final Matrix4f dynamicModelView = new Matrix4f();

    private ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private MappableRingBuffer vertexBuffer;
    private GpuBuffer drawVertexBuffer;
    private int drawIndexCount;
    private int anchorX;
    private int anchorY;
    private int anchorZ;
    private long uploadedRevision = Long.MIN_VALUE;
    private boolean closed;

    void render(LevelRenderContext context, AncientDebrisSnapshot sources) {
        if (closed || context == null || sources == null || sources.isEmpty()) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        if (camera == null) return;

        if (sources.renderRevision() != uploadedRevision) {
            sources.captureInto(capture);
            if (capture.count() == 0) {
                drawIndexCount = 0;
                uploadedRevision = capture.revision();
                return;
            }
            rebuildAndUpload(capture);
            uploadedRevision = capture.revision();
        }

        if (drawVertexBuffer == null || drawIndexCount == 0) return;
        draw(Minecraft.getInstance(), THROUGH_WALL_PIPELINE, camera);
    }

    private void rebuildAndUpload(AncientDebrisSnapshot.Capture state) {
        long anchor = state.positionAt(0);
        anchorX = BlockPos.getX(anchor);
        anchorY = BlockPos.getY(anchor);
        anchorZ = BlockPos.getZ(anchor);

        BufferBuilder buffer = new BufferBuilder(
                allocator,
                THROUGH_WALL_PIPELINE.getVertexFormatMode(),
                THROUGH_WALL_PIPELINE.getVertexFormat());
        for (int index = 0; index < state.count(); index++) {
            long packed = state.positionAt(index);
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);
            double dx = x + 0.5 - state.eyeX();
            double dy = y + 0.5 - state.eyeY();
            double dz = z + 0.5 - state.eyeZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            drawWireBox(
                    IDENTITY_POSE,
                    buffer,
                    x - anchorX,
                    y - anchorY,
                    z - anchorZ,
                    AncientDebrisAnalyzerPolicy.colorForDistance(distance),
                    AncientDebrisAnalyzerPolicy.edgeThicknessForDistance(distance),
                    AncientDebrisAnalyzerPolicy.boxInsetForDistance(distance));
        }

        MeshData builtBuffer = buffer.buildOrThrow();
        try {
            MeshData.DrawState drawParameters = builtBuffer.drawState();
            VertexFormat format = drawParameters.format();
            drawVertexBuffer = upload(drawParameters, format, builtBuffer);
            drawIndexCount = drawParameters.indexCount();
        } finally {
            builtBuffer.close();
        }
    }

    private GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(
                    () -> "ChiseTweaks Ancient Debris Analyzer retained buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    vertexBufferSize);
            drawVertexBuffer = null;
        }

        GpuBuffer uploadTarget = vertexBuffer.currentBuffer();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
                uploadTarget.slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }
        vertexBuffer.rotate();
        return uploadTarget;
    }

    private void draw(Minecraft client, RenderPipeline pipeline, Vec3 camera) {
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
                        () -> "ChiseTweaks Ancient Debris Analyzer retained rendering",
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

    void resetAfterFailure() {
        if (closed) return;
        drawVertexBuffer = null;
        drawIndexCount = 0;
        uploadedRevision = Long.MIN_VALUE;
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        allocator.close();
        allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    }

    private static void drawWireBox(
            Matrix4fc pose,
            BufferBuilder vertices,
            int blockX,
            int blockY,
            int blockZ,
            int argb,
            float thickness,
            float inset) {
        float minX = blockX + inset;
        float minY = blockY + inset;
        float minZ = blockZ + inset;
        float maxX = blockX + 1.0f - inset;
        float maxY = blockY + 1.0f - inset;
        float maxZ = blockZ + 1.0f - inset;
        float half = thickness * 0.5f;

        bar(vertices, pose, minX, minY - half, minZ - half, maxX, minY + half, minZ + half, argb);
        bar(vertices, pose, minX, minY - half, maxZ - half, maxX, minY + half, maxZ + half, argb);
        bar(vertices, pose, minX, maxY - half, minZ - half, maxX, maxY + half, minZ + half, argb);
        bar(vertices, pose, minX, maxY - half, maxZ - half, maxX, maxY + half, maxZ + half, argb);
        bar(vertices, pose, minX - half, minY, minZ - half, minX + half, maxY, minZ + half, argb);
        bar(vertices, pose, maxX - half, minY, minZ - half, maxX + half, maxY, minZ + half, argb);
        bar(vertices, pose, minX - half, minY, maxZ - half, minX + half, maxY, maxZ + half, argb);
        bar(vertices, pose, maxX - half, minY, maxZ - half, maxX + half, maxY, maxZ + half, argb);
        bar(vertices, pose, minX - half, minY - half, minZ, minX + half, minY + half, maxZ, argb);
        bar(vertices, pose, maxX - half, minY - half, minZ, maxX + half, minY + half, maxZ, argb);
        bar(vertices, pose, minX - half, maxY - half, minZ, minX + half, maxY + half, maxZ, argb);
        bar(vertices, pose, maxX - half, maxY - half, minZ, maxX + half, maxY + half, maxZ, argb);
    }

    private static void bar(
            BufferBuilder vertices,
            Matrix4fc pose,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            int argb) {
        float red = ((argb >>> 16) & 0xFF) / 255.0f;
        float green = ((argb >>> 8) & 0xFF) / 255.0f;
        float blue = (argb & 0xFF) / 255.0f;
        float alpha = ((argb >>> 24) & 0xFF) / 255.0f;

        vertex(vertices, pose, minX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, maxY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, maxY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, minY, minZ, red, green, blue, alpha);
        vertex(vertices, pose, maxX, minY, maxZ, red, green, blue, alpha);
        vertex(vertices, pose, minX, minY, maxZ, red, green, blue, alpha);
    }

    private static void vertex(
            BufferBuilder vertices,
            Matrix4fc pose,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha) {
        vertices.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
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
        uploadedRevision = Long.MIN_VALUE;
    }
}
