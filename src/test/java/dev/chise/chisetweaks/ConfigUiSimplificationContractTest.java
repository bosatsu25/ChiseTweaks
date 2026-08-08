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
    void configUiKeepsFeatureAndHotkeyEditingInOneView() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java"));

        assertTrue(source.contains("FEATURES(\"Features\"), LISTS(\"Lists\"), HELP(\"Feature Guide\")"));
        assertTrue(source.contains("BooleanHotkeyGuiWrapper"));
        assertFalse(source.contains("HOTKEYS("));
        assertFalse(source.contains("ALL("));
        assertFalse(source.contains("createHotkeyOptions"));
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
    void removedUiAndHudKeysDoNotReturnToTranslations() throws IOException {
        for (String locale : new String[] {"en_us.json", "ja_jp.json"}) {
            String language = Files.readString(ROOT.resolve(
                    "src/main/resources/assets/chisetweaks/lang/" + locale));
            assertFalse(language.contains("gui.chisetweaks.tab.all"));
            assertFalse(language.contains("gui.chisetweaks.tab.hotkeys"));
            assertFalse(language.contains("config.option.localworksitevisibilitymaxresults"));
        }
    }
}
