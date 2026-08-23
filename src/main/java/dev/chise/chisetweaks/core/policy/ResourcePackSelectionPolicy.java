package dev.chise.chisetweaks.core.policy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Chise管理built-in resource packの選択状態を決定的に更新する純粋policy。 */
public final class ResourcePackSelectionPolicy {
    private ResourcePackSelectionPolicy() {}

    public static List<String> withPack(List<String> current, String packId, boolean enabled) {
        String id = requirePackId(packId);
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (current != null) {
            for (String selected : current) {
                if (selected != null && !selected.isBlank()) ordered.add(selected);
            }
        }
        if (enabled) ordered.add(id);
        else ordered.remove(id);
        return List.copyOf(new ArrayList<>(ordered));
    }

    private static String requirePackId(String packId) {
        String normalized = Objects.requireNonNull(packId, "packId").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("packId must not be blank");
        return normalized;
    }
}
