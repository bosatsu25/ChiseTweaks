package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrosshairInspectorArchitectureContractTest {
    private static final Path GUI = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void crosshairOwnsExistingHitLifecycleAndMinimalBuilderCache() throws Exception {
        String crosshair = Files.readString(GUI.resolve("CrosshairInspector.java"));
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));

        assertTrue(crosshair.contains("client.hitResult"));
        assertTrue(crosshair.contains("cachedBlockState == state"));
        assertTrue(crosshair.contains("CrosshairSnapshotPolicy.blockSnapshot("));
        assertTrue(crosshair.contains("PlacementInspector.placementProbe("));
        assertTrue(screen.contains("inspector.refresh(minecraft)"));

        for (String removed : new String[]{
                "cachedFilterRevision", "cachedOreRevision", "cachedFeatureMask",
                "currentEnabledFeatureMask", "BuilderFocusVisibility", "OreHighlightResolver",
                "BlockInspectionPolicy", "VisualTargetSelectionPolicy",
                "GlassHighlightTargetPolicy", "responsibleFeatures"}) {
            assertFalse(crosshair.contains(removed), () -> "legacy crosshair analysis returned: " + removed);
        }
        assertFalse(crosshair.contains(".clip("));
        assertFalse(crosshair.contains("raycast("));
        assertFalse(crosshair.contains("pick("));
    }

    @Test
    void snapshotPolicyFormatsReadOnlyStateWithoutFeatureAnalysis() throws Exception {
        String policy = Files.readString(GUI.resolve("CrosshairSnapshotPolicy.java"));

        assertTrue(policy.contains("formatStateProperties("));
        assertTrue(policy.contains("stateProperties(state)"));
        for (String removed : new String[]{
                "BuilderFocusVisibility", "FeatureSwitches", "FeatureDefinition",
                "OreHighlightResolver", "BlockInspectionPolicy", "VisualTargetSelectionPolicy",
                "responsibleFeatures(", "enabledFeatureMask("}) {
            assertFalse(policy.contains(removed), () -> "legacy snapshot analysis returned: " + removed);
        }
        assertFalse(policy.contains("Minecraft"));
        assertFalse(policy.contains("client.hitResult"));
        assertFalse(policy.contains("BlockPlaceContext"));
    }

    @Test
    void inspectorCollaboratorsRemainReadOnlyAndPrivacyBounded() throws Exception {
        String combined = Files.readString(GUI.resolve("CrosshairInspector.java"))
                + Files.readString(GUI.resolve("CrosshairSnapshotPolicy.java"))
                + Files.readString(GUI.resolve("PlacementInspector.java"));

        for (String forbidden : new String[]{
                "setBlock(", "setValue(", "toggleBooleanValue(", "saveToFile(",
                "sendPacket", "getBlockEntity(", "getUpdateTag(", "saveWith",
                "getNbt", "getComponents(", "getUUID(", "getInventory", "getContainer",
                "getMessage", "getText", "chat"}) {
            assertFalse(combined.contains(forbidden), forbidden);
        }
        assertFalse(combined.contains("LOGGER"));
    }

    @Test
    void helpSurfaceRemainsInsideBuilderAssistWithoutLegacyUtilityChrome() throws Exception {
        String controller = Files.readString(GUI.resolve("ChiseTweaksSettingsController.java"));
        String inspectorRows = Files.readString(GUI.resolve("InspectorSettingsRows.java"));
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));

        assertTrue(controller.contains("INSPECTOR"));
        assertFalse(controller.contains("HELP"));
        assertFalse(controller.contains("VISUAL_FILTER"));
        assertTrue(inspectorRows.contains("addCommonHelpRows"));
        assertTrue(screen.contains("inspectorHelpVisible"));
        assertFalse(screen.contains("ChiseTweaksMetadata.MOD_VERSION"));
        assertFalse(screen.contains("Diagnostics"));
        assertFalse(screen.contains("Resource reload"));
    }
}
