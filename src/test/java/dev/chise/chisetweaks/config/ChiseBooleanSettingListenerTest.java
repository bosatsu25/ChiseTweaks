package dev.chise.chisetweaks.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void replacingPrimaryCallbackDoesNotDeleteIndependentListeners() {
        TestSetting setting = new TestSetting();
        ArrayList<String> calls = new ArrayList<>();
        setting.addValueChangeListener(ignored -> calls.add("listener"));
        setting.setValueChangeCallback(ignored -> calls.add("first"));
        setting.setValueChangeCallback(ignored -> calls.add("replacement"));

        setting.setBooleanValue(true);

        assertEquals(List.of("replacement", "listener"), calls);
    }

    private static final class TestSetting extends ChiseBooleanSetting {
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
}
