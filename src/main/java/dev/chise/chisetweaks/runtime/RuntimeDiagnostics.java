package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** クラッシュ・切断・隔離時に再現条件を復元するための構造化診断情報を生成する。 */
public final class RuntimeDiagnostics {
    private RuntimeDiagnostics() {}

    public static RuntimeDiagnosticSnapshot capture(Minecraft client) {
        return new RuntimeDiagnosticSnapshot(
                ChiseTweaksMetadata.MOD_VERSION,
                ClientSessionState.currentSessionId(),
                ClientSessionState.phase().name().toLowerCase(Locale.ROOT),
                dimension(client),
                enabledFeatures(),
                ChiseTexturePackController.selectedVisibilityPackIds(client),
                FeatureManager.getInstance().diagnosticQuarantinedComponentIds(),
                ChiseTexturePackController.isReloadInFlight(),
                ChiseTexturePackController.hasPendingRecovery());
    }

    public static void log(String event, Minecraft client) {
        try {
            RuntimeDiagnosticSnapshot snapshot = capture(client);
            ChiseTweaksClient.LOGGER.info(
                    "Chise diagnostics event={} {}",
                    normalizeEvent(event),
                    snapshot.toLogLine());
        } catch (RuntimeException | LinkageError failure) {
            ChiseTweaksClient.LOGGER.warn(
                    "Chise diagnostics capture failed after {}",
                    failure.getClass().getSimpleName());
        }
    }

    private static List<String> enabledFeatures() {
        ArrayList<String> enabled = new ArrayList<>(FeatureDefinition.VALUES.size());
        for (FeatureSwitch featureSwitch : FeatureSwitches.VALUES) {
            if (featureSwitch.getBooleanValue()) enabled.add(featureSwitch.definition().id());
        }
        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        if (local.lavaHighlightEnabled) enabled.add(FeatureDefinition.LAVA_HIGHLIGHT.id());
        if (local.ancientDebrisAnalyzerEnabled) enabled.add(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER.id());
        if (local.fireVisibilityEnabled) enabled.add(FeatureDefinition.FIRE_VISIBILITY.id());
        return List.copyOf(enabled);
    }

    private static String dimension(Minecraft client) {
        if (client == null || client.level == null) return "none";
        if (client.level.dimension().equals(Level.NETHER)) return "nether";
        if (client.level.dimension().equals(Level.END)) return "end";
        if (client.level.dimension().equals(Level.OVERWORLD)) return "overworld";
        return "other";
    }

    private static String normalizeEvent(String event) {
        if (event == null || event.isBlank()) return "unknown";
        return event.trim().replaceAll("[\\s=]+", "_");
    }
}
