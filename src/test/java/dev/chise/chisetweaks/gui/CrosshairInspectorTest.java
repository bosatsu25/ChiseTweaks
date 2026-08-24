package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.world.phys.HitResult;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrosshairInspectorTest {
    private static final BuilderFocusVisibility.FilterDecision VISIBLE =
            new BuilderFocusVisibility.FilterDecision(
                    false, BuilderFocusVisibility.REASON_FILTER_OFF, "");

    @Test
    void noTargetSnapshotContainsNoInventedTargetData() {
        var snapshot = CrosshairInspector.Snapshot.noTarget();

        assertEquals(HitResult.Type.MISS, snapshot.targetKind());
        assertTrue(snapshot.targetId().isEmpty());
        assertTrue(snapshot.stateProperties().isEmpty());
        assertEquals(null, snapshot.filterDecision());
        assertTrue(snapshot.responsibleFeatures().isEmpty());
    }

    @Test
    void blockAndEntitySnapshotsExposeOnlyTheirAllowedFields() {
        var block = new CrosshairInspector.Snapshot(
                HitResult.Type.BLOCK,
                "minecraft:oak_trapdoor",
                List.of("facing=north", "half=top", "open=false", "waterlogged=true"),
                VISIBLE,
                List.of(FeatureDefinition.NETHER_PALETTE),
                null,
                null,
                false);
        var entity = new CrosshairInspector.Snapshot(
                HitResult.Type.ENTITY,
                "minecraft:armor_stand",
                List.of(),
                VISIBLE,
                List.of(),
                null,
                null,
                false);

        assertEquals("minecraft:oak_trapdoor", block.targetId());
        assertEquals(List.of("facing=north", "half=top", "open=false", "waterlogged=true"),
                block.stateProperties());
        assertEquals("minecraft:armor_stand", entity.targetId());
        assertTrue(entity.stateProperties().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new CrosshairInspector.Snapshot(
                HitResult.Type.ENTITY,
                "minecraft:armor_stand",
                List.of("hidden=value"),
                VISIBLE,
                List.of(),
                null,
                null,
                false));
    }

    @Test
    void stateFormattingSortsKnownAndUnknownPropertiesWithoutAssumingTheirPresence() {
        assertEquals(
                List.of("custom_property=safe_value", "facing=north", "half=top", "open=false"),
                CrosshairInspector.formatStateProperties(Map.of(
                        "open", "false",
                        "half", "top",
                        "facing", "north",
                        "custom_property", "safe_value")));
        assertTrue(CrosshairInspector.formatStateProperties(Map.of()).isEmpty());
        assertTrue(CrosshairInspector.formatStateProperties(null).isEmpty());
        assertEquals(List.of("facing=north"), CrosshairInspector.formatStateProperties(Map.of(
                "facing", "north",
                "unsafe", "line\nbreak",
                "bad key", "value")));
    }

    @Test
    void allEnabledFeaturesRemainIndependentAndReportTheirActualRenderModes() {
        long all = CrosshairInspector.enabledFeatureMask(
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.KELP_HIGHLIGHT,
                FeatureDefinition.NETHER_PALETTE,
                FeatureDefinition.LAVA_HIGHLIGHT,
                FeatureDefinition.ANCIENT_DEBRIS_ANALYZER);
        Set<FeatureDefinition> observed = new HashSet<>();

        observed.addAll(features(impacts("minecraft:tripwire", null, false, false, false, all)));
        observed.addAll(features(impacts("minecraft:blue_ice", null, false, false, false, all)));
        observed.addAll(features(impacts(
                "minecraft:diamond_ore", Target.MATERIAL_DIAMOND_ORE, true, false, false, all)));
        observed.addAll(features(impacts("minecraft:glass", null, false, false, false, all)));
        observed.addAll(features(impacts("minecraft:kelp", null, false, false, false, all)));
        observed.addAll(features(impacts("minecraft:netherrack", null, false, false, false, all)));
        observed.addAll(features(impacts("minecraft:lava", null, false, true, false, all)));
        List<FeatureDefinition> ancient = impacts(
                "minecraft:ancient_debris", Target.MATERIAL_ANCIENT_DEBRIS, true, false, true, all);
        observed.addAll(features(ancient));

        assertEquals(Set.of(
                FeatureDefinition.FINE_THREAD_TRACE,
                FeatureDefinition.HIDDEN_SURFACE_TRACE,
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.GLASS_INSPECTION,
                FeatureDefinition.KELP_HIGHLIGHT,
                FeatureDefinition.NETHER_PALETTE,
                FeatureDefinition.LAVA_HIGHLIGHT,
                FeatureDefinition.ANCIENT_DEBRIS_ANALYZER), observed);
        assertTrue(ancient.contains(FeatureDefinition.MATERIAL_HIGHLIGHTS));
        assertTrue(ancient.contains(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER));
        assertEquals("screen.chisetweaks.inspector.render_mode.visible",
                ChiseTweaksSettingsCatalog.renderModeKey(
                        FeatureDefinition.MATERIAL_HIGHLIGHTS, false));
        assertEquals("screen.chisetweaks.inspector.render_mode.through_wall",
                ChiseTweaksSettingsCatalog.renderModeKey(
                        FeatureDefinition.ANCIENT_DEBRIS_ANALYZER, false));
    }

    @Test
    void filterHideSuppressesEveryOtherwiseResponsibleFeature() {
        long enabled = CrosshairInspector.enabledFeatureMask(
                FeatureDefinition.MATERIAL_HIGHLIGHTS,
                FeatureDefinition.ANCIENT_DEBRIS_ANALYZER);
        List<FeatureDefinition> impacts = CrosshairInspector.responsibleFeatures(
                "minecraft:ancient_debris",
                BlockInspectionPolicy.categories("minecraft:ancient_debris"),
                Target.MATERIAL_ANCIENT_DEBRIS,
                true,
                false,
                true,
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                true,
                enabled);

        assertFalse(impacts.isEmpty());
        assertEquals("screen.chisetweaks.inspector.render_mode.suppressed",
                ChiseTweaksSettingsCatalog.renderModeKey(impacts.get(0), true));
    }

    @Test
    void snapshotAndFilterRecordsHaveAPrivacyAllowlistByConstruction() {
        assertEquals(Set.of(
                        "targetKind", "targetId", "stateProperties", "filterDecision", "responsibleFeatures",
                        "predictedPlacement", "clickedFace", "upperClick"),
                recordComponents(CrosshairInspector.Snapshot.class));
        assertEquals(Set.of("hidden", "reason", "matchedRule"),
                recordComponents(BuilderFocusVisibility.FilterDecision.class));
    }

    private static List<FeatureDefinition> impacts(
            String id,
            Target oreTarget,
            boolean oreResolved,
            boolean sourceLava,
            boolean ancientAnalyzerTarget,
            long enabledFeatures) {
        return CrosshairInspector.responsibleFeatures(
                id,
                BlockInspectionPolicy.categories(id),
                oreTarget,
                oreResolved,
                sourceLava,
                ancientAnalyzerTarget,
                VisualTargetSelectionPolicy.ALL_TARGETS_MASK,
                true,
                enabledFeatures);
    }

    private static Set<FeatureDefinition> features(List<FeatureDefinition> features) {
        return Set.copyOf(features);
    }

    private static Set<String> recordComponents(Class<?> type) {
        Set<String> result = new HashSet<>();
        for (var component : type.getRecordComponents()) result.add(component.getName());
        return result;
    }
}
