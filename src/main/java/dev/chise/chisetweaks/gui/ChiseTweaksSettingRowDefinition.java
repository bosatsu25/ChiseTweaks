package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.ChiseIntegerSetting;

/** Immutable row description kept separate from Minecraft widget state. */
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

    static ChiseTweaksSettingRowDefinition bool(
            String id,
            String name,
            String description,
            ChiseBooleanSetting config) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.BOOLEAN, id, name, description, config, null, 0, null, "");
    }

    static ChiseTweaksSettingRowDefinition integer(
            String id,
            String name,
            String description,
            ChiseIntegerSetting config,
            int step) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.INTEGER, id, name, description, null, config, Math.max(1, step), null, "");
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
                description,
                null,
                null,
                0,
                action,
                actionLabel == null ? "" : actionLabel);
    }

    enum Kind {
        HEADER,
        BOOLEAN,
        INTEGER,
        ACTION
    }

    enum Action {
        EDIT_BLOCK_FILTER,
        EDIT_ENTITY_FILTER
    }
}
