package dev.chise.chisetweaks.feature.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import net.minecraft.core.BlockPos;

/** Placement-state-specific line geometry separated from generic surface primitives. */
final class PlacementGuideLineGeometry {
    private static final float INSET = SurfaceLinePrimitives.INSET;

    private PlacementGuideLineGeometry() {}

    static void draw(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            String blockId,
            OrientationOverlayPolicy.Overlay overlay,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        String id = blockId == null ? "" : blockId;
        OrientationOverlayPolicy.Overlay orientation =
                overlay == null ? OrientationOverlayPolicy.Overlay.EMPTY : overlay;

        if (id.endsWith("_slab")) {
            drawSlab(vertices, pose, position, orientation, primaryArgb, accentArgb, lineWidth);
            return;
        }
        if (id.endsWith("_stairs")) {
            drawStairs(vertices, pose, position, orientation, primaryArgb, accentArgb, lineWidth);
            return;
        }
        if (id.endsWith("_trapdoor")) {
            drawTrapdoor(vertices, pose, position, orientation, primaryArgb, accentArgb, lineWidth);
            return;
        }
        if (id.endsWith("_fence_gate")) {
            drawFenceGate(vertices, pose, position, orientation, primaryArgb, accentArgb, lineWidth);
            return;
        }
        if (id.endsWith("_froglight")
                || id.endsWith("_log")
                || id.endsWith("_wood")
                || id.endsWith("_stem")
                || id.endsWith("_hyphae")
                || id.endsWith("bamboo_block")) {
            drawAxisSkin(vertices, pose, position, orientation.axis(), primaryArgb, accentArgb, lineWidth);
            return;
        }

        SurfaceLinePrimitives.drawFaceFrame(vertices, pose, position, primaryArgb, lineWidth);
        drawDirectionGlyph(vertices, pose, position, orientation, accentArgb,
                Math.max(1.5f, lineWidth * 0.72f));
    }

    static void drawCompact(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos position,
            OrientationOverlayPolicy.Overlay overlay,
            int primaryArgb,
            int accentArgb,
            float lineWidth) {
        SurfaceLinePrimitives.drawFaceFrame(vertices, pose, position, primaryArgb, lineWidth);
        drawDirectionGlyph(
                vertices,
                pose,
                position,
                overlay == null ? OrientationOverlayPolicy.Overlay.EMPTY : overlay,
                accentArgb,
                Math.max(1.2f, lineWidth * 0.58f));
    }

    private static void drawSlab(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int primary,
            int accent,
            float width) {
        boolean top = overlay.half() == OrientationOverlayPolicy.Half.TOP
                || overlay.half() == OrientationOverlayPolicy.Half.UPPER;
        float minY = p.getY() + (top ? 0.50f : INSET);
        float maxY = p.getY() + (top ? 1.0f - INSET : 0.50f);
        SurfaceLinePrimitives.drawBounds(vertices, pose,
                p.getX() + INSET, minY, p.getZ() + INSET,
                p.getX() + 1.0f - INSET, maxY, p.getZ() + 1.0f - INSET,
                primary, width);
        float faceY = top ? minY + 0.008f : maxY - 0.008f;
        SurfaceLinePrimitives.drawHorizontalFaceCross(
                vertices, pose, p, faceY, accent, Math.max(1.2f, width * 0.60f));
    }

    private static void drawStairs(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int primary,
            int accent,
            float width) {
        boolean top = overlay.half() == OrientationOverlayPolicy.Half.TOP
                || overlay.half() == OrientationOverlayPolicy.Half.UPPER;
        float low = p.getY() + INSET;
        float mid = p.getY() + 0.50f;
        float high = p.getY() + 1.0f - INSET;
        if (top) {
            SurfaceLinePrimitives.drawBounds(vertices, pose,
                    p.getX() + INSET, mid, p.getZ() + INSET,
                    p.getX() + 1.0f - INSET, high, p.getZ() + 1.0f - INSET,
                    primary, width);
            drawDirectionalHalfBox(vertices, pose, p, overlay.facing(), low, mid, primary, width);
        } else {
            SurfaceLinePrimitives.drawBounds(vertices, pose,
                    p.getX() + INSET, low, p.getZ() + INSET,
                    p.getX() + 1.0f - INSET, mid, p.getZ() + 1.0f - INSET,
                    primary, width);
            drawDirectionalHalfBox(vertices, pose, p, overlay.facing(), mid, high, primary, width);
        }
        drawDirectionGlyph(vertices, pose, p, overlay, accent, Math.max(1.3f, width * 0.62f));
    }

    private static void drawDirectionalHalfBox(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Facing facing,
            float minY,
            float maxY,
            int argb,
            float width) {
        float minX = p.getX() + INSET;
        float maxX = p.getX() + 1.0f - INSET;
        float minZ = p.getZ() + INSET;
        float maxZ = p.getZ() + 1.0f - INSET;
        if (facing == OrientationOverlayPolicy.Facing.EAST) minX = p.getX() + 0.50f;
        else if (facing == OrientationOverlayPolicy.Facing.WEST) maxX = p.getX() + 0.50f;
        else if (facing == OrientationOverlayPolicy.Facing.SOUTH) minZ = p.getZ() + 0.50f;
        else if (facing == OrientationOverlayPolicy.Facing.NORTH) maxZ = p.getZ() + 0.50f;
        SurfaceLinePrimitives.drawBounds(
                vertices, pose, minX, minY, minZ, maxX, maxY, maxZ, argb, width);
    }

    private static void drawTrapdoor(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int primary,
            int accent,
            float width) {
        float thickness = 0.09f;
        if (Boolean.TRUE.equals(overlay.open())) {
            switch (overlay.facing() == null ? OrientationOverlayPolicy.Facing.NORTH : overlay.facing()) {
                case NORTH -> SurfaceLinePrimitives.drawBounds(vertices, pose,
                        p.getX() + INSET, p.getY() + INSET, p.getZ() + INSET,
                        p.getX() + 1.0f - INSET, p.getY() + 1.0f - INSET, p.getZ() + thickness,
                        primary, width);
                case SOUTH -> SurfaceLinePrimitives.drawBounds(vertices, pose,
                        p.getX() + INSET, p.getY() + INSET, p.getZ() + 1.0f - thickness,
                        p.getX() + 1.0f - INSET, p.getY() + 1.0f - INSET, p.getZ() + 1.0f - INSET,
                        primary, width);
                case WEST -> SurfaceLinePrimitives.drawBounds(vertices, pose,
                        p.getX() + INSET, p.getY() + INSET, p.getZ() + INSET,
                        p.getX() + thickness, p.getY() + 1.0f - INSET, p.getZ() + 1.0f - INSET,
                        primary, width);
                case EAST -> SurfaceLinePrimitives.drawBounds(vertices, pose,
                        p.getX() + 1.0f - thickness, p.getY() + INSET, p.getZ() + INSET,
                        p.getX() + 1.0f - INSET, p.getY() + 1.0f - INSET, p.getZ() + 1.0f - INSET,
                        primary, width);
                case UP, DOWN -> SurfaceLinePrimitives.drawFaceFrame(vertices, pose, p, primary, width);
            }
        } else {
            boolean top = overlay.half() == OrientationOverlayPolicy.Half.TOP
                    || overlay.half() == OrientationOverlayPolicy.Half.UPPER;
            float minY = p.getY() + (top ? 1.0f - thickness : INSET);
            float maxY = p.getY() + (top ? 1.0f - INSET : thickness);
            SurfaceLinePrimitives.drawBounds(vertices, pose,
                    p.getX() + INSET, minY, p.getZ() + INSET,
                    p.getX() + 1.0f - INSET, maxY, p.getZ() + 1.0f - INSET,
                    primary, width);
        }
        drawDirectionGlyph(vertices, pose, p, overlay, accent, Math.max(1.2f, width * 0.58f));
    }

    private static void drawFenceGate(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int primary,
            int accent,
            float width) {
        float y0 = p.getY() + 0.24f;
        float y1 = p.getY() + 0.76f;
        boolean eastWest = overlay.facing() == OrientationOverlayPolicy.Facing.EAST
                || overlay.facing() == OrientationOverlayPolicy.Facing.WEST;
        if (Boolean.TRUE.equals(overlay.open())) {
            float a = 0.16f;
            float b = 0.84f;
            if (eastWest) {
                SurfaceLinePrimitives.line(vertices, pose, p.getX() + 0.50f, y0, p.getZ() + a,
                        p.getX() + 0.50f, y1, p.getZ() + a, primary, width);
                SurfaceLinePrimitives.line(vertices, pose, p.getX() + 0.50f, y0, p.getZ() + b,
                        p.getX() + 0.50f, y1, p.getZ() + b, primary, width);
            } else {
                SurfaceLinePrimitives.line(vertices, pose, p.getX() + a, y0, p.getZ() + 0.50f,
                        p.getX() + a, y1, p.getZ() + 0.50f, primary, width);
                SurfaceLinePrimitives.line(vertices, pose, p.getX() + b, y0, p.getZ() + 0.50f,
                        p.getX() + b, y1, p.getZ() + 0.50f, primary, width);
            }
        } else if (eastWest) {
            drawFenceGateBar(vertices, pose, p, y0, true, primary, width);
            drawFenceGateBar(vertices, pose, p, y1, true, primary, width);
        } else {
            drawFenceGateBar(vertices, pose, p, y0, false, primary, width);
            drawFenceGateBar(vertices, pose, p, y1, false, primary, width);
        }
        drawDirectionGlyph(vertices, pose, p, overlay, accent, Math.max(1.2f, width * 0.58f));
    }

    private static void drawFenceGateBar(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            float y,
            boolean eastWest,
            int argb,
            float width) {
        if (eastWest) {
            SurfaceLinePrimitives.line(vertices, pose,
                    p.getX() + INSET, y, p.getZ() + 0.50f,
                    p.getX() + 1.0f - INSET, y, p.getZ() + 0.50f,
                    argb, width);
        } else {
            SurfaceLinePrimitives.line(vertices, pose,
                    p.getX() + 0.50f, y, p.getZ() + INSET,
                    p.getX() + 0.50f, y, p.getZ() + 1.0f - INSET,
                    argb, width);
        }
    }

    private static void drawAxisSkin(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Axis axis,
            int primary,
            int accent,
            float width) {
        SurfaceLinePrimitives.drawFaceFrame(vertices, pose, p, primary, width);
        float cx = p.getX() + 0.5f;
        float cy = p.getY() + 0.5f;
        float cz = p.getZ() + 0.5f;
        OrientationOverlayPolicy.Axis actual = axis == null ? OrientationOverlayPolicy.Axis.Y : axis;
        switch (actual) {
            case X -> {
                SurfaceLinePrimitives.line(vertices, pose,
                        p.getX() + INSET, cy, cz,
                        p.getX() + 1.0f - INSET, cy, cz,
                        accent, width);
                SurfaceLinePrimitives.line(vertices, pose,
                        p.getX() + INSET, cy - 0.16f, cz,
                        p.getX() + 1.0f - INSET, cy - 0.16f, cz,
                        accent, width * 0.62f);
                SurfaceLinePrimitives.line(vertices, pose,
                        p.getX() + INSET, cy + 0.16f, cz,
                        p.getX() + 1.0f - INSET, cy + 0.16f, cz,
                        accent, width * 0.62f);
            }
            case Y -> {
                SurfaceLinePrimitives.line(vertices, pose,
                        cx, p.getY() + INSET, cz,
                        cx, p.getY() + 1.0f - INSET, cz,
                        accent, width);
                SurfaceLinePrimitives.line(vertices, pose,
                        cx - 0.16f, p.getY() + INSET, cz,
                        cx - 0.16f, p.getY() + 1.0f - INSET, cz,
                        accent, width * 0.62f);
                SurfaceLinePrimitives.line(vertices, pose,
                        cx + 0.16f, p.getY() + INSET, cz,
                        cx + 0.16f, p.getY() + 1.0f - INSET, cz,
                        accent, width * 0.62f);
            }
            case Z -> {
                SurfaceLinePrimitives.line(vertices, pose,
                        cx, cy, p.getZ() + INSET,
                        cx, cy, p.getZ() + 1.0f - INSET,
                        accent, width);
                SurfaceLinePrimitives.line(vertices, pose,
                        cx, cy - 0.16f, p.getZ() + INSET,
                        cx, cy - 0.16f, p.getZ() + 1.0f - INSET,
                        accent, width * 0.62f);
                SurfaceLinePrimitives.line(vertices, pose,
                        cx, cy + 0.16f, p.getZ() + INSET,
                        cx, cy + 0.16f, p.getZ() + 1.0f - INSET,
                        accent, width * 0.62f);
            }
        }
    }

    private static void drawDirectionGlyph(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            BlockPos p,
            OrientationOverlayPolicy.Overlay overlay,
            int argb,
            float width) {
        float cx = p.getX() + 0.5f;
        float cy = p.getY() + 0.5f;
        float cz = p.getZ() + 0.5f;
        OrientationOverlayPolicy.Facing facing = overlay.facing();
        if (facing == null) return;
        float dx = 0.0f;
        float dz = 0.0f;
        if (facing == OrientationOverlayPolicy.Facing.EAST) dx = 0.34f;
        else if (facing == OrientationOverlayPolicy.Facing.WEST) dx = -0.34f;
        else if (facing == OrientationOverlayPolicy.Facing.SOUTH) dz = 0.34f;
        else if (facing == OrientationOverlayPolicy.Facing.NORTH) dz = -0.34f;
        else return;
        float ex = cx + dx;
        float ez = cz + dz;
        SurfaceLinePrimitives.line(vertices, pose, cx, cy, cz, ex, cy, ez, argb, width);
        if (Math.abs(dx) > 0.01f) {
            float back = Math.signum(dx) * 0.12f;
            SurfaceLinePrimitives.line(vertices, pose,
                    ex, cy, ez, ex - back, cy + 0.10f, ez, argb, width);
            SurfaceLinePrimitives.line(vertices, pose,
                    ex, cy, ez, ex - back, cy - 0.10f, ez, argb, width);
        } else {
            float back = Math.signum(dz) * 0.12f;
            SurfaceLinePrimitives.line(vertices, pose,
                    ex, cy, ez, ex, cy + 0.10f, ez - back, argb, width);
            SurfaceLinePrimitives.line(vertices, pose,
                    ex, cy, ez, ex, cy - 0.10f, ez - back, argb, width);
        }
    }
}
