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
    void configUiKeepsTheFeatureAndKeybindViewWhileReservingSpaceForTargetNavigation() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains(
                "FEATURES(\"Features & Keybinds\"), LISTS(\"Lists\"), HELP(\"Feature Guide\")"));
        assertTrue(source.contains("super(10, 100"));
        assertTrue(source.contains("case FEATURES -> 260"));
        assertTrue(source.contains("BooleanHotkeyGuiWrapper"));
        assertTrue(source.contains("return ConfigOptionWrapper.createFor(toggles);"));
        assertFalse(source.contains("HOTKEYS("));
        assertFalse(source.contains("ALL("));
        assertFalse(source.contains("createAllOptions"));
    }

    @Test
    void targetListsExposeFourTaskOrientedCategories() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("TargetListCategory"));
        assertTrue(source.contains("DECORATION(\"visualTargetPlacement\", \"Decoration\", \"装飾\")"));
        assertTrue(source.contains(
                "MINING_RESOURCES(\"visualTargetMaterial\", \"Mining / Resources\", \"採掘・資源\")"));
        assertTrue(source.contains(
                "VISUAL_SUPPORT(\"visualTargetHidden\", \"Visual Support\", \"視認支援\")"));
        assertTrue(source.contains("OTHER(\"\", \"Other\", \"その他\")"));
        assertTrue(source.contains("selectedTargetCategory.matches(option.getName())"));
    }

    @Test
    void targetListsKeepVisualTargetsAndSceneFilterRulesSeparated() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("case LISTS -> createTargetListOptions()"));
        assertTrue(source.contains("if (selectedTargetCategory == TargetListCategory.OTHER)"));
        assertTrue(source.contains("options.addAll(BuilderFocusConfig.RULE_OPTIONS)"));
        assertTrue(source.contains("for (IConfigBase option : VisualTargetSettings.ALL_OPTIONS)"));
        assertFalse(source.contains("options.addAll(LocalFeatureSettings.ALL_OPTIONS)"));
    }

    @Test
    void targetListsEnableSearchForTheCategoryFilteredRows() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains(
                "return selectedTab == ConfigGuiTab.FEATURES || selectedTab == ConfigGuiTab.LISTS;"));
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
    void miningCategoryProvidesOnlyBulkOreHighlightControls() throws IOException {
        String screen = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(screen.contains(
                "if (selectedTargetCategory == TargetListCategory.MINING_RESOURCES)"));
        assertTrue(screen.contains("TargetListAction"));
        assertTrue(screen.contains("ALL_ON"));
        assertTrue(screen.contains("ALL_OFF"));
        assertTrue(screen.contains("対象 全ON"));
        assertTrue(screen.contains("対象 全OFF"));
        assertTrue(screen.contains("VisualTargetSettings.setAllOreHighlightTargets(true)"));
        assertTrue(screen.contains("VisualTargetSettings.setAllOreHighlightTargets(false)"));

        assertFalse(screen.contains("SOLO"));
        assertFalse(screen.contains("Solo選択"));
        assertFalse(screen.contains("Solo:"));
        assertFalse(screen.contains("toggleSoloOreSelection"));
    }

    @Test
    void categoryChangesResetTransientTargetControlsBeforeRefreshingTheList() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("class CategoryButtonListener"));
        assertTrue(source.contains("VisualTargetSettings.resetTransientControls();\n            selectedTargetCategory = category;"));
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

    @Test
    void unifiedFeatureTabLabelsAndRemovedUiKeysStayStable() throws IOException {
        String english = Files.readString(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/lang/en_us.json"));
        String japanese = Files.readString(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/lang/ja_jp.json"));

        assertTrue(english.contains("\"gui.chisetweaks.tab.features\": \"Features & Keybinds\""));
        assertTrue(japanese.contains("\"gui.chisetweaks.tab.features\": \"機能・キー設定\""));

        for (String language : new String[] {english, japanese}) {
            assertFalse(language.contains("gui.chisetweaks.tab.all"));
            assertFalse(language.contains("gui.chisetweaks.tab.hotkeys"));
            assertFalse(language.contains("config.option.localworksitevisibilitymaxresults"));
        }
    }
}
