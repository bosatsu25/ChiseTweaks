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
        int step) {

    static ChiseTweaksSettingRowDefinition header(String id, String name) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.HEADER, id, name, "", null, null, 0);
    }

    static ChiseTweaksSettingRowDefinition bool(
            String id,
            String name,
            String description,
            ChiseBooleanSetting config) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.BOOLEAN, id, name, description, config, null, 0);
    }

    static ChiseTweaksSettingRowDefinition integer(
            String id,
            String name,
            String description,
            ChiseIntegerSetting config,
            int step) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.INTEGER, id, name, description, null, config, Math.max(1, step));
    }

    enum Kind {
        HEADER,
        BOOLEAN,
        INTEGER
    }
}
