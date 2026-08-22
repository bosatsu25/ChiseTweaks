package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UIアダプタ自身の即時ディスク書込みを防ぎ、runtime安全判定と永続化責務を分離する。 */
final class SettingsPersistenceBoundaryContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void localUiAdaptersDoNotOwnDiskPersistence() throws IOException {
        String localSettings = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targetSettings = source("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");
        String localSwitch = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitch.java");
        String controller = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");

        assertFalse(localSettings.contains(".save()"));
        assertFalse(targetSettings.contains(".save()"));
        assertFalse(localSwitch.contains(".save()"));
        assertFalse(targetSettings.contains("refreshTranslations"));

        assertTrue(localSwitch.contains("FeatureManager.getInstance().getFeature"));
        assertTrue(localSwitch.contains("runtimeFeature.setEnabled(effectiveValue)"));
        assertTrue(controller.contains("FeatureConfig.saveToFile()"));
        assertTrue(controller.contains("LocalFeatureConfig.getInstance().save()"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
