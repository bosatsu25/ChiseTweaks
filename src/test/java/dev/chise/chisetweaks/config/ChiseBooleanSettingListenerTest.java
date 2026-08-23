package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseBooleanSettingListenerTest {
    @Test
    void primaryCallbackAndIndependentListenerBothReceiveOneRealChange() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.setValueChangeCallback(ignored -> calls.add("primary"));
        setting.addValueChangeListener(ignored -> calls.add("listener"));

        assertTrue(setting.setBooleanValue(true));
        assertFalse(setting.setBooleanValue(true));

        assertEquals(List.of("primary", "listener"), calls);
    }

    @Test
    void refusedWriteDoesNotEmitFalseChangeNotification() {
        RefusingSetting setting = new RefusingSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.setValueChangeCallback(ignored -> calls.add("primary"));
        setting.addValueChangeListener(ignored -> calls.add("listener"));

        assertFalse(setting.setBooleanValue(true));

        assertFalse(setting.getBooleanValue());
        assertEquals(List.of(), calls);
    }

    @Test
    void persistenceDomainIsExplicitForApplyManagedAndExternalSettings() {
        assertEquals(SettingPersistence.FEATURE_CONFIG, new TestSetting().persistence());
        assertEquals(SettingPersistence.EXTERNAL, new ImmediateSetting().persistence());
    }

    @Test
    void replacingPrimaryCallbackDoesNotDeleteIndependentListeners() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.addValueChangeListener(ignored -> calls.add("listener"));
        setting.setValueChangeCallback(ignored -> calls.add("first"));
        setting.setValueChangeCallback(ignored -> calls.add("replacement"));

        assertTrue(setting.setBooleanValue(true));

        assertEquals(List.of("replacement", "listener"), calls);
    }

    @Test
    void primaryFailureIsContainedAndDoesNotPreventIndependentListenerNotification() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.setValueChangeCallback(ignored -> {
            calls.add("primary");
            throw new IllegalStateException("primary failure");
        });
        setting.addValueChangeListener(ignored -> calls.add("listener"));

        assertTrue(setting.setBooleanValue(true));
        assertTrue(setting.getBooleanValue());
        assertEquals(List.of("primary", "listener"), calls);
    }

    @Test
    void failingIndependentListenerIsContainedAndDoesNotBlockLaterListeners() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.addValueChangeListener(ignored -> {
            calls.add("first");
            throw new IllegalStateException("listener failure");
        });
        setting.addValueChangeListener(ignored -> calls.add("second"));

        assertTrue(setting.setBooleanValue(true));
        assertTrue(setting.getBooleanValue());
        assertEquals(List.of("first", "second"), calls);
    }

    private static class TestSetting extends ChiseBooleanSetting {
        private boolean value;

        TestSetting() {
            this(SettingPersistence.FEATURE_CONFIG);
        }

        TestSetting(SettingPersistence persistence) {
            super("test", false, "Test", "テスト", "test", "テスト", persistence);
        }

        @Override
        protected boolean readValue() {
            return value;
        }

        @Override
        protected void writeValue(boolean value) {
            this.value = value;
        }
    }

    private static final class RefusingSetting extends TestSetting {
        @Override
        protected void writeValue(boolean value) {
        }
    }

    private static final class ImmediateSetting extends TestSetting {
        ImmediateSetting() {
            super(SettingPersistence.EXTERNAL);
        }
    }
}
