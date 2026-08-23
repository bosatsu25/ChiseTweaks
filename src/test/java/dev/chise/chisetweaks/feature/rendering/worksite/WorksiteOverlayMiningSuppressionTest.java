package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorksiteOverlayMiningSuppressionTest {
    private static final BlockPos TARGET_POS = new BlockPos(2, 64, -3);
    private static final BlockPos OTHER_POS = new BlockPos(3, 64, -3);
    private static final Identifier NETHERRACK = Identifier.tryParse("minecraft:netherrack");
    private static final Identifier AIR = Identifier.tryParse("minecraft:air");

    @Test
    void staleCachedTargetStopsRenderingAsSoonAsTheLiveBlockChanges() {
        WorksiteRenderTarget target = target("minecraft:netherrack", BlockInspectionCategory.NETHER_PALETTE);

        assertFalse(WorksiteOverlayRenderer.shouldDrawTarget(target, AIR, false, null));
    }

    @Test
    void activelyMinedNetherTargetIsSuppressedBeforeTheNextVisibilityScan() {
        WorksiteRenderTarget target = target("minecraft:netherrack", BlockInspectionCategory.NETHER_PALETTE);

        assertFalse(WorksiteOverlayRenderer.shouldDrawTarget(target, NETHERRACK, true, TARGET_POS));
    }

    @Test
    void netherTargetStillRendersWhenOnlyObservedOrAnotherBlockIsBeingMined() {
        WorksiteRenderTarget target = target("minecraft:netherrack", BlockInspectionCategory.NETHER_PALETTE);

        assertTrue(WorksiteOverlayRenderer.shouldDrawTarget(target, NETHERRACK, false, TARGET_POS));
        assertTrue(WorksiteOverlayRenderer.shouldDrawTarget(target, NETHERRACK, true, OTHER_POS));
    }

    @Test
    void miningSuppressionDoesNotHideOtherInspectionCategories() {
        WorksiteRenderTarget target = target("minecraft:tripwire", BlockInspectionCategory.TECHNICAL_TRACE);
        Identifier tripwire = Identifier.tryParse("minecraft:tripwire");

        assertTrue(WorksiteOverlayRenderer.shouldDrawTarget(target, tripwire, true, TARGET_POS));
    }

    private static WorksiteRenderTarget target(String blockId, BlockInspectionCategory category) {
        var presentation = new BlockInspectionPolicy.InspectionPresentation(
                blockId,
                category,
                List.of(),
                category.argb());
        var style = new VisualAssistanceStylePolicy.OverlayStyle(
                category.argb(),
                100,
                VisualAssistanceStylePolicy.Marker.CROSS);
        return WorksiteRenderTarget.prepare(new WorksiteVisibleTarget(
                TARGET_POS,
                presentation,
                OrientationOverlayPolicy.Overlay.EMPTY,
                style,
                4.0));
    }
}
