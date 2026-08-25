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
    void localUiAdaptersAndRetainedRuntimeFeaturesDoNotOwnDiskPersistence() throws IOException {
        String localSettings = source("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSettings.java");
        String targetSettings = source("src/main/java/dev/chise/chisetweaks/config/VisualTargetSettings.java");
        String featureSwitch = source("src/main/java/dev/chise/chisetweaks/config/FeatureSwitch.java");
        String switches = source("src/main/java/dev/chise/chisetweaks/config/FeatureSwitches.java");
        String lava = source("src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");
        String featureContract = source("src/main/java/dev/chise/chisetweaks/feature/Feature.java");
        String controller = source("src/main/java/dev/chise/chisetweaks/gui/ChiseTweaksSettingsController.java");
        String persistence = source("src/main/java/dev/chise/chisetweaks/config/SettingPersistenceCoordinator.java");

        assertFalse(localSettings.contains(".save()"));
        assertFalse(targetSettings.contains(".save()"));
        assertFalse(featureSwitch.contains(".save()"));
        assertFalse(lava.contains(".save()"));
        assertFalse(featureSwitch.contains("FeatureManager"));
        assertFalse(featureSwitch.contains("setEnabled("));
        assertFalse(featureContract.contains("setEnabled("));
        assertFalse(targetSettings.contains("refreshTranslations"));

        assertTrue(localSettings.contains("SettingPersistence.LOCAL_CONFIG"));
        assertTrue(targetSettings.contains("SettingPersistence.LOCAL_CONFIG"));
        assertTrue(featureSwitch.contains("SettingPersistence persistence"));
        assertTrue(featureSwitch.contains("writer.accept(FeatureAvailabilityPolicy.isAvailable(definition) && requested)"));
        assertTrue(switches.contains("SettingPersistence.LOCAL_CONFIG"));
        assertTrue(switches.contains("config -> config.brightChestEnabled"));
        assertTrue(switches.contains("config -> config.brightConcreteEnabled"));

        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitch.java")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java")));
        assertFalse(controller.contains("FeatureConfig.saveToFile()"));
        assertFalse(controller.contains("LocalFeatureConfig.getInstance().save()"));
        assertTrue(controller.contains("SettingPersistenceCoordinator.production()"));
        assertTrue(controller.contains("return persistence.save(dirtyDomains);"));
        assertTrue(persistence.contains("FeatureConfig::saveToFile"));
        assertTrue(persistence.contains("LocalFeatureConfig.getInstance().save()"));
        assertTrue(persistence.contains("domain.isApplyManaged()"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
