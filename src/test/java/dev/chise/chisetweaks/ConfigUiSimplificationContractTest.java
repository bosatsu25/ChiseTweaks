package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigUiSimplificationContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void topLevelNavigationUsesExactlyFiveUserFacingSections() throws IOException {
        String navigation = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksUiSection.java");
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");

        assertTrue(navigation.contains("PLACEMENT(\"Placement & Direction\", \"設置・向き\")"));
        assertTrue(navigation.contains("RESOURCES(\"Resources\", \"資源\")"));
        assertTrue(navigation.contains("VISIBILITY(\"Visibility\", \"見やすさ\")"));
        assertTrue(navigation.contains("HOTKEYS(\"Keybinds\", \"キー設定\")"));
        assertTrue(navigation.contains("HELP(\"Guide\", \"使い方\")"));
        assertTrue(screen.contains("for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values())"));
        assertFalse(navigation.contains("OTHER("));
    }

    @Test
    void mainSettingsScreenIsAChiseOwnedViewWithDomainLogicDelegated() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");

        assertTrue(screen.contains("extends Screen"));
        assertTrue(screen.contains("GuiGraphicsExtractor"));
        assertTrue(screen.contains("EditBox"));
        assertTrue(screen.contains("ChiseTweaksSettingsLayout.calculate"));
        assertTrue(screen.contains("ChiseTweaksSettingsController"));
        assertTrue(controller.contains("rowsFor(ChiseTweaksUiSection section)"));
        assertTrue(controller.contains("toggleBulk(ChiseTweaksUiSection section)"));
        assertTrue(controller.contains("resetSection(ChiseTweaksUiSection section)"));
        assertFalse(screen.contains("extends GuiConfigsBase"));
        assertFalse(screen.contains("VisualTargetSettings.ALL_OPTIONS"));
        assertFalse(screen.contains("FeatureSwitches.MATERIAL_HIGHLIGHTS"));
    }

    @Test
    void categoryContentLivesInTheControllerAndKeepsApprovedVisibilityGroups() throws IOException {
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");

        assertTrue(controller.contains("case PLACEMENT -> addPlacementRows(rows)"));
        assertTrue(controller.contains("case RESOURCES -> addResourceRows(rows)"));
        assertTrue(controller.contains("case VISIBILITY -> addVisibilityRows(rows)"));
        assertTrue(controller.contains("細線トレース"));
        assertTrue(controller.contains("見えにくいブロックの対象"));
        assertTrue(controller.contains("表示を絞る対象"));
        assertTrue(controller.contains("溶岩・視認の詳細設定"));
        assertTrue(controller.contains("視認スキャン範囲"));
        assertTrue(controller.contains("スキャン間隔"));
    }

    @Test
    void categorySearchAndBulkSelectionStayCompactWithoutKeySearchControls() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");

        assertTrue(screen.contains("searchBox.setResponder"));
        assertTrue(screen.contains("一括選択："));
        assertTrue(screen.contains("geometry.search()"));
        assertTrue(screen.contains("geometry.bulk()"));
        assertTrue(screen.contains("controller.toggleBulk(selectedSection)"));
        assertTrue(controller.contains("VisualTargetGroupPolicy.withAll"));
        assertFalse(screen.contains("NONE"));
        assertFalse(screen.contains("Solo選択"));
        assertFalse(screen.contains("SOLO"));
    }

    @Test
    void fullHotkeyEditorIsIsolatedBehindTheKeybindDestination() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String hotkeys = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksHotkeyScreen.java");

        assertTrue(screen.contains("new ChiseTweaksHotkeyScreen(this)"));
        assertTrue(controller.contains("キー割り当てを編集する"));
        assertTrue(controller.contains("case HOTKEYS -> addHotkeyRows(rows)"));
        assertTrue(hotkeys.contains("extends GuiConfigsBase"));
        assertTrue(hotkeys.contains("BooleanHotkeyGuiWrapper"));
        assertTrue(hotkeys.contains("return true;"));
        assertFalse(screen.contains("BooleanHotkeyGuiWrapper"));
    }

    @Test
    void responsiveGeometryHasAHeadlessPolicyAndPinnedAutomatedTests() throws IOException {
        String layout = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsLayout.java");
        String test = read("src/test/java/dev/chise/chisetweaks/ChiseTweaksSettingsLayoutTest.java");

        assertTrue(layout.contains("MAX_CONTENT_WIDTH = 1180"));
        assertTrue(layout.contains("BULK_WIDTH = 134"));
        assertTrue(layout.contains("public static Geometry calculate"));
        assertTrue(layout.contains("public boolean overlaps"));
        assertTrue(test.contains("640, 360"));
        assertTrue(test.contains("854, 480"));
        assertTrue(test.contains("2560, 1440"));
        assertTrue(test.contains("approved854x480CategoryGeometryIsPinnedAgainstUiRegression"));
    }

    @Test
    void guideUsesTheSameNavigationAndAddsSearch() throws IOException {
        String help = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksHelpScreen.java");

        assertTrue(help.contains("createNavigation()"));
        assertTrue(help.contains("for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values())"));
        assertTrue(help.contains("new ChiseTweaksConfigScreen(section)"));
        assertTrue(help.contains("EditBox"));
        assertTrue(help.contains("filteredEntries()"));
    }

    @Test
    void localizedConfigRowsSetTranslatedGuiNamesInsteadOfOnlyPrettyNames() throws IOException {
        String localization = read("src/main/java/dev/chise/chisetweaks/config/ConfigUiLocalization.java");
        String localSettings = read("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");

        assertTrue(localization.contains("option.setTranslatedName(displayName)"));
        assertTrue(localization.contains("mirrorPrettyNamesToGui(LocalFeatureSettings.ALL_OPTIONS)"));
        assertTrue(localization.contains("mirrorPrettyNamesToGui(VisualTargetSettings.ALL_OPTIONS)"));
        assertTrue(localSettings.contains("option.setTranslatedName(displayName)"));
    }

    @Test
    void oreHighlightRowsRepresentResourceFamiliesInsteadOfStoneVariants() throws IOException {
        String targets = read("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");

        assertTrue(targets.contains("normal ore and its deepslate variant share one switch"));
        assertTrue(targets.contains("visualTargetMaterialCoalOre"));
        assertTrue(targets.contains("visualTargetMaterialDiamondOre"));
        assertTrue(targets.contains("ネザー資源：古代の残骸"));
        assertTrue(targets.contains("特殊資材：黒曜石"));
        assertFalse(targets.contains("visualTargetMaterialDeepslate"));
    }

    @Test
    void worksiteVisibilityUsesWorldOverlayWithoutTheRemovedHudPath() throws IOException {
        Path worksite = ROOT.resolve("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite");
        assertFalse(Files.exists(worksite.resolve("WorksiteHudPresenter.java")));
        assertFalse(Files.exists(worksite.resolve("WorksiteTargetInspector.java")));
        assertFalse(Files.exists(worksite.resolve("WorksiteTargetInspection.java")));

        String engine = Files.readString(worksite.resolve("WorksiteVisibilityEngine.java"));
        assertTrue(engine.contains("WorksiteOverlayRenderer"));
        assertTrue(engine.contains("overlayRenderer.updateTargets(targets)"));
        assertTrue(engine.contains("config.worksiteVisibilityWorldOverlay"));
        assertFalse(engine.contains("hudPresenter"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
