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
        assertFalse(navigation.contains("OTHER("));
    }

    @Test
    void categoryPagesKeepFeaturesAndTheirTargetsTogether() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("case PLACEMENT -> createPlacementOptions()"));
        assertTrue(source.contains("case RESOURCES -> createResourceOptions()"));
        assertTrue(source.contains("case VISIBILITY -> createVisibilityOptions()"));

        assertTrue(source.contains("addFeatureToggle(rows, FeatureSwitches.PUMPKIN_SCAFFOLD)"));
        assertTrue(source.contains("addFeatureToggle(rows, FeatureSwitches.PLACEMENT_GUIDE)"));
        assertTrue(source.contains("visualTargetsFor(ChiseTweaksUiSection.PLACEMENT)"));

        assertTrue(source.contains("addFeatureToggle(rows, FeatureSwitches.MATERIAL_HIGHLIGHTS)"));
        assertTrue(source.contains("visualTargetsFor(ChiseTweaksUiSection.RESOURCES)"));

        assertTrue(source.contains("addFeatureToggle(rows, FeatureSwitches.HIDDEN_SURFACE_TRACE)"));
        assertTrue(source.contains("visualTargetsFor(ChiseTweaksUiSection.VISIBILITY)"));
        assertTrue(source.contains("addConfigs(rows, BuilderFocusConfig.RULE_OPTIONS)"));
        assertFalse(source.contains("addSpacer(rows)"));
    }

    @Test
    void categoryFeatureRowsAreBooleanOnlyAndHotkeyEditorsStayOnKeybindPage() throws IOException {
        String screen = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));
        String featureSwitch = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/config/FeatureSwitch.java"));

        assertTrue(screen.contains("feature.booleanGuiView()"));
        assertTrue(screen.contains("addFeatureHotkey(rows, feature)"));
        assertTrue(featureSwitch.contains("public IConfigBoolean booleanGuiView()"));
        assertTrue(featureSwitch.contains("return ConfigType.BOOLEAN"));
        assertTrue(featureSwitch.contains("FeatureSwitch.this.setBooleanValue(value)"));
        assertTrue(screen.contains("BooleanHotkeyGuiWrapper"));
    }

    @Test
    void categoryPagesExposeOneContextAwareBulkSelectionControl() throws IOException {
        String screen = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(screen.contains("BULK_BUTTON_WIDTH = 126"));
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
    void settingsGeometryIsResponsiveAndSearchDoesNotCompeteWithBulkControl() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("MAX_GROUP_WIDTH = 1080"));
        assertTrue(source.contains("setListPosition(groupX(), LIST_Y)"));
        assertTrue(source.contains("groupX() + getBrowserWidth() + BULK_GAP"));
        assertTrue(source.contains("return Math.max(160, groupWidth() - reserved);"));
        assertTrue(source.contains("return selectedSection == ChiseTweaksUiSection.HOTKEYS;"));
        assertTrue(source.contains("int preferred = selectedSection == ChiseTweaksUiSection.HOTKEYS ? 220 : 180;"));
    }

    @Test
    void localizedConfigRowsSetTranslatedGuiNamesInsteadOfOnlyPrettyNames() throws IOException {
        String localization = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/config/ConfigUiLocalization.java"));
        String localSettings = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java"));

        assertTrue(localization.contains("option.setTranslatedName(displayName)"));
        assertTrue(localization.contains("mirrorPrettyNamesToGui(LocalFeatureSettings.ALL_OPTIONS)"));
        assertTrue(localization.contains("mirrorPrettyNamesToGui(VisualTargetSettings.ALL_OPTIONS)"));
        assertTrue(localization.contains("option.setTranslatedName(option.getPrettyName())"));
        assertTrue(localSettings.contains("option.setTranslatedName(displayName)"));
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
