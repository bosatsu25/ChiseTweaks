package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the design boundaries introduced by the settings/rendering cleanup. */
final class SettingsAndAnalyzerDesignContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void localSettingsUseConfigAsSingleInMemorySourceOfTruth() throws IOException {
        String settings = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targets = source("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");

        assertTrue(settings.contains("UI-facing settings bound directly to"));
        assertTrue(settings.contains("() -> config().worksiteVisibilityHorizontalRadius"));
        assertTrue(settings.contains("value -> config().lavaAnalyzerIntervalTicks = value"));
        assertFalse(settings.contains("syncFromStorage"));
        assertFalse(settings.contains("private static boolean syncing"));
        assertFalse(targets.contains("syncFromConfig"));
        assertFalse(targets.contains("private static boolean syncing"));
        assertTrue(targets.contains("VisualTargetSelectionPolicy.withEnabled("));
    }

    @Test
    void configLoadingDoesNotReachIntoRenderingState() throws IOException {
        String config = source("src/main/java/dev/chise/chisetweaks/config/FeatureConfig.java");
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");

        assertFalse(config.contains("feature.rendering"));
        assertFalse(config.contains("BuilderFocusVisibility"));
        assertTrue(bindings.contains("BuilderFocusVisibility.applyConfig()"));
    }

    @Test
    void analyzerFeaturesShareOnlyGenericRetentionAndRenderingInfrastructure() throws IOException {
        String lava = source("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String debris = source("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String renderer = source("src/main/java/dev/chise/chisetweaks/feature/rendering/ThroughWallMarkerRenderer.java");

        assertTrue(lava.contains("ThroughWallMarkerRenderer.Style.LAVA_SOURCE"));
        assertTrue(debris.contains("ThroughWallMarkerRenderer.Style.ANCIENT_DEBRIS"));
        assertTrue(lava.contains("new NearestPositionBuffer("));
        assertTrue(debris.contains("new NearestPositionBuffer("));
        assertTrue(renderer.contains("enum Style"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaAnalyzerThroughWallRenderer.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisThroughWallRenderer.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaSourceSnapshot.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisSnapshot.java")));
    }

    @Test
    void settingsPresentationIsSeparatedAndLocalizedByMinecraftResources() throws IOException {
        String controller = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String catalog = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsCatalog.java");
        String screen = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        String sceneFilter = source("src/main/java/dev/chise/chisetweaks/gui/ChiseSceneFilterEditorScreen.java");
        String oreCompat = source("src/main/java/dev/chise/chisetweaks/gui/ChiseOreCompatibilityScreen.java");
        String english = source("src/main/resources/assets/chisetweaks/lang/en_us.json");
        String japanese = source("src/main/resources/assets/chisetweaks/lang/ja_jp.json");

        assertTrue(controller.contains("ChiseTweaksSettingsCatalog"));
        assertFalse(controller.contains("boolean japanese"));
        assertFalse(controller.contains("japanese ?"));
        assertTrue(catalog.contains("Component.translatable("));
        assertTrue(catalog.contains("definition.nameKey()"));
        assertFalse(screen.contains("controller.japanese()"));
        assertFalse(sceneFilter.contains("boolean japanese"));
        assertFalse(oreCompat.contains("boolean japanese"));
        assertTrue(english.contains("\"screen.chisetweaks.settings.title.highlight\""));
        assertTrue(japanese.contains("\"screen.chisetweaks.settings.title.highlight\""));
        assertTrue(english.contains("\"screen.chisetweaks.scene_filter.title\""));
        assertTrue(japanese.contains("\"screen.chisetweaks.ore_compat.title\""));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
