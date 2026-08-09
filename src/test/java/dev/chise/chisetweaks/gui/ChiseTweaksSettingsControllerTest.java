package dev.chise.chisetweaks.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source-level content contract for the standalone Chise settings controller. */
final class ChiseTweaksSettingsControllerTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void visibilityScreenKeepsTheApprovedGroupAndControlOrder() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String body = between(controller, "private void addVisibilityRows", "private void header(");

        assertOrdered(body,
                "\"header.visibility\"",
                "\"thread\"",
                "\"hidden\"",
                "\"glass\"",
                "\"header.hiddenTargets\"",
                "visibilityTargets",
                "\"header.sceneFilter\"",
                "\"focusBlocks\"",
                "\"focusBlocksEdit\"",
                "\"focusEntities\"",
                "\"focusEntitiesEdit\"",
                "\"header.visibilityDetails\"",
                "\"lava\"",
                "\"scanRange\"",
                "\"scanInterval\"");

        assertTrue(body.contains("\"見やすさ\""));
        assertTrue(body.contains("\"見えにくいブロックの対象\""));
        assertTrue(body.contains("\"表示を絞る対象\""));
        assertTrue(body.contains("\"溶岩源・視認の詳細設定\""));
        assertTrue(body.contains("\"溶岩源ガイド\""));
        assertTrue(body.contains("\"細線トレース\""));
        assertTrue(body.contains("\"隠面トレース\""));
        assertTrue(body.contains("\"ガラス検査\""));
        assertTrue(body.contains("\"視認スキャン範囲\""));
        assertTrue(body.contains("\"スキャン間隔\""));
        assertTrue(body.contains("EDIT_BLOCK_FILTER"));
        assertTrue(body.contains("EDIT_ENTITY_FILTER"));
    }

    @Test
    void visualTargetFamiliesKeepTheirApprovedCountsAndHiddenOrder() throws IOException {
        String targets = read("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");

        assertEquals(11, occurrences(targets, "entry(Target.PLACEMENT_"));
        assertEquals(13, occurrences(targets, "entry(Target.MATERIAL_"));
        assertEquals(4, occurrences(targets, "entry(Target.HIDDEN_"));
        assertOrdered(targets,
                "Target.HIDDEN_BLUE_ICE",
                "Target.HIDDEN_DEAD_CORAL",
                "Target.HIDDEN_POWDER_SNOW",
                "Target.HIDDEN_SCULK_CATALYST");
    }

    @Test
    void placementAndResourcePagesKeepTheirApprovedSectionLabels() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String placement = between(controller, "private void addPlacementRows", "private void addResourceRows");
        String resources = between(controller, "private void addResourceRows", "private void addVisibilityRows");

        assertOrdered(placement,
                "\"header.placement\"",
                "\"pumpkin\"",
                "\"pumpkinRange\"",
                "\"placementGuide\"",
                "\"header.placementTargets\"",
                "placementTargets");
        assertTrue(placement.contains("\"設置・向き\""));
        assertTrue(placement.contains("\"設置方向ガイドの対象\""));

        assertOrdered(resources,
                "\"header.resources\"",
                "\"materials\"",
                "\"nether\"",
                "\"header.vanillaOreTargets\"",
                "resourceTargets",
                "\"header.specialMaterialTargets\"",
                "resourceTargets");
        assertTrue(resources.contains("\"資源\""));
        assertTrue(resources.contains("\"鉱石・資源ハイライト\""));
        assertTrue(resources.contains("\"バニラ鉱石の対象\""));
        assertTrue(resources.contains("\"特殊資材\""));
        assertTrue(resources.contains("VanillaOreVisualCatalog.blockVariantCount()"));
        assertTrue(resources.contains("\"対象鉱石を発光枠で強調する\""));
        assertTrue(resources.contains("\"対象資材を発光枠で強調する\""));
        assertTrue(resources.contains("isSpecialMaterialTarget(option)"));
    }

    @Test
    void controllerHasNoKeybindPageOrSoloModeAndUsesExplicitSceneFilterActions() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String combined = (controller + "\n" + screen).toLowerCase();

        assertFalse(controller.contains("addHotkeyRows"));
        assertFalse(controller.contains("case HOTKEYS"));
        assertTrue(controller.contains("ChiseTweaksSettingRowDefinition.action"));
        assertTrue(screen.contains("ChiseSceneFilterEditorScreen"));
        assertFalse(screen.contains("ChiseTweaksHotkeyScreen"));
        assertFalse(screen.contains("BooleanHotkeyGuiWrapper"));
        assertFalse(combined.contains("solo選択"));
        assertFalse(combined.contains("case solo"));
    }

    @Test
    void visibilityResetRestoresEveryOwnedAdvancedSetting() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String reset = between(controller, "boolean resetSection", "void saveFeatureConfig");

        for (String token : new String[] {
                "BuilderFocusConfig.BLOCK_RULE_MODE.resetToDefault()",
                "BuilderFocusConfig.BLOCK_WHITELIST.resetToDefault()",
                "BuilderFocusConfig.BLOCK_BLACKLIST.resetToDefault()",
                "BuilderFocusConfig.ENTITY_RULE_MODE.resetToDefault()",
                "BuilderFocusConfig.ENTITY_WHITELIST.resetToDefault()",
                "BuilderFocusConfig.ENTITY_BLACKLIST.resetToDefault()",
                "LocalFeatureSettings.LAVA_SOURCE.resetToDefault()",
                "LocalFeatureSettings.LAVA_FLOWING.resetToDefault()",
                "LocalFeatureSettings.LAVA_SOURCE_COLOR.resetToDefault()",
                "LocalFeatureSettings.LAVA_FLOWING_COLOR.resetToDefault()",
                "LocalFeatureSettings.WORKSITE_VISIBILITY_VERTICAL_RADIUS.resetToDefault()",
                "LocalFeatureSettings.WORKSITE_VISIBILITY_MAX_OVERLAYS.resetToDefault()",
                "LocalFeatureSettings.WORKSITE_VISIBILITY_WORLD_OVERLAY.resetToDefault()",
                "LocalFeatureSettings.WORKSITE_VISIBILITY_EXCLUSIVE_MODE.resetToDefault()"
        }) {
            assertTrue(reset.contains(token), token);
        }
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, startToken);
        assertTrue(end > start, endToken);
        return source.substring(start, end);
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static void assertOrdered(String source, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int index = source.indexOf(token, previous + 1);
            assertTrue(index > previous, token);
            previous = index;
        }
    }
}
