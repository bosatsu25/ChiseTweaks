package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SettingChangeDispatcherTest {
    @Test
    void revisionAdvancesOnlyForEffectivePublishedSettingChanges() {
        ChiseBooleanSetting booleanSetting = new ChiseBooleanSetting("revisionBoolean", false);
        ChiseIntegerSetting integerSetting = new ChiseIntegerSetting(
                "revisionInteger", 2, 0, 10, "Revision Integer", "Revision Integer", "", "");

        long start = SettingChangeDispatcher.revision();

        booleanSetting.setBooleanValue(false);
        assertEquals(start, SettingChangeDispatcher.revision());

        booleanSetting.setBooleanValue(true);
        assertEquals(start + 1, SettingChangeDispatcher.revision());

        integerSetting.setIntegerValueSilently(4);
        assertEquals(start + 1, SettingChangeDispatcher.revision());

        integerSetting.setIntegerValue(5);
        assertEquals(start + 2, SettingChangeDispatcher.revision());
    }
}
