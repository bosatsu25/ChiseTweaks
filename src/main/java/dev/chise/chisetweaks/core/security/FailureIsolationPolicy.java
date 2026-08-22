package dev.chise.chisetweaks.core.security;

/** JVM致命障害を隠さず、失敗した任意機能だけを隔離する判定規則。 */
public final class FailureIsolationPolicy {
    public static final int MAX_RECOVERABLE_FAILURES = 1;

    private FailureIsolationPolicy() {
    }

    public static boolean isRecoverable(Throwable throwable) {
        return throwable instanceof RuntimeException || throwable instanceof LinkageError;
    }

    public static boolean shouldQuarantine(int recoverableFailureCount) {
        return recoverableFailureCount >= MAX_RECOVERABLE_FAILURES;
    }
}
