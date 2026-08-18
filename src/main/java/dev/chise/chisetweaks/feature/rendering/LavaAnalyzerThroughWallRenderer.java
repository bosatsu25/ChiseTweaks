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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.chise.chisetweaks.core.policy.LavaVisionPalettePolicy;
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

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Small dedicated GPU path for Lava Source Highlight cubes that remain visible through terrain.
 *
 * <p>The pipeline intentionally has no depth/stencil state. It is used only for already-retained,
 * nearby lava-source positions; it never reads world data, loads chunks, or expands the scan radius.
 * The geometry is a 12-edge wireframe built from narrow solid bars so it can use Minecraft's public
 * position-colour debug snippet without depending on renderer internals.</p>
 */
final class LavaAnalyzerThroughWallRenderer implements AutoCloseable {
    private static final RenderPipeline THROUGH_WALL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(
                            "chisetweaks", "pipeline/lava_analyzer_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build());

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final float BOX_INSET = 0.018f;

    private ByteBufferBuilder allocator = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private BufferBuilder buffer;
    private MappableRingBuffer vertexBuffer;
    private boolean closed;

    void render(LevelRenderContext context, List<BlockPos> sources) {
        if (closed || context == null || sources == null || sources.isEmpty()) return;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        if (camera == null) return;

        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        boolean geometryComplete = false;
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z);
            if (buffer == null) {
                buffer = new BufferBuilder(
                        allocator,
                        THROUGH_WALL_PIPELINE.getVertexFormatMode(),
                        THROUGH_WALL_PIPELINE.getVertexFormat());
            }

            Matrix4fc pose = matrices.last().pose();
            for (BlockPos source : sources) {
                double dx = source.getX() + 0.5 - camera.x;
                double dy = source.getY() + 0.5 - camera.y;
                double dz = source.getZ() + 0.5 - camera.z;
                int argb = LavaVisionPalettePolicy.colorForDistance(
                        Math.sqrt(dx * dx + dy * dy + dz * dz));
                drawWireBox(pose, buffer, source, argb, LavaVisionPalettePolicy.ANALYZER_EDGE_THICKNESS);
            }
            geometryComplete = true;
        } finally {
            matrices.popPose();
            if (!geometryComplete) buffer = null;
        }

        drawThroughWalls(Minecraft.getInstance(), THROUGH_WALL_PIPELINE);
    }

    private void drawThroughWalls(Minecraft client, RenderPipeline pipeline) {
        MeshData builtBuffer;
        try {
            builtBuffer = buffer.buildOrThrow();
        } finally {
            // Never reuse a builder after either a successful build or a build-time failure.
            buffer = null;
        }
        try {
            MeshData.DrawState drawParameters = builtBuffer.drawState();
            VertexFormat format = drawParameters.format();
            GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
            draw(client, pipeline, builtBuffer, drawParameters, vertices, format);
            vertexBuffer.rotate();
        } finally {
            builtBuffer.close();
        }
    }

    private GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(
                    () -> "ChiseTweaks Lava Source Highlight through-wall buffer",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
                vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()),
                false,
                true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }
        return vertexBuffer.currentBuffer();
    }

    private void draw(
            Minecraft client,
            RenderPipeline pipeline,
            MeshData builtBuffer,
            MeshData.DrawState drawParameters,
            GpuBuffer vertices,
            VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;
        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorting());
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = builtBuffer.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
                    RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "ChiseTweaks Lava Source Highlight through-wall rendering",
                        client.getMainRenderTarget().getColorTextureView(),
                        OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(),
                        OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }
    }

    /** Drops all transient CPU/GPU workspace so a later session never inherits a failed frame. */
    void resetAfterFailure() {
        if (closed) return;
        buffer = null;
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
            BlockPos position,
            int argb,
            float thickness) {
        float minX = position.getX() + BOX_INSET;
        float minY = position.getY() + BOX_INSET;
        float minZ = position.getZ() + BOX_INSET;
        float maxX = position.getX() + 1.0f - BOX_INSET;
        float maxY = position.getY() + 1.0f - BOX_INSET;
        float maxZ = position.getZ() + 1.0f - BOX_INSET;
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
        buffer = null;
    }
}
