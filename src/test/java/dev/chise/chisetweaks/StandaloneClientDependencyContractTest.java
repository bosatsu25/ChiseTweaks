package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StandaloneClientDependencyContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path MAIN = ROOT.resolve("src/main/java");

    @Test
    void fabricMetadataHasNoExternalConfigEntrypointOrDependency() throws IOException {
        String descriptor = Files.readString(
                ROOT.resolve("src/main/resources/fabric.mod.json"), StandardCharsets.UTF_8);
        String lower = descriptor.toLowerCase(Locale.ROOT);

        assertTrue(descriptor.contains("\"environment\": \"client\""));
        assertTrue(descriptor.contains("dev.chise.chisetweaks.ChiseTweaksClient"));
        assertTrue(descriptor.contains("\"serverInstallationRequired\": false"));
        assertTrue(descriptor.contains("\"settingsOwnership\": \"standalone\""));
        assertTrue(descriptor.contains("\"externalConfigLibraryRequired\": false"));
        assertTrue(descriptor.contains("\"modMenuRequired\": false"));
        assertFalse(lower.contains("malilib"));
        assertFalse(lower.contains("litematica"));
        assertFalse(descriptor.contains("\"main\""));
        assertFalse(descriptor.contains("\"modmenu\""));
        assertFalse(descriptor.contains("\"modmenu\":"));
    }

    @Test
    void buildHasNoExternalSettingsUiLibraries() throws IOException {
        String build = Files.readString(ROOT.resolve("build.gradle"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);
        String properties = Files.readString(ROOT.resolve("gradle.properties"), StandardCharsets.UTF_8)
                .toLowerCase(Locale.ROOT);

        for (String forbidden : List.of("malilib", "modmenu", "litematica", "minihud", "tweakeroo")) {
            assertFalse(build.contains(forbidden), forbidden + " in build.gradle");
            assertFalse(properties.contains(forbidden), forbidden + " in gradle.properties");
        }
    }

    @Test
    void productionJavaHasNoExternalSettingsOrMasaFamilyReferences() throws IOException {
        List<String> failures = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path path : files.filter(value -> value.getFileName().toString().endsWith(".java")).toList()) {
                String text = Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                for (String forbidden : List.of(
                        "fi.dy.masa", "malilib", "litematica", "modmenu", "minihud", "tweakeroo")) {
                    if (text.contains(forbidden)) failures.add(ROOT.relativize(path) + " -> " + forbidden);
                }
            }
        }
        assertTrue(failures.isEmpty(), () -> "external settings/UI coupling remains: " + failures);
    }

    @Test
    void standaloneUiHasVanillaOptionsLauncherWithoutFixedKeyChord() throws IOException {
        Path screen = MAIN.resolve("dev/chise/chisetweaks/gui/ChiseTweaksConfigScreen.java");
        Path launcher = MAIN.resolve("dev/chise/chisetweaks/gui/ChiseTweaksScreenLauncher.java");
        assertTrue(Files.isRegularFile(screen));
        assertTrue(Files.isRegularFile(launcher));

        String client = Files.readString(
                MAIN.resolve("dev/chise/chisetweaks/ChiseTweaksClient.java"), StandardCharsets.UTF_8);
        String launcherSource = Files.readString(launcher, StandardCharsets.UTF_8);

        assertTrue(client.contains("ChiseTweaksScreenLauncher::register"));
        assertTrue(launcherSource.contains("ScreenEvents.AFTER_INIT"));
        assertTrue(launcherSource.contains("OptionsScreen"));
        assertTrue(launcherSource.contains("Screens.getButtons(screen)"));
        assertTrue(launcherSource.contains("new ChiseTweaksConfigScreen()"));
        assertTrue(launcherSource.contains("settings.setParent(parent)"));

        assertFalse(client.contains("KeyMapping"));
        assertFalse(client.contains("C+T"));
        assertFalse(client.contains("GLFW_KEY_C"));
        assertFalse(client.contains("GLFW_KEY_T"));
    }
}
