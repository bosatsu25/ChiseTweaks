package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MaLiLibConfigSelectorRegistrationContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void chiseRegistersCompatibleMaLiLibFallbackWhileModMenuUsesCustomSettings() throws IOException {
        String descriptor = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));
        String mainInitializer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/ChiseTweaks.java"));
        String clientInitializer = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/ChiseTweaksClient.java"));
        String bootstrap = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/ClientFeatureBootstrap.java"));
        String modMenu = Files.readString(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/compat/ChiseTweaksModMenu.java"));

        assertTrue(descriptor.contains("\"environment\": \"client\""));
        assertTrue(descriptor.contains("\"main\""));
        assertTrue(descriptor.contains("dev.chise.chisetweaks.ChiseTweaks"));
        assertTrue(descriptor.contains("\"serverInstallationRequired\": false"));

        assertTrue(mainInitializer.contains("implements ModInitializer"));
        assertTrue(mainInitializer.contains("InitializationHandler.getInstance()"));
        assertTrue(mainInitializer.contains(
                ".registerInitializationHandler(new ClientFeatureBootstrap())"));
        assertFalse(clientInitializer.contains("registerInitializationHandler"));

        assertTrue(bootstrap.contains("ConfigManager.getInstance().registerConfigHandler"));
        assertTrue(bootstrap.contains("Registry.CONFIG_SCREEN.registerConfigScreenFactory"));
        assertTrue(bootstrap.contains("new ModInfo("));
        assertTrue(bootstrap.contains("ChiseTweaksHotkeyScreen::new"));
        assertFalse(bootstrap.contains("ChiseTweaksConfigScreen::new"));

        assertTrue(modMenu.contains("new ChiseTweaksConfigScreen()"));
        assertTrue(modMenu.contains("settings.setParent(parent)"));
    }
}
