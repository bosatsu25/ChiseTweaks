package dev.chise.chisetweaks.integration.compat;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Flashback録画中だけ右上へ軽量なREC表示を出すoptional integration。
 * Flashbackへのhard dependencyやrender-loop内reflectionは持たない。
 */
public final class RecordingIndicatorComponent
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    static final int POLL_INTERVAL_TICKS = 5;
    private static final String ID = "flashback-recording-indicator";
    private static final int TEXT_COLOR = 0xFFFF5555;
    private static final int HUD_PADDING = 8;

    private Field recorderField;
    private Method recorderPausedMethod;
    private boolean supported;
    private boolean recording;
    private boolean paused;
    private int pollCountdown;
    private long startedAtMillis;
    private long pausedAtMillis;
    private long pausedAccumulatedMillis;
    private long lastDisplayedSecond = Long.MIN_VALUE;
    private String displayText = "REC";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init() {
        try {
            ClassLoader loader = RecordingIndicatorComponent.class.getClassLoader();
            Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback", false, loader);
            Class<?> recorder = Class.forName("com.moulberry.flashback.record.Recorder", false, loader);
            recorderField = flashback.getField("RECORDER");
            recorderPausedMethod = recorder.getMethod("isPaused");
            supported = true;
            HudElementRegistry.addLast(
                    Identifier.fromNamespaceAndPath(ChiseTweaksClient.MOD_ID, "recording_indicator"),
                    (graphics, delta) -> renderHud(graphics));
        } catch (ReflectiveOperationException | LinkageError failure) {
            disableAfterApiDrift(failure);
        }
    }

    @Override
    public void tick(Minecraft client) {
        if (!supported) return;

        if (pollCountdown <= 0) {
            pollCountdown = POLL_INTERVAL_TICKS - 1;
            sampleFlashbackState();
        } else {
            pollCountdown--;
        }

        if (recording) refreshDisplayText(System.currentTimeMillis());
    }

    @Override
    public void resetSession(Minecraft client) {
        clearRecordingState();
        pollCountdown = 0;
    }

    @Override
    public void onQuarantined(Minecraft client) {
        supported = false;
        clearRecordingState();
    }

    private void sampleFlashbackState() {
        try {
            Object recorder = recorderField.get(null);
            if (recorder == null) {
                clearRecordingState();
                return;
            }

            long now = System.currentTimeMillis();
            boolean currentlyPaused = Boolean.TRUE.equals(recorderPausedMethod.invoke(recorder));
            if (!recording) {
                recording = true;
                startedAtMillis = now;
                pausedAccumulatedMillis = 0L;
                pausedAtMillis = currentlyPaused ? now : 0L;
                lastDisplayedSecond = Long.MIN_VALUE;
            } else if (paused != currentlyPaused) {
                if (currentlyPaused) {
                    pausedAtMillis = now;
                } else if (pausedAtMillis != 0L) {
                    pausedAccumulatedMillis += Math.max(0L, now - pausedAtMillis);
                    pausedAtMillis = 0L;
                }
            }
            paused = currentlyPaused;
        } catch (ReflectiveOperationException | LinkageError failure) {
            disableAfterApiDrift(failure);
        }
    }

    private void refreshDisplayText(long now) {
        long effectiveNow = paused && pausedAtMillis != 0L ? pausedAtMillis : now;
        long elapsedMillis = Math.max(0L, effectiveNow - startedAtMillis - pausedAccumulatedMillis);
        long elapsedSecond = elapsedMillis / 1000L;
        if (elapsedSecond == lastDisplayedSecond) return;
        lastDisplayedSecond = elapsedSecond;

        long hours = elapsedSecond / 3600L;
        long minutes = (elapsedSecond / 60L) % 60L;
        long seconds = elapsedSecond % 60L;
        displayText = (paused ? "REC PAUSED " : "REC ")
                + twoDigits(hours) + ':' + twoDigits(minutes) + ':' + twoDigits(seconds);
    }

    private void renderHud(net.minecraft.client.gui.GuiGraphics graphics) {
        if (!supported || !recording) return;
        Minecraft client = Minecraft.getInstance();
        int x = Math.max(HUD_PADDING, graphics.guiWidth() - HUD_PADDING - client.font.width(displayText));
        graphics.text(client.font, displayText, x, HUD_PADDING, TEXT_COLOR, true);
    }

    private void disableAfterApiDrift(Throwable failure) {
        supported = false;
        clearRecordingState();
        ChiseTweaksClient.LOGGER.warn(
                "Flashback recording indicator disabled after {}",
                failure.getClass().getSimpleName());
    }

    private void clearRecordingState() {
        recording = false;
        paused = false;
        startedAtMillis = 0L;
        pausedAtMillis = 0L;
        pausedAccumulatedMillis = 0L;
        lastDisplayedSecond = Long.MIN_VALUE;
        displayText = "REC";
    }

    static String twoDigits(long value) {
        return value < 10L ? "0" + value : Long.toString(value);
    }
}
