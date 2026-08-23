package dev.chise.chisetweaks.runtime;

import java.util.List;
import java.util.Objects;

/** 異常解析に必要なChiseTweaks実行状態を1行で保存できる不変snapshot。 */
public record RuntimeDiagnosticSnapshot(
        String version,
        long sessionId,
        String sessionPhase,
        String dimension,
        List<String> enabledFeatures,
        List<String> selectedVisibilityPacks,
        List<String> quarantinedComponents,
        boolean resourceReloadInFlight,
        boolean resourceRecoveryPending) {

    public RuntimeDiagnosticSnapshot {
        version = requireText(version, "version");
        sessionPhase = requireText(sessionPhase, "sessionPhase");
        dimension = requireText(dimension, "dimension");
        enabledFeatures = List.copyOf(Objects.requireNonNull(enabledFeatures, "enabledFeatures"));
        selectedVisibilityPacks = List.copyOf(
                Objects.requireNonNull(selectedVisibilityPacks, "selectedVisibilityPacks"));
        quarantinedComponents = List.copyOf(
                Objects.requireNonNull(quarantinedComponents, "quarantinedComponents"));
    }

    public String reloadState() {
        return reloadState(resourceReloadInFlight, resourceRecoveryPending);
    }

    public static String reloadState(boolean reloadInFlight, boolean recoveryPending) {
        if (reloadInFlight && recoveryPending) return "reloading_with_recovery";
        if (reloadInFlight) return "reloading";
        if (recoveryPending) return "recovery_pending";
        return "idle";
    }

    public String toLogLine() {
        return "version=" + version
                + " sessionId=" + sessionId
                + " phase=" + sessionPhase
                + " dimension=" + dimension
                + " enabled=" + listValue(enabledFeatures)
                + " visibilityPacks=" + listValue(selectedVisibilityPacks)
                + " quarantined=" + listValue(quarantinedComponents)
                + " reloadState=" + reloadState();
    }

    private static String listValue(List<String> values) {
        return "[" + String.join(",", values) + "]";
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
