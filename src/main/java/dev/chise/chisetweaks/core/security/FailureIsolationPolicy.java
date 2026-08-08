package dev.chise.chisetweaks.core.security;

/** Pure policy for quarantining a failing optional feature without hiding JVM-fatal errors. */
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
