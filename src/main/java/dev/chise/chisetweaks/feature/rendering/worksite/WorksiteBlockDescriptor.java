package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;

import java.util.Map;
import java.util.Set;

record WorksiteBlockDescriptor(
        String id,
        Set<BlockInspectionCategory> categories,
        Map<BlockInspectionCategory, VisualAssistanceStylePolicy.OverlayStyle> styles) {

    VisualAssistanceStylePolicy.OverlayStyle styleFor(BlockInspectionCategory category) {
        return styles.getOrDefault(category, VisualAssistanceStylePolicy.OverlayStyle.NONE);
    }
}
