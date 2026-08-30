package dev.chise.chisetweaks.gui;

import net.minecraft.world.phys.HitResult;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrosshairInspectorTest {
    @Test
    void noTargetSnapshotContainsNoInventedTargetData() {
        var snapshot = CrosshairInspector.Snapshot.noTarget();
        assertEquals(HitResult.Type.MISS, snapshot.targetKind());
        assertTrue(snapshot.targetId().isEmpty());
        assertTrue(snapshot.stateProperties().isEmpty());
    }

    @Test
    void snapshotIsBoundedToBlockInfoAndPlacementAssist() {
        var block = new CrosshairInspector.Snapshot(
                HitResult.Type.BLOCK,
                "minecraft:oak_trapdoor",
                List.of("facing=north", "half=top", "open=false", "waterlogged=true"),
                null, null, PlacementInspector.NONE, null, false);
        var entity = new CrosshairInspector.Snapshot(
                HitResult.Type.ENTITY,
                "minecraft:armor_stand",
                List.of(),
                null, null, PlacementInspector.NONE, null, false);

        assertEquals("minecraft:oak_trapdoor", block.targetId());
        assertEquals(List.of("facing=north", "half=top", "open=false", "waterlogged=true"),
                block.stateProperties());
        assertEquals("minecraft:armor_stand", entity.targetId());
        assertTrue(entity.stateProperties().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new CrosshairInspector.Snapshot(
                HitResult.Type.ENTITY,
                "minecraft:armor_stand",
                List.of("hidden=value"),
                null, null, PlacementInspector.NONE, null, false));
    }

    @Test
    void stateFormattingRemainsSafeAndSemantic() {
        assertEquals(
                List.of("custom_property=safe_value", "facing=north", "half=top", "open=false"),
                CrosshairSnapshotPolicy.formatStateProperties(Map.of(
                        "open", "false",
                        "half", "top",
                        "facing", "north",
                        "custom_property", "safe_value")));
        assertEquals("orientation", CrosshairSnapshotPolicy.semanticPropertyGroup("facing"));
        assertEquals("shape", CrosshairSnapshotPolicy.semanticPropertyGroup("shape"));
        assertEquals("connection", CrosshairSnapshotPolicy.semanticPropertyGroup("north"));
        assertEquals("interaction", CrosshairSnapshotPolicy.semanticPropertyGroup("powered"));
        assertEquals("fluid", CrosshairSnapshotPolicy.semanticPropertyGroup("waterlogged"));
        assertEquals("other", CrosshairSnapshotPolicy.semanticPropertyGroup("modded_property"));
    }

    @Test
    void comparisonResultKeysCoverEveryBoundedState() {
        assertEquals("screen.chisetweaks.placement.result.match",
                BuilderAssistRows.comparisonResultKey(PlacementInspector.MATCH));
        assertEquals("screen.chisetweaks.placement.result.adjusted",
                BuilderAssistRows.comparisonResultKey(PlacementInspector.ADJUSTED));
        assertEquals("screen.chisetweaks.placement.result.different",
                BuilderAssistRows.comparisonResultKey(PlacementInspector.DIFFERENT));
        assertEquals("screen.chisetweaks.placement.result.unavailable",
                BuilderAssistRows.comparisonResultKey(PlacementInspector.UNAVAILABLE));
    }

    @Test
    void snapshotPrivacyAllowlistDropsDeveloperDiagnostics() {
        assertEquals(Set.of(
                        "targetKind", "targetId", "stateProperties",
                        "predictedPlacement", "actualPlacement", "placementResult",
                        "clickedFace", "upperClick"),
                recordComponents(CrosshairInspector.Snapshot.class));
    }

    private static Set<String> recordComponents(Class<?> type) {
        Set<String> result = new HashSet<>();
        for (var component : type.getRecordComponents()) result.add(component.getName());
        return result;
    }
}
