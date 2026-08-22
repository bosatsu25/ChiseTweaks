package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UI変更中の即時ディスク書込みを防ぎ、Applyを単一の永続化境界として固定する。 */
final class SettingsPersistenceBoundaryContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void localUiAdaptersMutateMemoryWithoutOwningPersistence() throws IOException {
        String localSettings = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targetSettings = source("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");
        String localSwitch = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitch.java");
        String controller = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");

        assertFalse(localSettings.contains(".save()"));
        assertFalse(targetSettings.contains(".save()"));
        assertFalse(localSwitch.contains("FeatureManager"));
        assertFalse(localSwitch.contains(".save()"));
        assertFalse(targetSettings.contains("refreshTranslations"));

        assertTrue(controller.contains("FeatureConfig.saveToFile()"));
        assertTrue(controller.contains("LocalFeatureConfig.getInstance().save()"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
