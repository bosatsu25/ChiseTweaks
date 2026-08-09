package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mutable pooled candidate used only while one bounded worksite scan is being assembled.
 *
 * <p>Instances are never exposed outside the scanner and are not mutated while present in the
 * priority queue. Reusing them removes candidate churn from repeated visibility scans without
 * leaking mutable state into render snapshots.</p>
 */
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
