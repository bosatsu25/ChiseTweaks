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

    /** async reload失敗後も、旧optionsの証拠へ依存せず同じ移行を再開するための最小状態。 */
    public record PendingIntent(Source source, List<String> fallbackIds, List<String> targetIds) {
        public PendingIntent {
            source = Objects.requireNonNull(source, "source");
            if (source == Source.ALREADY_MIGRATED) {
                throw new IllegalArgumentException("completed migration cannot be pending");
            }
            fallbackIds = sanitize(fallbackIds);
            targetIds = sanitize(targetIds);
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

    public static PendingIntent pendingIntent(Plan plan, List<String> currentSelection) {
        Objects.requireNonNull(plan, "plan");
        if (!plan.selectionChanged() || plan.source() == Source.ALREADY_MIGRATED) {
            throw new IllegalArgumentException("only an incomplete selection change can be pending");
        }
        return new PendingIntent(plan.source(), currentSelection, plan.selectedIds());
    }

    /**
     * 前回reloadが失敗してoptionsの旧pack証拠が消えても、保存済みintentから同じ移行を再構成する。
     * unrelated packが変更されていた場合はその現行状態を維持し、Chise管理packだけをtarget状態へ合わせる。
     */
    public static Plan resumePending(
            PendingIntent intent,
            List<String> currentSelection,
            String legacyPackId,
            String chestPackId,
            String whiteConcretePackId) {
        Objects.requireNonNull(intent, "intent");
        String legacy = requireId(legacyPackId);
        String chest = requireId(chestPackId);
        String concrete = requireId(whiteConcretePackId);
        List<String> current = sanitize(currentSelection);
        if (current.equals(intent.targetIds())) {
            return new Plan(intent.source(), current, false);
        }
        if (current.equals(intent.fallbackIds())) {
            return new Plan(intent.source(), intent.targetIds(), true);
        }

        List<String> merged = mergeManagedState(
                current,
                intent.targetIds(),
                legacy,
                chest,
                concrete);
        return new Plan(intent.source(), merged, !merged.equals(current));
    }

    static boolean optionPackSelected(String optionsDocument, String packId) {
        if (optionsDocument == null || optionsDocument.isBlank()) return false;
        String requiredId = requireId(packId);
        String exactToken = "\"" + requiredId + "\"";
        for (String line : optionsDocument.split("\\R", -1)) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("resourcePacks:")) continue;
            return trimmed.contains(exactToken);
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

    private static List<String> mergeManagedState(
            List<String> current,
            List<String> target,
            String legacy,
            String chest,
            String concrete) {
        ArrayList<String> base = new ArrayList<>();
        for (String id : current) {
            if (!isManaged(id, legacy, chest, concrete)) base.add(id);
        }

        ArrayList<String> desiredManaged = new ArrayList<>(2);
        for (String id : target) {
            if ((id.equals(chest) || id.equals(concrete)) && !desiredManaged.contains(id)) {
                desiredManaged.add(id);
            }
        }
        if (desiredManaged.isEmpty()) return List.copyOf(base);

        int firstManaged = firstManagedIndex(target, legacy, chest, concrete);
        String before = nearestUnmanagedBefore(target, firstManaged, legacy, chest, concrete);
        String after = nearestUnmanagedAfter(target, firstManaged, legacy, chest, concrete);
        int insertionIndex;
        if (before != null && base.contains(before)) {
            insertionIndex = base.indexOf(before) + 1;
        } else if (after != null && base.contains(after)) {
            insertionIndex = base.indexOf(after);
        } else {
            insertionIndex = Math.min(unmanagedCountBefore(target, firstManaged, legacy, chest, concrete), base.size());
        }
        base.addAll(insertionIndex, desiredManaged);
        return List.copyOf(base);
    }

    private static int firstManagedIndex(
            List<String> values,
            String legacy,
            String chest,
            String concrete) {
        for (int index = 0; index < values.size(); index++) {
            if (isManaged(values.get(index), legacy, chest, concrete)) return index;
        }
        return values.size();
    }

    private static String nearestUnmanagedBefore(
            List<String> values,
            int index,
            String legacy,
            String chest,
            String concrete) {
        for (int cursor = Math.min(index, values.size()) - 1; cursor >= 0; cursor--) {
            String id = values.get(cursor);
            if (!isManaged(id, legacy, chest, concrete)) return id;
        }
        return null;
    }

    private static String nearestUnmanagedAfter(
            List<String> values,
            int index,
            String legacy,
            String chest,
            String concrete) {
        for (int cursor = Math.max(0, index); cursor < values.size(); cursor++) {
            String id = values.get(cursor);
            if (!isManaged(id, legacy, chest, concrete)) return id;
        }
        return null;
    }

    private static int unmanagedCountBefore(
            List<String> values,
            int index,
            String legacy,
            String chest,
            String concrete) {
        int count = 0;
        for (int cursor = 0; cursor < Math.min(index, values.size()); cursor++) {
            if (!isManaged(values.get(cursor), legacy, chest, concrete)) count++;
        }
        return count;
    }

    private static boolean isManaged(String id, String legacy, String chest, String concrete) {
        return id.equals(legacy) || id.equals(chest) || id.equals(concrete);
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
