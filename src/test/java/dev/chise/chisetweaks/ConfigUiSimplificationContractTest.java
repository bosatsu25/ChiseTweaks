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
    void topLevelNavigationUsesExactlyFourStandaloneSections() throws IOException {
        String navigation = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksUiSection.java");
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");

        assertTrue(navigation.contains("PLACEMENT(\"Placement & Direction\", \"設置・向き\")"));
        assertTrue(navigation.contains("RESOURCES(\"Resources\", \"資源\")"));
        assertTrue(navigation.contains("VISIBILITY(\"Visibility\", \"見やすさ\")"));
        assertTrue(navigation.contains("HELP(\"Guide\", \"使い方\")"));
        assertFalse(navigation.contains("HOTKEYS("));
        assertFalse(navigation.contains("OTHER("));
        assertTrue(screen.contains("for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values())"));
    }

    @Test
    void mainSettingsScreenIsChiseOwnedAndHasNoExternalSelectorOrHotkeyEditor() throws IOException {
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
        assertFalse(screen.contains("ChiseTweaks ▼"));
        assertFalse(screen.contains("ChiseTweaksHotkeyScreen"));
        assertFalse(screen.contains("GuiConfigsBase"));
        assertFalse(screen.contains("BooleanHotkeyGuiWrapper"));
        assertFalse(screen.contains("NONE"));
    }

    @Test
    void removedExternalUiIntegrationFilesStayRemoved() {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/compat/ChiseTweaksModMenu.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksHotkeyScreen.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/runtime/ClientInputHandler.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/ClientFeatureBootstrap.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/config/AbstractBooleanOption.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/config/ConfigUiLocalization.java")));
    }

    @Test
    void categoryContentLivesInControllerAndKeepsApprovedVisibilityGroups() throws IOException {
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
        assertFalse(controller.contains("addHotkeyRows"));
    }

    @Test
    void categorySearchAndBulkSelectionStayCompact() throws IOException {
        String screen = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String controller = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");

        assertTrue(screen.contains("searchBox.setResponder"));
        assertTrue(screen.contains("一括選択："));
        assertTrue(screen.contains("geometry.search()"));
        assertTrue(screen.contains("geometry.bulk()"));
        assertTrue(screen.contains("controller.toggleBulk(selectedSection)"));
        assertTrue(controller.contains("VisualTargetGroupPolicy.withAll"));
        assertFalse(screen.contains("Solo選択"));
        assertFalse(screen.contains("SOLO"));
    }

    @Test
    void responsiveGeometryHasHeadlessPolicyAndPinnedAutomatedTests() throws IOException {
        String layout = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsLayout.java");
        String test = read("src/test/java/dev/chise/chisetweaks/ChiseTweaksSettingsLayoutTest.java");

        assertTrue(layout.contains("MAX_CONTENT_WIDTH = 1180"));
        assertTrue(layout.contains("NAV_COUNT = 4"));
        assertTrue(layout.contains("Rect selector = Rect.EMPTY"));
        assertTrue(layout.contains("BULK_WIDTH = 134"));
        assertTrue(layout.contains("public static Geometry calculate"));
        assertTrue(test.contains("640, 360"));
        assertTrue(test.contains("854, 480"));
        assertTrue(test.contains("2560, 1440"));
        assertTrue(test.contains("approved854x480StandaloneGeometryIsPinnedAgainstUiRegression"));
    }

    @Test
    void guideUsesSameStandaloneNavigationAndAddsSearch() throws IOException {
        String help = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksHelpScreen.java");
        assertTrue(help.contains("createNavigation()"));
        assertTrue(help.contains("for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values())"));
        assertTrue(help.contains("new ChiseTweaksConfigScreen(section)"));
        assertTrue(help.contains("EditBox"));
        assertTrue(help.contains("filteredEntries()"));
    }

    @Test
    void settingRowsUseOnlyChiseOwnedSettingTypes() throws IOException {
        String row = read("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingRowDefinition.java");
        String local = read("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targets = read("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");

        assertTrue(row.contains("ChiseBooleanSetting"));
        assertTrue(row.contains("ChiseIntegerSetting"));
        assertTrue(local.contains("SimpleBooleanSetting"));
        assertTrue(local.contains("ChiseIntegerSetting"));
        assertTrue(targets.contains("SimpleBooleanSetting"));
        assertFalse(row.contains("fi.dy.masa"));
        assertFalse(local.contains("fi.dy.masa"));
        assertFalse(targets.contains("fi.dy.masa"));
    }

    @Test
    void oreHighlightRowsRepresentResourceFamiliesInsteadOfStoneVariants() throws IOException {
        String targets = read("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");
        assertTrue(targets.contains("Normal/deepslate ore variants intentionally share one"));
        assertTrue(targets.contains("visualTargetMaterialCoalOre"));
        assertTrue(targets.contains("visualTargetMaterialDiamondOre"));
        assertTrue(targets.contains("ネザー資源：古代の残骸"));
        assertTrue(targets.contains("特殊資材：黒曜石"));
        assertFalse(targets.contains("visualTargetMaterialDeepslate"));
    }

    @Test
    void worksiteVisibilityUsesWorldOverlayWithoutRemovedHudPath() throws IOException {
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
