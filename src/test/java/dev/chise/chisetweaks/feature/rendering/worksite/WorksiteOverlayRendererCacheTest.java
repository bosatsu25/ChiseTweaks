package dev.chise.chisetweaks.feature.rendering.worksite;

import dev.chise.chisetweaks.core.policy.OrientationOverlayPolicy;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualAssistanceStylePolicy;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorksiteOverlayRendererCacheTest {
    @Test
    void equalVisibleSnapshotsReusePreparedRenderTargets() throws Exception {
        WorksiteOverlayRenderer renderer = new WorksiteOverlayRenderer(() -> true);
        WorksiteVisibleTarget first = target(4.0);

        renderer.updateTargets(List.of(first));
        Object initialPrepared = preparedTargets(renderer);

        renderer.updateTargets(List.of(target(4.0)));
        assertSame(initialPrepared, preparedTargets(renderer));

        renderer.updateTargets(List.of(target(81.0)));
        assertNotSame(initialPrepared, preparedTargets(renderer));
    }

    @Test
    void clearDropsBothPreparedAndSourceSnapshots() throws Exception {
        WorksiteOverlayRenderer renderer = new WorksiteOverlayRenderer(() -> true);
        renderer.updateTargets(List.of(target(4.0)));
        renderer.clear();

        assertTrue(((List<?>) preparedTargets(renderer)).isEmpty());
        Field cached = WorksiteOverlayRenderer.class.getDeclaredField("cachedVisibleTargets");
        cached.setAccessible(true);
        assertTrue(((List<?>) cached.get(renderer)).isEmpty());
    }

    private static WorksiteVisibleTarget target(double distanceSquared) {
        var presentation = new BlockInspectionPolicy.InspectionPresentation(
                "minecraft:tripwire",
                BlockInspectionCategory.TECHNICAL_TRACE,
                List.of("powered=false", "north=true"),
                0xFFB29CFF);
        var style = new VisualAssistanceStylePolicy.OverlayStyle(
                0xCCB29CFF,
                100,
                VisualAssistanceStylePolicy.Marker.CROSS);
        return new WorksiteVisibleTarget(
                new BlockPos(1, 64, 1),
                presentation,
                OrientationOverlayPolicy.Overlay.EMPTY,
                style,
                distanceSquared);
    }

    private static Object preparedTargets(WorksiteOverlayRenderer renderer) throws Exception {
        Field field = WorksiteOverlayRenderer.class.getDeclaredField("targets");
        field.setAccessible(true);
        return field.get(renderer);
    }
}
