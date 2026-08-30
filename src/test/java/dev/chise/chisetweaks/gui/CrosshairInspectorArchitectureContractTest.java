package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrosshairInspectorArchitectureContractTest {
    private static final Path GUI = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void crosshairOwnsHitLifecycleWithoutFeatureImpactDiagnostics() throws Exception {
        String crosshair = Files.readString(GUI.resolve("CrosshairInspector.java"));
        String policy = Files.readString(GUI.resolve("CrosshairSnapshotPolicy.java"));
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));

        assertTrue(crosshair.contains("client.hitResult"));
        assertTrue(crosshair.contains("cachedBlockState == state"));
        assertTrue(crosshair.contains("CrosshairSnapshotPolicy.blockSnapshot("));
        assertTrue(crosshair.contains("PlacementInspector.placementProbe("));
        assertTrue(screen.contains("crosshair.refresh(minecraft)"));

        for (String removed : new String[]{
                "currentEnabledFeatureMask", "cachedFeatureMask", "cachedFilterRevision",
                "cachedOreRevision", "responsibleFeatures", "filterDecision"}) {
            assertFalse(crosshair.contains(removed), removed);
            assertFalse(policy.contains(removed), removed);
        }
    }

    @Test
    void snapshotPolicyFormatsReadOnlyBlockInfoOnly() throws Exception {
        String policy = Files.readString(GUI.resolve("CrosshairSnapshotPolicy.java"));
        assertTrue(policy.contains("formatStateProperties("));
        assertTrue(policy.contains("BuiltInRegistries.BLOCK"));
        assertFalse(policy.contains("FeatureDefinition"));
        assertFalse(policy.contains("OreHighlightResolver"));
        assertFalse(policy.contains("BuilderFocusVisibility"));
        assertFalse(policy.contains("Minecraft"));
        assertFalse(policy.contains("BlockPlaceContext"));
    }

    @Test
    void builderAssistReplacesTheMonolithicInspectorSurface() throws Exception {
        String controller = Files.readString(GUI.resolve("ChiseTweaksSettingsController.java"));
        String assist = Files.readString(GUI.resolve("BuilderAssistRows.java"));
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));

        assertTrue(controller.contains("BUILDER_ASSIST"));
        assertFalse(controller.contains("INSPECTOR"));
        assertFalse(controller.contains("ANALYZER"));
        assertTrue(assist.contains("addPlacementRows"));
        assertTrue(assist.contains("addPatternConsistencyRows"));
        assertTrue(screen.contains("builderAssistHelpVisible"));
        assertFalse(Files.exists(GUI.resolve("InspectorSettingsRows.java")));
    }
}
