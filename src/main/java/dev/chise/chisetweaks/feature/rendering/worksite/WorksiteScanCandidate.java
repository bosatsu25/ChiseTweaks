package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import net.minecraft.world.level.block.state.BlockState;

 






final class WorksiteScanCandidate {
    private int x;
    private int y;
    private int z;
    private BlockState state;
    private String blockId;
    private BlockInspectionCategory category;
    private VisualAssistanceStylePolicy.OverlayStyle style;
    private double distanceSquared;

    void assign(
            int x,
            int y,
            int z,
            BlockState state,
            String blockId,
            BlockInspectionCategory category,
            VisualAssistanceStylePolicy.OverlayStyle style,
            double distanceSquared) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.state = state;
        this.blockId = blockId;
        this.category = category;
        this.style = style;
        this.distanceSquared = distanceSquared;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    int z() {
        return z;
    }

    BlockState state() {
        return state;
    }

    String blockId() {
        return blockId;
    }

    BlockInspectionCategory category() {
        return category;
    }

    VisualAssistanceStylePolicy.OverlayStyle style() {
        return style;
    }

    double distanceSquared() {
        return distanceSquared;
    }
}
