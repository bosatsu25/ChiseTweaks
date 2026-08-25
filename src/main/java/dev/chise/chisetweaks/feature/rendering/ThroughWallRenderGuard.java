package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticDetail;
import dev.chise.chisetweaks.runtime.RuntimeDiagnosticEvent;
import dev.chise.chisetweaks.runtime.RuntimeDiagnostics;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;

/** Through-wall renderer の失敗隔離・復旧・close をAnalyzer間で共有する。 */
final class ThroughWallRenderGuard {
    private final ThroughWallMarkerRenderer renderer;
    private final ThroughWallPositionSnapshot snapshot;
    private final String componentId;
    private final String displayName;
    private boolean quarantined;

    ThroughWallRenderGuard(
            ThroughWallMarkerRenderer renderer,
            ThroughWallPositionSnapshot snapshot,
            String componentId,
            String displayName) {
        this.renderer = renderer;
        this.snapshot = snapshot;
        this.componentId = componentId;
        this.displayName = displayName;
    }

    boolean isQuarantined() {
        return quarantined;
    }

    void render(LevelRenderContext context) {
        if (quarantined) return;
        try {
            renderer.render(context, snapshot);
        } catch (RuntimeException | LinkageError failure) {
            quarantined = true;
            snapshot.clear();
            resetRenderer();
            ChiseTweaksClient.LOGGER.error(
                    "{} rendering was quarantined after {}",
                    displayName,
                    failure.getClass().getSimpleName());
            RuntimeDiagnostics.log(
                    RuntimeDiagnosticEvent.COMPONENT_QUARANTINE,
                    Minecraft.getInstance(),
                    RuntimeDiagnosticDetail.of("componentId", componentId),
                    RuntimeDiagnosticDetail.of("failure", failure.getClass().getSimpleName()),
                    RuntimeDiagnosticDetail.of("stage", "render"));
        }
    }

    void resetSession() {
        quarantined = false;
    }

    void close() {
        try {
            renderer.close();
        } catch (RuntimeException | LinkageError cleanupFailure) {
            ChiseTweaksClient.LOGGER.warn(
                    "{} renderer close failed after {}",
                    displayName,
                    cleanupFailure.getClass().getSimpleName());
        }
    }

    private void resetRenderer() {
        try {
            renderer.resetAfterFailure();
        } catch (RuntimeException | LinkageError cleanupFailure) {
            ChiseTweaksClient.LOGGER.warn(
                    "{} renderer cleanup failed after {}",
                    displayName,
                    cleanupFailure.getClass().getSimpleName());
        }
    }
}
