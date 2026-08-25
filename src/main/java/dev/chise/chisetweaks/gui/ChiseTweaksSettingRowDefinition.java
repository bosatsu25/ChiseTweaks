package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;

import java.util.Objects;

final class ChiseTweaksSettingRowDefinition {
    private final Kind kind;
    private final SettingRowId settingId;
    private final String name;
    private final String description;
    private final ChiseBooleanSetting booleanConfig;
    private final ChiseIntegerSetting integerConfig;
    private final int step;
    private final Action action;
    private final String actionLabel;

    private ChiseTweaksSettingRowDefinition(
            Kind kind,
            SettingRowId settingId,
            String name,
            String description,
            ChiseBooleanSetting booleanConfig,
            ChiseIntegerSetting integerConfig,
            int step,
            Action action,
            String actionLabel) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.settingId = Objects.requireNonNull(settingId, "settingId");
        this.name = name == null ? "" : name;
        this.description = description == null ? "" : description;
        this.booleanConfig = booleanConfig;
        this.integerConfig = integerConfig;
        this.step = step;
        this.action = action;
        this.actionLabel = actionLabel == null ? "" : actionLabel;
    }

    Kind kind() { return kind; }
    SettingRowId settingId() { return settingId; }
    String name() { return name; }
    String description() { return description; }
    ChiseBooleanSetting booleanConfig() { return booleanConfig; }
    ChiseIntegerSetting integerConfig() { return integerConfig; }
    int step() { return step; }
    Action action() { return action; }
    String actionLabel() { return actionLabel; }

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
