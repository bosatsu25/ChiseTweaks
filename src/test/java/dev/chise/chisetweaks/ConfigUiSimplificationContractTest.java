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
    void mainSettingsScreenIsChiseOwnedInsteadOfAStretchedMalilibConfigList() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");

        assertTrue(screen.contains("extends Screen"));
        assertTrue(screen.contains("GuiGraphicsExtractor"));
        assertTrue(screen.contains("EditBox"));
        assertTrue(screen.contains("ChiseTweaksSettingsLayout.calculate"));
        assertTrue(screen.contains("設定をリセット"));
        assertTrue(screen.contains("適用"));
        assertTrue(screen.contains("完了"));
        assertFalse(screen.contains("extends GuiConfigsBase"));
        assertFalse(screen.contains("BooleanHotkeyGuiWrapper"));
    }

    @Test
    void categoryPagesExposeCompactFeatureTargetAndDescriptionRows() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");

        assertTrue(screen.contains("細線トレース"));
        assertTrue(screen.contains("見えにくいブロックの対象"));
        assertTrue(screen.contains("表示を絞る対象"));
        assertTrue(screen.contains("溶岩・視認の詳細設定"));
        assertTrue(screen.contains("視認スキャン範囲"));
        assertTrue(screen.contains("スキャン間隔"));
        assertTrue(screen.contains("toggleMessage(config)"));
        assertTrue(screen.contains("geometry.controlWidth()"));
    }

    @Test
    void categorySearchAndBulkSelectionShareOneRowWithoutKeySearchControls() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");

        assertTrue(screen.contains("searchBox.setResponder"));
        assertTrue(screen.contains("一括選択："));
        assertTrue(screen.contains("geometry.search()"));
        assertTrue(screen.contains("geometry.bulk()"));
        assertTrue(screen.contains("toggleBulk()"));
        assertFalse(screen.contains("NONE"));
        assertFalse(screen.contains("Solo選択"));
        assertFalse(screen.contains("SOLO"));
    }

    @Test
    void fullHotkeyEditorIsIsolatedBehindTheKeybindDestination() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String hotkeys = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksHotkeyScreen.java");

        assertTrue(screen.contains("new ChiseTweaksHotkeyScreen(this)"));
        assertTrue(screen.contains("キー割り当てを編集する"));
        assertTrue(hotkeys.contains("extends GuiConfigsBase"));
        assertTrue(hotkeys.contains("BooleanHotkeyGuiWrapper"));
        assertTrue(hotkeys.contains("return true;"));
    }

    @Test
    void responsiveGeometryHasAHeadlessPolicyAndDedicatedAutomatedTests() throws IOException {
        String layout = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsLayout.java");
        String test = read("src/test/java/dev/chise/chisetweaks/ChiseTweaksSettingsLayoutTest.java");

        assertTrue(layout.contains("MAX_CONTENT_WIDTH = 1180"));
        assertTrue(layout.contains("BULK_WIDTH = 134"));
        assertTrue(layout.contains("public static Geometry calculate"));
        assertTrue(layout.contains("public boolean overlaps"));
        assertTrue(test.contains("640, 360"));
        assertTrue(test.contains("854, 480"));
        assertTrue(test.contains("2560, 1440"));
        assertTrue(test.contains("categoryLayoutNeverOverlapsSearchBulkPanelOrFooter"));
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
        assertFalse(engine.contains("hudPresenter"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
