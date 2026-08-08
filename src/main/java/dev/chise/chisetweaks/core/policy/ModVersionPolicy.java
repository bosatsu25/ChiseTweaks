package dev.chise.chisetweaks.core.policy;

import java.util.ArrayList;
import java.util.List;

/** Fail-closed numeric release comparison for optional integration gates. */
public final class ModVersionPolicy {
    private ModVersionPolicy() {}

    public static boolean meetsMinimum(String actual, String minimum) {
        List<Integer> left = releaseParts(actual);
        List<Integer> right = releaseParts(minimum);
        if (left.isEmpty() || right.isEmpty()) return false;
        int width = Math.max(left.size(), right.size());
        for (int i = 0; i < width; i++) {
            int a = i < left.size() ? left.get(i) : 0;
            int b = i < right.size() ? right.get(i) : 0;
            if (a != b) return a > b;
        }
        return true;
    }

    public static boolean matchesPinnedRelease(String actual, String pinned) {
        List<Integer> left = releaseParts(actual);
        List<Integer> right = releaseParts(pinned);
        return !left.isEmpty() && !right.isEmpty() && compareParts(left, right) == 0;
    }

    /**
     * Numeric release comparison that ignores common build suffixes.
     * Returns a negative value when actual is older, zero when equal, and a
     * positive value when actual is newer. Unparseable values fail closed as
     * older than a valid expected release.
     */
    public static int compareRelease(String actual, String expected) {
        List<Integer> left = releaseParts(actual);
        List<Integer> right = releaseParts(expected);
        if (left.isEmpty() && right.isEmpty()) return 0;
        if (left.isEmpty()) return -1;
        if (right.isEmpty()) return 1;
        return compareParts(left, right);
    }

    private static int compareParts(List<Integer> left, List<Integer> right) {
        int width = Math.max(left.size(), right.size());
        for (int i = 0; i < width; i++) {
            int a = i < left.size() ? left.get(i) : 0;
            int b = i < right.size() ? right.get(i) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    private static List<Integer> releaseParts(String raw) {
        if (raw == null) return List.of();
        String value = raw.trim();
        if (value.length() > 1 && (value.charAt(0) == 'v' || value.charAt(0) == 'V')
                && Character.isDigit(value.charAt(1))) value = value.substring(1);
        int suffix = value.length();
        int plus = value.indexOf('+');
        int dash = value.indexOf('-');
        if (plus >= 0) suffix = Math.min(suffix, plus);
        if (dash >= 0) suffix = Math.min(suffix, dash);
        value = value.substring(0, suffix);
        if (value.isEmpty()) return List.of();
        ArrayList<Integer> result = new ArrayList<>();
        for (String part : value.split("\\.", -1)) {
            if (part.isEmpty()) return List.of();
            int number = 0;
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (!Character.isDigit(c)) return List.of();
                int digit = c - '0';
                if (number > (Integer.MAX_VALUE - digit) / 10) return List.of();
                number = number * 10 + digit;
            }
            result.add(number);
        }
        return List.copyOf(result);
    }
}
