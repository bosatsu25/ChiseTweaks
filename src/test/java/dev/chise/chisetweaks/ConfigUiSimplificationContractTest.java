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
        String navigation = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksUiSection.java"));
        String screen = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(navigation.contains("PLACEMENT(\"Placement & Direction\", \"設置・向き\")"));
        assertTrue(navigation.contains("RESOURCES(\"Resources\", \"資源\")"));
        assertTrue(navigation.contains("VISIBILITY(\"Visibility\", \"見やすさ\")"));
        assertTrue(navigation.contains("HOTKEYS(\"Keybinds\", \"キー設定\")"));
        assertTrue(navigation.contains("HELP(\"Guide\", \"使い方\")"));
        assertTrue(screen.contains("for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values())"));

        assertFalse(screen.contains("ConfigGuiTab"));
        assertFalse(screen.contains("TargetListCategory"));
        assertFalse(screen.contains("FEATURES(\"Features & Keybinds\")"));
        assertFalse(screen.contains("LISTS(\"Lists\")"));
        assertFalse(navigation.contains("OTHER("));
    }

    @Test
    void categoryPagesKeepFeaturesAndTheirTargetsTogether() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("case PLACEMENT -> createPlacementOptions()"));
        assertTrue(source.contains("case RESOURCES -> createResourceOptions()"));
        assertTrue(source.contains("case VISIBILITY -> createVisibilityOptions()"));

        assertTrue(source.contains("addFeature(rows, FeatureSwitches.PUMPKIN_SCAFFOLD)"));
        assertTrue(source.contains("addFeature(rows, FeatureSwitches.PLACEMENT_GUIDE)"));
        assertTrue(source.contains("visualTargetsFor(ChiseTweaksUiSection.PLACEMENT)"));

        assertTrue(source.contains("addFeature(rows, FeatureSwitches.MATERIAL_HIGHLIGHTS)"));
        assertTrue(source.contains("visualTargetsFor(ChiseTweaksUiSection.RESOURCES)"));

        assertTrue(source.contains("addFeature(rows, FeatureSwitches.HIDDEN_SURFACE_TRACE)"));
        assertTrue(source.contains("visualTargetsFor(ChiseTweaksUiSection.VISIBILITY)"));
        assertTrue(source.contains("addConfigs(rows, BuilderFocusConfig.RULE_OPTIONS)"));
    }

    @Test
    void categoryPagesExposeOneContextAwareBulkSelectionControl() throws IOException {
        String screen = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(screen.contains("createBulkToggleButton"));
        assertTrue(screen.contains("一括選択："));
        assertTrue(screen.contains("boolean turnOn = !areAllCategoryTargetsEnabled"));
        assertTrue(screen.contains("setAllCategoryTargets(section, turnOn)"));
        assertTrue(screen.contains("VisualTargetSettings.setAllOreHighlightTargets(enabled)"));

        assertFalse(screen.contains("ALL_ON"));
        assertFalse(screen.contains("ALL_OFF"));
        assertFalse(screen.contains("対象 全ON"));
        assertFalse(screen.contains("対象 全OFF"));
        assertFalse(screen.contains("SOLO"));
        assertFalse(screen.contains("Solo選択"));
    }

    @Test
    void keybindsAreAFirstClassTopLevelDestination() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("case HOTKEYS -> createHotkeyOptions()"));
        assertTrue(source.contains("Feature keybinds"));
        assertTrue(source.contains("機能のキー設定"));
        assertTrue(source.contains("BooleanHotkeyGuiWrapper"));
        assertTrue(source.contains("for (FeatureSwitch feature : FeatureSwitches.VALUES)"));
    }

    @Test
    void guideUsesTheSameNavigationAndAddsSearch() throws IOException {
        String help = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksHelpScreen.java"));

        assertTrue(help.contains("createNavigation()"));
        assertTrue(help.contains("for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values())"));
        assertTrue(help.contains("new ChiseTweaksConfigScreen(section)"));
        assertTrue(help.contains("EditBox"));
        assertTrue(help.contains("filteredEntries()"));
        assertTrue(help.contains("searchable.contains(query)"));
    }

    @Test
    void searchRemainsAvailableAcrossSettingsDestinations() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("return selectedSection != ChiseTweaksUiSection.HELP;"));
        assertTrue(source.contains("super(10, 76"));
    }

    @Test
    void oreHighlightRowsRepresentResourceFamiliesInsteadOfStoneVariants() throws IOException {
        String targets = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java"));

        assertTrue(targets.contains("normal ore and its deepslate variant share one switch"));
        assertTrue(targets.contains("visualTargetMaterialCoalOre"));
        assertTrue(targets.contains("visualTargetMaterialIronOre"));
        assertTrue(targets.contains("visualTargetMaterialCopperOre"));
        assertTrue(targets.contains("visualTargetMaterialGoldOre"));
        assertTrue(targets.contains("visualTargetMaterialLapisOre"));
        assertTrue(targets.contains("visualTargetMaterialRedstoneOre"));
        assertTrue(targets.contains("visualTargetMaterialDiamondOre"));
        assertTrue(targets.contains("visualTargetMaterialEmeraldOre"));
        assertTrue(targets.contains("ネザー資源：古代の残骸"));
        assertTrue(targets.contains("特殊資材：黒曜石"));
        assertTrue(targets.contains("特殊資材：泣く黒曜石"));
        assertFalse(targets.contains("visualTargetMaterialDeepslate"));
        assertFalse(targets.contains("鉱石：深層"));
    }

    @Test
    void worksiteVisibilityUsesWorldOverlayWithoutTheRemovedHudPath() throws IOException {
        Path worksite = ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite");
        assertFalse(Files.exists(worksite.resolve("WorksiteHudPresenter.java")));
        assertFalse(Files.exists(worksite.resolve("WorksiteTargetInspector.java")));
        assertFalse(Files.exists(worksite.resolve("WorksiteTargetInspection.java")));

        String engine = Files.readString(worksite.resolve("WorksiteVisibilityEngine.java"));
        assertTrue(engine.contains("WorksiteOverlayRenderer"));
        assertTrue(engine.contains("overlayRenderer.updateTargets(targets)"));
        assertFalse(engine.contains("hudPresenter"));
        assertFalse(engine.contains("targetInspector"));
    }
}
