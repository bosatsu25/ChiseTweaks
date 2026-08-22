package dev.chise.chisetweaks.core.policy;

import java.util.EnumSet;
import java.util.Set;

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
        if (!enabled) {
            result.remove(selected);
            return Set.copyOf(result);
        }
        if (exclusiveMode) result.clear();
        result.add(selected);
        return Set.copyOf(result);
    }

    public static Set<Mode> normalize(Set<Mode> current, boolean exclusiveMode) {
        EnumSet<Mode> result = copy(current);

        if (!exclusiveMode || result.size() < 2) return Set.copyOf(result);
        return Set.of(result.iterator().next());
    }

    private static EnumSet<Mode> copy(Set<Mode> current) {
        if (current == null || current.isEmpty()) return EnumSet.noneOf(Mode.class);
        return EnumSet.copyOf(current);
    }
}
