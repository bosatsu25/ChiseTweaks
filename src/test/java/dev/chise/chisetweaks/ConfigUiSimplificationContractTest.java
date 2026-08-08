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
    void configUiKeepsTheFormerHotkeyLayoutAsThePrimaryFeatureView() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains(
                "FEATURES(\"Features & Keybinds\"), LISTS(\"Lists\"), HELP(\"Feature Guide\")"));
        assertTrue(source.contains("case FEATURES -> 260"));
        assertTrue(source.contains("BooleanHotkeyGuiWrapper"));
        assertTrue(source.contains("createFeatureAndHotkeyOptions"));
        assertFalse(source.contains("HOTKEYS("));
        assertFalse(source.contains("ALL("));
        assertFalse(source.contains("createAllOptions"));
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
