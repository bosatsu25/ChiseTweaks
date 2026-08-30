package dev.chise.chisetweaks.core.policy;

import java.util.List;

/** Parses the user-facing schematic-block -> inventory-block redirect map without touching Litematica. */
public final class LitematicaPickRedirectPolicy {
    private LitematicaPickRedirectPolicy() {}

    public static String replacementFor(String schematicBlockId, List<String> entries) {
        String source = MasaIdListPolicy.normalize(schematicBlockId);
        if (source.isEmpty() || entries == null) return "";
        for (String entry : entries) {
            Mapping mapping = parse(entry);
            if (mapping != null && mapping.source().equals(source)) return mapping.replacement();
        }
        return "";
    }

    static Mapping parse(String value) {
        if (value == null) return null;
        String[] parts = value.split(",", -1);
        if (parts.length != 2) return null;
        String source = MasaIdListPolicy.normalize(parts[0]);
        String replacement = MasaIdListPolicy.normalize(parts[1]);
        if (!validId(source) || !validId(replacement)) return null;
        return new Mapping(source, replacement);
    }

    private static boolean validId(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) return false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == ':') continue;
            boolean allowed = current >= 'a' && current <= 'z'
                    || current >= '0' && current <= '9'
                    || current == '_' || current == '-' || current == '.' || current == '/';
            if (!allowed) return false;
        }
        return value.indexOf(':', separator + 1) < 0;
    }

    record Mapping(String source, String replacement) {}
}
