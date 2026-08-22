package dev.chise.chisetweaks.core.security;

/**
 * 起動時の処理量を固定上限で制限し、任意機能の故障が後続処理へ連鎖し続けることを防ぐ。
 */
public final class StartupPhasePolicy {
    public static final int MAX_RECORDED_FAILURES = 16;

    private StartupPhasePolicy() {
    }

    public static int boundedFailureCount(int requested) {
        return Math.max(0, Math.min(requested, MAX_RECORDED_FAILURES));
    }
}
