package dev.chise.chisetweaks.gui;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;

/** Immutable row description kept separate from Minecraft widget state. */
record ChiseTweaksSettingRowDefinition(
        Kind kind,
        String id,
        String name,
        String description,
        IConfigBoolean booleanConfig,
        ConfigInteger integerConfig,
        int step) {

    static ChiseTweaksSettingRowDefinition header(String id, String name) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.HEADER, id, name, "", null, null, 0);
    }

    static ChiseTweaksSettingRowDefinition bool(
            String id,
            String name,
            String description,
            IConfigBoolean config) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.BOOLEAN, id, name, description, config, null, 0);
    }

    static ChiseTweaksSettingRowDefinition integer(
            String id,
            String name,
            String description,
            ConfigInteger config,
            int step) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.INTEGER, id, name, description, null, config, Math.max(1, step));
    }

    static ChiseTweaksSettingRowDefinition action(
            String id,
            String name,
            String description) {
        return new ChiseTweaksSettingRowDefinition(
                Kind.ACTION, id, name, description, null, null, 0);
    }

    enum Kind {
        HEADER,
        BOOLEAN,
        INTEGER,
        ACTION
    }
}
