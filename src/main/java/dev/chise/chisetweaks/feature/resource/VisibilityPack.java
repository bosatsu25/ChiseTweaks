package dev.chise.chisetweaks.feature.resource;

import dev.chise.chisetweaks.ChiseTweaksClient;

import java.util.Objects;

/** ChiseTweaksが管理するbuilt-in resource packを型安全に表現する。 */
public enum VisibilityPack {
    CHEST("chise_chest_visibility", "Bright Chest"),
    WHITE_CONCRETE("chise_white_concrete_visibility", "Bright Concrete");

    private final String path;
    private final String displayName;

    VisibilityPack(String path, String displayName) {
        this.path = requireText(path, "path");
        this.displayName = requireText(displayName, "displayName");
    }

    public String path() { return path; }
    public String displayName() { return displayName; }
    public String repositoryPackId() { return ChiseTweaksClient.MOD_ID + ":" + path; }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
