package dev.chise.chisetweaks.core.vision;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class BlockInspectionPolicy {
    private static final int MAX_DETAILS = 8;
    private static final int MAX_VALUE_LENGTH = 48;
    private static final Pattern BLOCK_ID_PATTERN =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern SAFE_TOKEN_PATTERN =
            Pattern.compile("[a-z0-9_.-]+");

    private static final Set<String> TECHNICAL_PROPERTIES = Set.of(
            "attached", "disarmed", "powered", "facing", "north", "east", "south", "west");
    private static final Set<String> HIDDEN_PROPERTIES = Set.of(
            "bloom", "waterlogged", "facing");

    private BlockInspectionPolicy() {}

    public static InspectionPresentation inspect(String rawBlockId, Map<String, String> rawProperties) {
        String blockId = normalizeBlockId(rawBlockId);
        return inspect(blockId, rawProperties, classify(blockId));
    }

    public static InspectionPresentation inspect(
            String rawBlockId,
            Map<String, String> rawProperties,
            BlockInspectionCategory category) {
        String blockId = normalizeBlockId(rawBlockId);
        Map<String, String> properties = rawProperties == null ? Map.of() : rawProperties;
        if (category == null || category == BlockInspectionCategory.NONE
                || !matchesNormalized(blockId, category)) {
            return InspectionPresentation.none(blockId);
        }

        Set<String> allowed = category == BlockInspectionCategory.TECHNICAL_TRACE
                ? TECHNICAL_PROPERTIES
                : category == BlockInspectionCategory.HIDDEN_SURFACE ? HIDDEN_PROPERTIES : Set.of();

        ArrayList<String> details = new ArrayList<>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (details.size() >= MAX_DETAILS) break;
            String key = safeToken(entry.getKey());
            String value = safeToken(entry.getValue());
            if (!key.isEmpty() && !value.isEmpty() && allowed.contains(key)) {
                details.add(key + "=" + value);
            }
        }
        details.sort(String::compareTo);
        return new InspectionPresentation(
                blockId, category, List.copyOf(details), category.argb());
    }

    public static BlockInspectionCategory classify(String rawBlockId) {
        Set<BlockInspectionCategory> categories = categories(rawBlockId);
        if (categories.isEmpty()) return BlockInspectionCategory.NONE;
        for (BlockInspectionCategory category : BlockInspectionCategory.values()) {
            if (category != BlockInspectionCategory.NONE && categories.contains(category)) {
                return category;
            }
        }
        return BlockInspectionCategory.NONE;
    }

    public static Set<BlockInspectionCategory> categories(String rawBlockId) {
        String id = normalizeBlockId(rawBlockId);
        if (id.isEmpty()) return Set.of();
        return VisualCapabilityCatalog.categories(id);
    }

    public static boolean matches(String rawBlockId, BlockInspectionCategory category) {
        if (category == null || category == BlockInspectionCategory.NONE) return false;
        return matchesNormalized(normalizeBlockId(rawBlockId), category);
    }

    private static boolean matchesNormalized(String id, BlockInspectionCategory category) {
        return !id.isEmpty() && VisualCapabilityCatalog.matches(id, category);
    }

    public static boolean isScanCategory(BlockInspectionCategory category) {
        return category != null && category != BlockInspectionCategory.NONE;
    }

    public static Set<String> materialHighlightIds() {
        return VisualCapabilityCatalog.materialHighlightIds();
    }

    public static Set<String> netherPaletteIds() {
        return VisualCapabilityCatalog.netherPaletteIds();
    }

    private static String normalizeBlockId(String raw) {
        if (raw == null) return "";
        String normalized = Normalizer.normalize(
                raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        if (normalized.length() > 256
                || !BLOCK_ID_PATTERN.matcher(normalized).matches()) return "";
        return normalized;
    }

    private static String safeToken(String raw) {
        if (raw == null) return "";
        String value = Normalizer.normalize(
                raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        if (value.length() > MAX_VALUE_LENGTH
                || !SAFE_TOKEN_PATTERN.matcher(value).matches()) return "";
        return value;
    }

    public record InspectionPresentation(
            String blockId,
            BlockInspectionCategory category,
            List<String> details,
            int argb) {
        public InspectionPresentation {
            blockId = Objects.requireNonNull(blockId, "blockId");
            category = Objects.requireNonNull(category, "category");
            details = List.copyOf(Objects.requireNonNull(details, "details"));
            if (details.size() > MAX_DETAILS) throw new IllegalArgumentException("too many details");
        }

        private static InspectionPresentation none(String blockId) {
            return new InspectionPresentation(
                    blockId, BlockInspectionCategory.NONE, List.of(), 0xFFFFFFFF);
        }

        public String compactDetails() {
            return details.isEmpty() ? "" : String.join(", ", details);
        }
    }
}
