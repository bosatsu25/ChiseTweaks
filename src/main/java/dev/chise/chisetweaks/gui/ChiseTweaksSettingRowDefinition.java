package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;

import java.util.Objects;

final class ChiseTweaksSettingRowDefinition {
    private final Kind kind;
    private final String id;
    private final String name;
    private final String description;
    private final ChiseBooleanSetting booleanConfig;
    private final ChiseIntegerSetting integerConfig;
    private final int step;
    private final Action action;
    private final String actionLabel;

    private ChiseTweaksSettingRowDefinition(
            Kind kind,
            String id,
            String name,
            String description,
            ChiseBooleanSetting booleanConfig,
            ChiseIntegerSetting integerConfig,
            int step,
            Action action,
            String actionLabel) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.id = requireId(id);
        this.name = name == null ? "" : name;
        this.description = description == null ? "" : description;
        this.booleanConfig = booleanConfig;
        this.integerConfig = integerConfig;
        this.step = step;
        this.action = action;
        this.actionLabel = actionLabel == null ? "" : actionLabel;
    }

    Kind kind() { return kind; }
    String name() { return name; }
    String description() { return description; }
    ChiseBooleanSetting booleanConfig() { return booleanConfig; }
    ChiseIntegerSetting integerConfig() { return integerConfig; }
    int step() { return step; }
    Action action() { return action; }
    String actionLabel() { return actionLabel; }

    String id() {
        return id;
    }

    static ChiseTweaksSettingRowDefinition header(String id, String name) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.HEADER, id, name, "", null, null, 0, null, "");
    }

    static ChiseTweaksSettingRowDefinition info(String id, String name, String description) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.INFO, id, name, description, null, null, 0, null, "");
    }

    static ChiseTweaksSettingRowDefinition bool(
            String id,
            String name,
            String description,
            ChiseBooleanSetting config) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.BOOLEAN, id, name, description == null ? "" : description,
                config, null, 0, null, "");
    }

    static ChiseTweaksSettingRowDefinition integer(
            String id,
            String name,
            String description,
            ChiseIntegerSetting config,
            int step) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.INTEGER, id, name, description == null ? "" : description,
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
                id,
                name,
                description == null ? "" : description,
                null,
                null,
                0,
                action,
                actionLabel == null ? "" : actionLabel);
    }

    private static String requireId(String value) {
        String normalized = Objects.requireNonNull(value, "id").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("setting row id must not be blank");
        if (!normalized.equals(value)) {
            throw new IllegalArgumentException("setting row id must not contain surrounding whitespace");
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (!Character.isLetterOrDigit(current)
                    && current != '.' && current != '_' && current != '-') {
                throw new IllegalArgumentException("setting row id contains an unsupported character");
            }
        }
        return value;
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
        EDIT_LITEMATICA_PICK_REDIRECT,
        EDIT_TWEAKERMORE_AUTO_PICK_GUARD,
        EDIT_TWEAKEROO_TOOL_SWITCH_GUARD,
        OPEN_MASA_GUIDE,
        SELECT_PATTERN_REFERENCE,
        CLEAR_PATTERN_REFERENCE
    }
}
