package dev.chise.chisetweaks.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.util.FormattedCharSequence;

final class ChiseTweaksSettingRowView {
    final ChiseTweaksSettingRowDefinition definition;
    final Button primary;
    final Button minus;
    final Button value;
    final Button plus;
    ChiseTweaksInfoTextLayout.Layout<FormattedCharSequence> infoTextLayout;
    int screenY;
    boolean renderVisible;

    ChiseTweaksSettingRowView(
            ChiseTweaksSettingRowDefinition definition,
            Button primary,
            Button minus,
            Button value,
            Button plus) {
        this.definition = definition;
        this.primary = primary;
        this.minus = minus;
        this.value = value;
        this.plus = plus;
    }

    static ChiseTweaksSettingRowView header(
            ChiseTweaksSettingRowDefinition definition,
            Button actionButton) {
        return new ChiseTweaksSettingRowView(definition, actionButton, null, null, null);
    }

    void setWidgetsVisible(boolean visible) {
        if (primary != null) primary.visible = visible;
        if (minus != null) minus.visible = visible;
        if (value != null) value.visible = visible;
        if (plus != null) plus.visible = visible;
    }

    void removeWidgets(java.util.function.Consumer<Button> remover) {
        if (remover == null) return;
        if (primary != null) remover.accept(primary);
        if (minus != null) remover.accept(minus);
        if (value != null) remover.accept(value);
        if (plus != null) remover.accept(plus);
    }
}
