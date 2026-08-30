package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrosshairInspectorArchitectureContractTest {
    private static final Path GUI = Path.of("src/main/java/dev/chise/chisetweaks/gui");

    @Test
    void crosshairOwnsExistingHitLifecycleAndCacheButNotDomainDerivation() throws Exception {
        String crosshair = Files.readString(GUI.resolve("CrosshairInspector.java"));
        String screen = Files.readString(GUI.resolve("ChiseTweaksConfigScreen.java"));

        assertTrue(crosshair.contains("client.hitResult"));
        assertTrue(crosshair.contains("cachedBlockState == state"));
        assertTrue(crosshair.contains("cachedFilterRevision == filterRevision"));
        assertTrue(crosshair.contains("CrosshairSnapshotPolicy.blockSnapshot("));
        assertTrue(crosshair.contains("PlacementInspector.placementProbe("));
        assertTrue(screen.contains("inspector.refresh(minecraft)"));

        for (String misplaced : new String[]{
                "BlockPlaceContext", "BlockStateProperties", "BlockInspectionPolicy",
                "VisualTargetSelectionPolicy", "TrapDoorBlock", "SlabBlock", "StairBlock",
                "GlassHighlightTargetPolicy", "OreHighlightResolver.resolve("}) {
            assertFalse(crosshair.contains(misplaced),
                    () -> "CrosshairInspector reacquired extracted responsibility: " + misplaced);
        }
        assertFalse(crosshair.contains(".clip("));
        assertFalse(crosshair.contains("raycast("));
        assertFalse(crosshair.contains("pick("));
    }

    @Test
    void snapshotPolicyDerivesReadOnlyStateWithoutOwningMinecraftHitLifecycle() throws Exception {
        String policy = Files.readString(GUI.resolve("CrosshairSnapshotPolicy.java"));

        assertTrue(policy.contains("BlockInspectionPolicy.categories("));
        assertTrue(policy.contains("BuilderFocusVisibility.inspect("));
        assertTrue(policy.contains("responsibleFeatures("));
        assertTrue(policy.contains("formatStateProperties("));
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
    void helpSurfaceMigratesToInspectorWithoutRestoringLegacyUtilityChrome() throws Exception {
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
