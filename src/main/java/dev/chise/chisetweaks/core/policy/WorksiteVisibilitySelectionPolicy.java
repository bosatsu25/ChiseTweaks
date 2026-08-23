package dev.chise.chisetweaks.core.policy;

import java.util.EnumSet;
import java.util.Set;

/**
 * Scan-based visual assistance modes are independent. The legacy exclusiveMode argument is kept
 * for config/API compatibility, but it no longer removes other enabled modes.
 */
public final class WorksiteVisibilitySelectionPolicy {
    private WorksiteVisibilitySelectionPolicy() {}

    public enum Mode {
        FINE_THREAD,
        HIDDEN_SURFACE,
        NETHER_PALETTE
    }

    public static Set<Mode> afterToggle(
            Set<Mode> current,
            Mode selected,
            boolean enabled,
            boolean exclusiveMode) {
        EnumSet<Mode> result = copy(current);
        if (enabled) result.add(selected);
        else result.remove(selected);
        return Set.copyOf(result);
    }

    public static Set<Mode> normalize(Set<Mode> current, boolean exclusiveMode) {
        return Set.copyOf(copy(current));
    }

    private static EnumSet<Mode> copy(Set<Mode> current) {
        if (current == null || current.isEmpty()) return EnumSet.noneOf(Mode.class);
        return EnumSet.copyOf(current);
    }
}
