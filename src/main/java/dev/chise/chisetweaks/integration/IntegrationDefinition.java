package dev.chise.chisetweaks.integration;

import java.util.List;
import java.util.Objects;

/** Optional external-mod integrations. These are not counted as Chise runtime visual features. */
public enum IntegrationDefinition {
    LITEMATICA_PICK_REDIRECT(
            "litematica_pick_redirect", "litematica", "Litematica Pick Redirect"),
    TWEAKEROO_TOOL_SWITCH_GUARD(
            "tweakeroo_tool_switch_guard", "tweakeroo", "Tweakeroo Tool Switch Guard"),
    TWEAKEROO_GAMMA_RESTORE(
            "tweakeroo_gamma_restore", "tweakeroo", "Tweakeroo Persistent Gamma Override"),
    TWEAKERMORE_AUTO_PICK_GUARD(
            "tweakermore_auto_pick_guard", "tweakermore", "TweakerMore Auto Pick Guard"),
    TWEAKERMORE_MATERIAL_REFRESH(
            "tweakermore_material_refresh", "tweakermore", "TweakerMore Material List Refresh"),
    SYNCMATICA_REMOVE_DISABLED(
            "syncmatica_remove_disabled", "syncmatica", "Syncmatica Disable Remove"),
    SYNCMATICA_REMOVE_REQUIRE_SHIFT(
            "syncmatica_remove_require_shift", "syncmatica", "Syncmatica Require Shift To Remove"),
    MASA_JAPANESE_UI(
            "masa_japanese_ui", "malilib", "Masa Japanese UI"),
    MASA_GUIDE(
            "masa_guide", "malilib", "Masa Guide");

    public static final List<IntegrationDefinition> VALUES = List.of(values());

    private final String id;
    private final String modId;
    private final String englishName;

    IntegrationDefinition(String id, String modId, String englishName) {
        this.id = requireText(id, "id");
        this.modId = requireText(modId, "modId");
        this.englishName = requireText(englishName, "englishName");
    }

    public String id() { return id; }
    public String modId() { return modId; }
    public String englishName() { return englishName; }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
