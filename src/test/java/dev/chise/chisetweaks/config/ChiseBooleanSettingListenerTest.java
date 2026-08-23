package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseBooleanSettingListenerTest {
    @Test
    void primaryCallbackAndIndependentListenerBothReceiveOneRealChange() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.setValueChangeCallback(ignored -> calls.add("primary"));
        setting.addValueChangeListener(ignored -> calls.add("listener"));

        setting.setBooleanValue(true);
        setting.setBooleanValue(true);

        assertEquals(List.of("primary", "listener"), calls);
    }

    @Test
    void refusedWriteDoesNotEmitFalseChangeNotification() {
        RefusingSetting setting = new RefusingSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.setValueChangeCallback(ignored -> calls.add("primary"));
        setting.addValueChangeListener(ignored -> calls.add("listener"));

        setting.setBooleanValue(true);

        assertFalse(setting.getBooleanValue());
        assertEquals(List.of(), calls);
    }

    @Test
    void applyPersistenceIsDefaultButCanBeOwnedExternally() {
        assertTrue(new TestSetting().requiresApplyPersistence());
        assertFalse(new ImmediateSetting().requiresApplyPersistence());
    }

    @Test
    void replacingPrimaryCallbackDoesNotDeleteIndependentListeners() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.addValueChangeListener(ignored -> calls.add("listener"));
        setting.setValueChangeCallback(ignored -> calls.add("first"));
        setting.setValueChangeCallback(ignored -> calls.add("replacement"));

        setting.setBooleanValue(true);

        assertEquals(List.of("replacement", "listener"), calls);
    }

    @Test
    void primaryFailureDoesNotPreventIndependentListenerNotification() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.setValueChangeCallback(ignored -> {
            calls.add("primary");
            throw new IllegalStateException("primary failure");
        });
        setting.addValueChangeListener(ignored -> calls.add("listener"));

        assertThrows(IllegalStateException.class, () -> setting.setBooleanValue(true));
        assertEquals(List.of("primary", "listener"), calls);
    }

    @Test
    void failingIndependentListenerDoesNotBlockLaterListeners() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.addValueChangeListener(ignored -> {
            calls.add("first");
            throw new IllegalStateException("listener failure");
        });
        setting.addValueChangeListener(ignored -> calls.add("second"));

        assertThrows(IllegalStateException.class, () -> setting.setBooleanValue(true));
        assertEquals(List.of("first", "second"), calls);
    }

    private static class TestSetting extends ChiseBooleanSetting {
        private boolean value;

        TestSetting() {
            super("test", false, "Test", "テスト", "test", "テスト");
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
        @Override
        public boolean requiresApplyPersistence() {
            return false;
        }
    }
}
