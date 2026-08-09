package dev.chise.chisetweaks.gui;

import net.minecraft.client.gui.components.Button;

import java.util.Locale;

/** Mutable widget/render state for one immutable settings row definition. */
final class ChiseTweaksSettingRowView {
    final ChiseTweaksSettingRowDefinition definition;
    final Button primary;
    final Button minus;
    final Button value;
    final Button plus;
    final String searchableText;
    int screenY;
    boolean renderVisible;
    String renderedDescriptionLine1 = "";
    String renderedDescriptionLine2 = "";

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
        this.searchableText = normalizeSearchText(definition);
    }

    static ChiseTweaksSettingRowView header(ChiseTweaksSettingRowDefinition definition) {
        return new ChiseTweaksSettingRowView(definition, null, null, null, null);
    }

    void cacheDescription(ChiseTweaksRowTextLayout.WrappedLines lines) {
        renderedDescriptionLine1 = lines == null ? "" : lines.first();
        renderedDescriptionLine2 = lines == null ? "" : lines.second();
    }

    int renderedDescriptionLineCount() {
        if (renderedDescriptionLine1.isEmpty()) return 0;
        return renderedDescriptionLine2.isEmpty() ? 1 : 2;
    }

    void setWidgetsVisible(boolean visible) {
        if (primary != null) primary.visible = visible;
        if (minus != null) minus.visible = visible;
        if (value != null) value.visible = visible;
        if (plus != null) plus.visible = visible;
    }

    private static String normalizeSearchText(ChiseTweaksSettingRowDefinition definition) {
        return (definition.name() + " " + definition.description())
                .toLowerCase(Locale.ROOT);
    }
}
