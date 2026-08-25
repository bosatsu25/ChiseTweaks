package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;

import java.util.Objects;

record ChiseTweaksSettingRowDefinition(
        Kind kind,
        SettingRowId settingId,
        String name,
        String description,
        ChiseBooleanSetting booleanConfig,
        ChiseIntegerSetting integerConfig,
        int step,
        Action action,
        String actionLabel) {

    ChiseTweaksSettingRowDefinition {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(settingId, "settingId");
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        actionLabel = actionLabel == null ? "" : actionLabel;
    }

    String id() {
        return settingId.value();
    }

    static ChiseTweaksSettingRowDefinition header(String id, String name) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.HEADER, SettingRowId.of(id), name, "", null, null, 0, null, "");
    }

    static ChiseTweaksSettingRowDefinition info(String id, String name, String description) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.INFO, SettingRowId.of(id), name, description, null, null, 0, null, "");
    }

    static ChiseTweaksSettingRowDefinition bool(
            String id,
            String name,
            String description,
            ChiseBooleanSetting config) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.BOOLEAN, SettingRowId.of(id), name, description == null ? "" : description,
                config, null, 0, null, "");
    }

    static ChiseTweaksSettingRowDefinition integer(
            String id,
            String name,
            String description,
            ChiseIntegerSetting config,
            int step) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.INTEGER, SettingRowId.of(id), name, description == null ? "" : description,
                null, config, Math.max(1, step), null, "");
    }

    static ChiseTweaksSettingRowDefinition action(
            String id,
            String name,
            String description,
            Action action,
            String actionLabel) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.ACTION,
                SettingRowId.of(id),
                name,
                description == null ? "" : description,
                null,
                null,
                0,
                action,
                actionLabel == null ? "" : actionLabel);
    }

    enum Kind {
        HEADER,
        INFO,
        BOOLEAN,
        INTEGER,
        ACTION
    }

    enum Action {
        EDIT_BLOCK_FILTER,
        EDIT_ENTITY_FILTER,
        EDIT_ORE_COMPAT,
        SELECT_PATTERN_REFERENCE,
        CLEAR_PATTERN_REFERENCE
    }
}
