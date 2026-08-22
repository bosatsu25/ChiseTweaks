package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;

record ChiseTweaksSettingRowDefinition(
        Kind kind,
        String id,
        String name,
        String description,
        ChiseBooleanSetting booleanConfig,
        ChiseIntegerSetting integerConfig,
        int step,
        Action action,
        String actionLabel) {

    static ChiseTweaksSettingRowDefinition header(String id, String name) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.HEADER, id, name, "", null, null, 0, null, "");
    }

    static ChiseTweaksSettingRowDefinition headerAction(
            String id,
            String name,
            Action action,
            String actionLabel) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.HEADER, id, name, "", null, null, 0, action,
                actionLabel == null ? "" : actionLabel);
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

    static ChiseTweaksSettingRowDefinition boolAction(
            String id,
            String name,
            String description,
            ChiseBooleanSetting config,
            Action action,
            String actionLabel) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.BOOLEAN_ACTION, id, name, description == null ? "" : description,
                config, null, 0, action, actionLabel == null ? "" : actionLabel);
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

    enum Kind {
        HEADER,
        BOOLEAN,
        BOOLEAN_ACTION,
        INTEGER,
        ACTION
    }

    enum Action {
        OPEN_HIGHLIGHT_DETAILS,
        OPEN_LAVA_DETAILS,
        EDIT_BLOCK_FILTER,
        EDIT_ENTITY_FILTER,
        EDIT_ORE_COMPAT
    }
}
