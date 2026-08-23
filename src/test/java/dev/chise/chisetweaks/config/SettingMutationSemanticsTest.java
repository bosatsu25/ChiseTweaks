package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingMutationSemanticsTest {
    @Test
    void ruleModeReportsOnlyEffectiveChangesAndContainsCallbackFailure() {
        AtomicInteger callbacks = new AtomicInteger();
        ChiseRuleModeSetting setting = new ChiseRuleModeSetting(
                "mode", ChiseRuleMode.NONE, SettingPersistence.FEATURE_CONFIG);
        setting.setValueChangeCallback(ignored -> {
            callbacks.incrementAndGet();
            throw new IllegalStateException("rebuild failed");
        });

        assertTrue(setting.setValue(ChiseRuleMode.BLACKLIST));
        assertFalse(setting.setValue(ChiseRuleMode.BLACKLIST));
        assertEquals(ChiseRuleMode.BLACKLIST, setting.getValue());
        assertEquals(1, callbacks.get());
        assertEquals(SettingPersistence.FEATURE_CONFIG, setting.persistence());
    }

    @Test
    void stringListReportsOnlyEffectiveChangesAndContainsCallbackFailure() {
        AtomicInteger callbacks = new AtomicInteger();
        ChiseStringListSetting setting = new ChiseStringListSetting(
                "list", List.of(), SettingPersistence.FEATURE_CONFIG);
        setting.setValueChangeCallback(ignored -> {
            callbacks.incrementAndGet();
            throw new IllegalStateException("rebuild failed");
        });

        assertTrue(setting.setStrings(List.of("minecraft:stone")));
        assertFalse(setting.setStrings(List.of("minecraft:stone")));
        assertEquals(List.of("minecraft:stone"), setting.getStrings());
        assertEquals(1, callbacks.get());
        assertEquals(SettingPersistence.FEATURE_CONFIG, setting.persistence());
    }
}
