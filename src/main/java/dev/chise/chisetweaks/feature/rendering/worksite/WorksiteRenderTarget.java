package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.performance.WorksiteOverlayDetailPolicy;
import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 毎フレームの描画で文字列や状態を再判定しないよう、描画専用情報を事前計算して保持する。
 */
record WorksiteRenderTarget(
        BlockPos position,
        BlockInspectionPolicy.InspectionPresentation presentation,
        OrientationOverlayPolicy.Overlay orientation,
        VisualAssistanceStylePolicy.OverlayStyle style,
        WorksiteOverlayDetailPolicy.Detail detail,
        int pulseSeed,
        boolean powered,
        boolean tripwireHook,
        Identifier expectedBlockId) {

    static WorksiteRenderTarget prepare(WorksiteVisibleTarget source) {
        List<String> details = source.presentation().details();
        String blockId = source.presentation().blockId();
        return new WorksiteRenderTarget(
                source.position(),
                source.presentation(),
                source.orientation(),
                source.style(),
                WorksiteOverlayDetailPolicy.detailFor(source.distanceSquared()),
                Math.floorMod(source.position().hashCode(), 8),
                details != null && details.contains("powered=true"),
                blockId != null && blockId.endsWith("tripwire_hook"),
                blockId == null ? null : Identifier.tryParse(blockId));
    }
}
