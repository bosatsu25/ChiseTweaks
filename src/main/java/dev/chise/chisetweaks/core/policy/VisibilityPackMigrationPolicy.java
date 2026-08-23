package dev.chise.chisetweaks.core.policy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** 旧単一Visibility packから分割packへ移行する際の選択状態を決定する。 */
public final class VisibilityPackMigrationPolicy {
    public enum Source {
        ALREADY_MIGRATED,
        SPLIT_PACK_STATE,
        LEGACY_ENABLED,
        LEGACY_DISABLED,
        NEW_INSTALL
    }

    public record Plan(Source source, List<String> selectedIds, boolean selectionChanged) {
        public Plan {
            source = Objects.requireNonNull(source, "source");
            selectedIds = List.copyOf(selectedIds);
        }
    }

    private VisibilityPackMigrationPolicy() {}

    public static Plan plan(
            List<String> currentSelection,
            boolean markerPresent,
            boolean priorChiseConfigPresent,
            String optionsDocument,
            String legacyPackId,
            String chestPackId,
            String whiteConcretePackId) {
        String legacy = requireId(legacyPackId);
        String chest = requireId(chestPackId);
        String concrete = requireId(whiteConcretePackId);
        List<String> current = sanitize(currentSelection);

        if (markerPresent) {
            return new Plan(Source.ALREADY_MIGRATED, current, false);
        }

        boolean splitStateRecorded = optionPackSelected(optionsDocument, chest)
                || optionPackSelected(optionsDocument, concrete);
        if (splitStateRecorded) {
            List<String> cleaned = without(current, legacy);
            return new Plan(Source.SPLIT_PACK_STATE, cleaned, !cleaned.equals(current));
        }

        if (optionPackSelected(optionsDocument, legacy)) {
            List<String> migrated = enableSplitPacks(current, legacy, chest, concrete);
            return new Plan(Source.LEGACY_ENABLED, migrated, !migrated.equals(current));
        }

        if (priorChiseConfigPresent) {
            List<String> migrated = disableSplitPacks(current, legacy, chest, concrete);
            return new Plan(Source.LEGACY_DISABLED, migrated, !migrated.equals(current));
        }

        List<String> cleaned = without(current, legacy);
        return new Plan(Source.NEW_INSTALL, cleaned, !cleaned.equals(current));
    }

    static boolean optionPackSelected(String optionsDocument, String packId) {
        if (optionsDocument == null || optionsDocument.isBlank()) return false;
        String requiredId = requireId(packId);
        for (String line : optionsDocument.split("\\R", -1)) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("resourcePacks:")) continue;
            return trimmed.contains(requiredId);
        }
        return false;
    }

    private static List<String> enableSplitPacks(
            List<String> current,
            String legacy,
            String chest,
            String concrete) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        boolean insertedAtLegacy = false;
        for (String id : current) {
            if (id.equals(legacy)) {
                if (!insertedAtLegacy) {
                    result.add(chest);
                    result.add(concrete);
                    insertedAtLegacy = true;
                }
                continue;
            }
            if (!id.equals(chest) && !id.equals(concrete)) result.add(id);
        }
        if (!insertedAtLegacy) {
            result.add(chest);
            result.add(concrete);
        }
        return List.copyOf(result);
    }

    private static List<String> disableSplitPacks(
            List<String> current,
            String legacy,
            String chest,
            String concrete) {
        ArrayList<String> result = new ArrayList<>(current.size());
        for (String id : current) {
            if (id.equals(legacy) || id.equals(chest) || id.equals(concrete)) continue;
            result.add(id);
        }
        return List.copyOf(result);
    }

    private static List<String> without(List<String> current, String removedId) {
        ArrayList<String> result = new ArrayList<>(current.size());
        for (String id : current) {
            if (!id.equals(removedId)) result.add(id);
        }
        return List.copyOf(result);
    }

    private static List<String> sanitize(List<String> selection) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (selection != null) {
            for (String value : selection) {
                if (value == null) continue;
                String normalized = value.trim();
                if (!normalized.isEmpty()) result.add(normalized);
            }
        }
        return List.copyOf(result);
    }

    private static String requireId(String value) {
        String normalized = Objects.requireNonNull(value, "pack id").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("pack id must not be blank");
        return normalized;
    }
}
