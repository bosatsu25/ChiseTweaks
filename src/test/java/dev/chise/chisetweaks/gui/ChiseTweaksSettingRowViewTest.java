package dev.chise.chisetweaks.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChiseTweaksSettingRowViewTest {
    @Test
    void oneHundredInspectorRowReplacementsDoNotAccumulateOwnedWidgets() {
        List<Button> host = new ArrayList<>();
        ChiseTweaksSettingRowDefinition definition =
                ChiseTweaksSettingRowDefinition.info("test.row", "Test", "");
        ChiseTweaksSettingRowView previous = null;

        for (int rebuild = 0; rebuild < 100; rebuild++) {
            if (previous != null) previous.removeWidgets(host::remove);

            Button primary = button("primary");
            Button minus = button("minus");
            Button value = button("value");
            Button plus = button("plus");
            previous = new ChiseTweaksSettingRowView(
                    definition, primary, minus, value, plus);
            host.addAll(List.of(primary, minus, value, plus));

            assertEquals(4, host.size(), "widget count leaked at rebuild " + rebuild);
        }

        previous.removeWidgets(host::remove);
        assertTrue(host.isEmpty());
    }

    private static Button button(String label) {
        return Button.builder(Component.literal(label), ignored -> {})
                .bounds(0, 0, 24, 18)
                .build();
    }
}
