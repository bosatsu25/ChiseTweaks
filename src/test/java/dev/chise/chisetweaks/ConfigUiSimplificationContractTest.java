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
    void configUiUsesExactlyTheFormerHotkeyListAsThePrimaryFeatureView() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains(
                "FEATURES(\"Features & Keybinds\"), LISTS(\"Lists\"), HELP(\"Feature Guide\")"));
        assertTrue(source.contains("case FEATURES -> 260"));
        assertTrue(source.contains("BooleanHotkeyGuiWrapper"));
        assertTrue(source.contains("return ConfigOptionWrapper.createFor(toggles);"));
        assertFalse(source.contains("HOTKEYS("));
        assertFalse(source.contains("ALL("));
        assertFalse(source.contains("createAllOptions"));
    }

    @Test
    void targetListsOwnFineGrainedVisualTargetsWithoutDuplicatingParentFeatures() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("VisualTargetSettings.init()"));
        assertTrue(source.contains("case LISTS -> createTargetListOptions()"));
        assertTrue(source.contains("options.addAll(VisualTargetSettings.ALL_OPTIONS)"));
        assertTrue(source.contains("options.addAll(BuilderFocusConfig.RULE_OPTIONS)"));
        assertFalse(source.contains("options.addAll(LocalFeatureSettings.ALL_OPTIONS)"));
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
    void targetListsProvideSoloAndBulkOreHighlightControls() throws IOException {
        String screen = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));
        String targets = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java"));

        assertTrue(screen.contains("TargetListAction"));
        assertTrue(screen.contains("SOLO"));
        assertTrue(screen.contains("ALL_ON"));
        assertTrue(screen.contains("ALL_OFF"));
        assertTrue(screen.contains("Solo選択: "));
        assertTrue(screen.contains("対象 全ON"));
        assertTrue(screen.contains("対象 全OFF"));
        assertTrue(screen.contains("VisualTargetSettings.toggleSoloOreSelection()"));
        assertTrue(screen.contains("VisualTargetSettings.setAllOreHighlightTargets(true)"));
        assertTrue(screen.contains("VisualTargetSettings.setAllOreHighlightTargets(false)"));

        assertTrue(targets.contains("soloOreSelection"));
        assertTrue(targets.contains("withOnlyOreHighlightTarget"));
        assertTrue(targets.contains("withAllOreHighlightTargets"));
        assertTrue(targets.contains("if (soloOreSelection\n                && enabled"));
        assertTrue(targets.contains("toggleSoloOreSelection"));
        assertTrue(targets.contains("if (soloOreSelection) setAllOreHighlightTargets(false)"));
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
