package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import net.minecraft.core.BlockPos;

record WorksiteVisibleTarget(
        BlockPos position,
        BlockInspectionPolicy.InspectionPresentation presentation,
        OrientationOverlayPolicy.Overlay orientation,
        VisualAssistanceStylePolicy.OverlayStyle style,
        double distanceSquared) {}
