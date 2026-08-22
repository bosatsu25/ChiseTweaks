package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UI作業コピー・runtime障害分離・ディスク永続化の責務境界を固定する。 */
final class SettingsPersistenceBoundaryContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void localUiAdaptersAndRuntimeFeaturesDoNotOwnDiskPersistence() throws IOException {
        String localSettings = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targetSettings = source("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");
        String localSwitch = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitch.java");
        String lava = source("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String debris = source("src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String featureContract = source("src/main/java/dev/chise/chisetweaks/feature/Feature.java");
        String controller = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");

        assertFalse(localSettings.contains(".save()"));
        assertFalse(targetSettings.contains(".save()"));
        assertFalse(localSwitch.contains(".save()"));
        assertFalse(lava.contains(".save()"));
        assertFalse(debris.contains(".save()"));
        assertFalse(localSwitch.contains("FeatureManager"));
        assertFalse(localSwitch.contains("setEnabled("));
        assertFalse(featureContract.contains("setEnabled("));
        assertFalse(targetSettings.contains("refreshTranslations"));

        assertTrue(localSwitch.contains("setter.accept(LocalFeatureConfig.getInstance(), effectiveValue)"));
        assertTrue(controller.contains("FeatureConfig.saveToFile()"));
        assertTrue(controller.contains("LocalFeatureConfig.getInstance().save()"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
