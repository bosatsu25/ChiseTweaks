package dev.chise.chisetweaks.core.policy;

/** Chise decides whether Syncmatica's existing remove listener may run; Chise never sends its packet. */
public final class SyncmaticaRemoveGuardPolicy {
    private SyncmaticaRemoveGuardPolicy() {}

    public static Decision decide(boolean disabled, boolean requireShift, boolean shiftDown) {
        if (disabled) return Decision.DENY_DISABLED;
        if (requireShift && !shiftDown) return Decision.DENY_SHIFT_REQUIRED;
        return Decision.ALLOW;
    }

    public enum Decision {
        ALLOW,
        DENY_DISABLED,
        DENY_SHIFT_REQUIRED;

        public boolean allowed() {
            return this == ALLOW;
        }
    }
}
