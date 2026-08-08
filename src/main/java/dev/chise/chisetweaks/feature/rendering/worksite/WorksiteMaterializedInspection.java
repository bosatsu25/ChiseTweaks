package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;

record WorksiteMaterializedInspection(
        BlockInspectionPolicy.InspectionPresentation presentation,
        OrientationOverlayPolicy.Overlay orientation) {}
