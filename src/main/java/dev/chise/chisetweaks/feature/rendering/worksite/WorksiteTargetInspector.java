package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

/** Inspects only the block currently targeted by the player. */
final class WorksiteTargetInspector {
    private final WorksiteBlockInspector blockInspector;

    WorksiteTargetInspector(WorksiteBlockInspector blockInspector) {
        this.blockInspector = blockInspector;
    }

    WorksiteTargetInspection inspect(
            Minecraft client,
            Set<BlockInspectionCategory> activeCategories) {
        if (!(client.hitResult instanceof BlockHitResult hit)) return null;

        BlockPos position = hit.getBlockPos();
        BlockState state = client.level.getBlockState(position);
        WorksiteBlockDescriptor descriptor = blockInspector.describe(state);
        BlockInspectionCategory category =
                blockInspector.resolveActiveCategory(descriptor, activeCategories);
        if (category == BlockInspectionCategory.NONE) return null;

        return new WorksiteTargetInspection(
                blockInspector.presentation(state, descriptor.id(), category));
    }
}
