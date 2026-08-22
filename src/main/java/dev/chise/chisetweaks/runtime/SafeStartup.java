package dev.chise.chisetweaks.runtime;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.core.security.FailureIsolationPolicy;
import dev.chise.chisetweaks.core.security.RuntimeSecurityPolicy;
import dev.chise.chisetweaks.core.security.StartupPhasePolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * 任意の初期化フェーズを個別に隔離する。
 * VM致命障害は捕捉せず、通常の実行時障害やリンク障害だけを対象フェーズ内に封じ込める。
 */
public final class SafeStartup {
    private static final List<String> FAILURES = new ArrayList<>(StartupPhasePolicy.MAX_RECORDED_FAILURES);

    private SafeStartup() {
    }

    public static synchronized boolean run(String phaseName, Runnable action) {
        String safeName = RuntimeSecurityPolicy.sanitizeDiagnosticValue(phaseName);
        try {
            action.run();
            return true;
        } catch (RuntimeException | LinkageError failure) {
            if (!FailureIsolationPolicy.isRecoverable(failure)) {
                throw failure;
            }
            if (FAILURES.size() < StartupPhasePolicy.MAX_RECORDED_FAILURES) {
                FAILURES.add(safeName + ":" + failure.getClass().getSimpleName());
            }
            ChiseTweaksClient.LOGGER.error(
                    "ChiseTweaks startup phase '{}' was disabled after {}",
                    safeName,
                    failure.getClass().getSimpleName());
            return false;
        }
    }

    public static synchronized List<String> failures() {
        return List.copyOf(FAILURES);
    }
}
