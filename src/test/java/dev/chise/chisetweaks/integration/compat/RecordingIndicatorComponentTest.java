package dev.chise.chisetweaks.integration.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordingIndicatorComponentTest {
    @Test
    void formatsElapsedFieldsWithoutHeavyFormatterDependency() {
        assertEquals("00", RecordingIndicatorComponent.twoDigits(0));
        assertEquals("09", RecordingIndicatorComponent.twoDigits(9));
        assertEquals("10", RecordingIndicatorComponent.twoDigits(10));
        assertEquals("123", RecordingIndicatorComponent.twoDigits(123));
    }

    @Test
    void keepsFlashbackOptionalAndOutOfTheHudRenderHotPath() throws Exception {
        String component = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/integration/compat/RecordingIndicatorComponent.java"));
        String manager = Files.readString(Path.of(
                "src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java"));

        assertFalse(component.contains("import com.moulberry.flashback"));
        assertTrue(component.contains("Class.forName(\"com.moulberry.flashback.Flashback\""));
        assertTrue(component.contains("POLL_INTERVAL_TICKS = 5"));
        assertTrue(manager.contains("isModLoaded(\"flashback\")"));

        String renderPath = component.substring(component.indexOf("private void renderHud"));
        assertFalse(renderPath.contains("recorderPausedMethod.invoke"));
        assertFalse(renderPath.contains("recorderField.get(null)"));
    }
}
