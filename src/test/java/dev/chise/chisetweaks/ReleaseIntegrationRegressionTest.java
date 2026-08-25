package dev.chise.chisetweaks;

import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.FeatureSwitches;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Headless integration contracts for the retained runtime acceptance matrix. */
final class ReleaseIntegrationRegressionTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void allTwelveRuntimeFeaturesCanRemainEnabledAtTheSameTime() {
        List<ChiseBooleanSetting> switches = new ArrayList<>(FeatureSwitches.VALUES);
        assertEquals(12, switches.size());

        boolean[] original = new boolean[switches.size()];
        for (int index = 0; index < switches.size(); index++) {
            original[index] = switches.get(index).getBooleanValue();
        }
        try {
            switches.forEach(setting -> setting.setBooleanValueSilently(true));
            assertTrue(switches.stream().allMatch(ChiseBooleanSetting::getBooleanValue));

            for (int index = 0; index < switches.size(); index++) {
                ChiseBooleanSetting selected = switches.get(index);
                selected.setBooleanValueSilently(false);
                assertFalse(selected.getBooleanValue(), selected.getName());
                for (int other = 0; other < switches.size(); other++) {
                    if (other == index) continue;
                    ChiseBooleanSetting otherSetting = switches.get(other);
                    assertTrue(otherSetting.getBooleanValue(),
                            () -> otherSetting.getName() + " was coupled to " + selected.getName());
                }
                selected.setBooleanValueSilently(true);
            }
        } finally {
            for (int index = 0; index < switches.size(); index++) {
                switches.get(index).setBooleanValueSilently(original[index]);
            }
        }
    }

    @Test
    void joinDisconnectAndDimensionSensitiveFeaturesHaveExplicitResetPaths() throws IOException {
        String session = source("src/main/java/dev/chise/chisetweaks/runtime/ClientSessionState.java");
        String manager = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java");
        String worksite = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteVisibilityEngine.java");
        String lava = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");

        assertTrue(session.contains("public static void onJoin"));
        assertTrue(session.contains("public static void onDisconnect"));
        assertTrue(session.contains("FeatureManager.getInstance().resetSessionState(client)"));
        assertTrue(manager.contains("resetSessionState"));
        assertTrue(worksite.contains("resetSession(Minecraft client)"));
        assertTrue(lava.contains("resetSession(Minecraft client)"));
    }

    @Test
    void visualFilterAndVisibilityFeaturesDoNotReintroduceCrossToggleCallbacks() throws IOException {
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        assertFalse(bindings.contains("WORKSITE_VISIBILITY_TOGGLES"));
        assertFalse(bindings.contains("bindExclusiveWorksiteMode"));
        assertFalse(bindings.contains("setWorksiteVisibilityModeChangedCallback"));
        assertFalse(bindings.contains("setBooleanValueSilently(false)"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
