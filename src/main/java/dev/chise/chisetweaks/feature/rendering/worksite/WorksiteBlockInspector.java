package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Converts Minecraft block state into stable Chise inspection data. */
final class WorksiteBlockInspector {
    private final Map<Block, WorksiteBlockDescriptor> descriptorCache = new IdentityHashMap<>();

    WorksiteBlockDescriptor describe(BlockState state) {
        Block block = state.getBlock();
        WorksiteBlockDescriptor cached = descriptorCache.get(block);
        if (cached != null) return cached;

        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        Set<BlockInspectionCategory> categories = BlockInspectionPolicy.categories(id);
        EnumMap<BlockInspectionCategory, VisualAssistanceStylePolicy.OverlayStyle> styles =
                new EnumMap<>(BlockInspectionCategory.class);
        for (BlockInspectionCategory category : categories) {
            styles.put(category, VisualAssistanceStylePolicy.styleFor(id, category));
        }
        WorksiteBlockDescriptor descriptor = new WorksiteBlockDescriptor(
                id, categories, Map.copyOf(styles));
        descriptorCache.put(block, descriptor);
        return descriptor;
    }

    BlockInspectionCategory resolveActiveCategory(
            WorksiteBlockDescriptor descriptor,
            Set<BlockInspectionCategory> activeCategories) {
        BlockInspectionCategory selected = BlockInspectionCategory.NONE;
        int selectedPriority = Integer.MIN_VALUE;
        for (BlockInspectionCategory category : descriptor.categories()) {
            if (!activeCategories.contains(category)) continue;
            int priority = descriptor.styleFor(category).priority();
            if (priority > selectedPriority) {
                selected = category;
                selectedPriority = priority;
            }
        }
        return selected;
    }

    BlockInspectionPolicy.InspectionPresentation presentation(
            BlockState state,
            String blockId,
            BlockInspectionCategory category) {
        return BlockInspectionPolicy.inspect(blockId, properties(state), category);
    }

    WorksiteMaterializedInspection materialize(
            BlockState state,
            String blockId,
            BlockInspectionCategory category) {
        Map<String, String> stateProperties = properties(state);
        return new WorksiteMaterializedInspection(
                BlockInspectionPolicy.inspect(blockId, stateProperties, category),
                OrientationOverlayPolicy.inspect(stateProperties));
    }

    private Map<String, String> properties(BlockState state) {
        HashMap<String, String> result = new HashMap<>(Math.max(4, state.getValues().size() * 2));
        for (var value : state.getValues()) {
            result.put(value.property().getName(), value.valueName());
        }
        return result;
    }
}
