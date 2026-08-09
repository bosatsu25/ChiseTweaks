package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Locale;

/** Standalone ChiseTweaks settings UI. It does not depend on another mod's config surface. */
public final class ChiseTweaksConfigScreen extends Screen {
    private static ChiseTweaksUiSection selectedSection = ChiseTweaksUiSection.PLACEMENT;

    private final ChiseTweaksSettingsController controller;
    private final ArrayList<ChiseTweaksSettingRowView> rows = new ArrayList<>();
    private final ArrayList<ChiseTweaksSettingRowView> filteredRows = new ArrayList<>();

    private Screen parent;
    private ChiseTweaksSettingsLayout.Geometry geometry;
    private EditBox searchBox;
    private Button bulkButton;
    private Button applyButton;
    private String searchQuery = "";
    private int scrollOffset;
    private int maxScroll;
    private boolean dirty;

    public ChiseTweaksConfigScreen() {
        this(selectedSection);
    }

    public ChiseTweaksConfigScreen(ChiseTweaksUiSection initialSection) {
        super(Component.literal(ChiseTweaksMetadata.MOD_NAME));
        this.controller = ChiseTweaksSettingsController.forCurrentLanguage();
        if (initialSection != null && initialSection != ChiseTweaksUiSection.HELP) {
            selectedSection = initialSection;
        }
    }

    public void setParent(Screen parent) {
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        controller.initialize();
        geometry = ChiseTweaksSettingsLayout.calculate(width, height, true);

        createNavigation();
        createSearch();
        createFooter();
        createRows();
        rebuildFilteredRows();
        refreshDescriptionCache();
        updateRowPositions();
        refreshRowButtons();
    }

    private void createNavigation() {
        int x = geometry.navigation().x();
        for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values()) {
            Button button = addRenderableWidget(Button.builder(
                    Component.literal(section.getDisplayName()), ignored -> navigate(section))
                    .bounds(x, geometry.navigation().y(), geometry.navButtonWidth(), 20)
                    .build());
            button.active = section == ChiseTweaksUiSection.HELP || section != selectedSection;
            x += geometry.navButtonWidth() + geometry.navGap();
        }
    }

    private void createSearch() {
        searchBox = addRenderableWidget(new EditBox(
                font,
                geometry.search().x(),
                geometry.search().y(),
                geometry.search().width(),
                geometry.search().height(),
                Component.literal("検索")));
        searchBox.setHint(Component.literal(controller.japanese() ? "検索..." : "Search..."));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            searchQuery = value == null ? "" : value;
            scrollOffset = 0;
            rebuildFilteredRows();
            updateRowPositions();
        });

        bulkButton = addRenderableWidget(Button.builder(
                bulkMessage(), ignored -> toggleBulk())
                .bounds(
                        geometry.bulk().x(),
                        geometry.bulk().y(),
                        geometry.bulk().width(),
                        geometry.bulk().height())
                .build());
    }

    private void createFooter() {
        int y = geometry.footer().y();
        int left = geometry.footer().x();
        int right = geometry.footer().right();

        addRenderableWidget(Button.builder(
                Component.literal(controller.japanese() ? "設定をリセット" : "Reset section"),
                ignored -> resetCurrentSection())
                .bounds(left, y, 112, 20)
                .build());

        applyButton = addRenderableWidget(Button.builder(
                Component.literal(controller.japanese() ? "適用" : "Apply"),
                ignored -> applyChanges())
                .bounds(left + 122, y, 92, 20)
                .build());
        applyButton.active = dirty;

        addRenderableWidget(Button.builder(
                Component.literal(controller.japanese() ? "完了" : "Done"),
                ignored -> onClose())
                .bounds(right - 132, y, 132, 20)
                .build());
    }

    private void createRows() {
        rows.clear();
        for (ChiseTweaksSettingRowDefinition definition : controller.rowsFor(selectedSection)) {
            rows.add(createRow(definition));
        }
    }

    private ChiseTweaksSettingRowView createRow(ChiseTweaksSettingRowDefinition definition) {
        return switch (definition.kind()) {
            case HEADER -> ChiseTweaksSettingRowView.header(definition);
            case BOOLEAN -> createBooleanRow(definition);
            case INTEGER -> createIntegerRow(definition);
        };
    }

    private ChiseTweaksSettingRowView createBooleanRow(ChiseTweaksSettingRowDefinition definition) {
        ChiseBooleanSetting config = definition.booleanConfig();
        Button button = addRenderableWidget(Button.builder(toggleMessage(config), ignored -> {
            config.toggleBooleanValue();
            markDirty();
        }).bounds(0, 0, geometry.controlWidth(), 18).build());
        return new ChiseTweaksSettingRowView(definition, button, null, null, null);
    }

    private ChiseTweaksSettingRowView createIntegerRow(ChiseTweaksSettingRowDefinition definition) {
        var config = definition.integerConfig();
        int step = definition.step();
        Button minus = addRenderableWidget(Button.builder(Component.literal("−"), ignored -> {
            config.setIntegerValue(config.getIntegerValue() - step);
            markDirty();
        }).bounds(0, 0, 24, 18).build());
        Button value = addRenderableWidget(Button.builder(
                Component.literal(Integer.toString(config.getIntegerValue())), ignored -> {})
                .bounds(0, 0, 54, 18)
                .build());
        value.active = false;
        Button plus = addRenderableWidget(Button.builder(Component.literal("+"), ignored -> {
            config.setIntegerValue(config.getIntegerValue() + step);
            markDirty();
        }).bounds(0, 0, 24, 18).build());
        return new ChiseTweaksSettingRowView(definition, null, minus, value, plus);
    }

    private void navigate(ChiseTweaksUiSection section) {
        if (minecraft == null || section == null) return;
        if (section == ChiseTweaksUiSection.HELP) {
            applyChanges();
            minecraft.setScreen(new ChiseTweaksHelpScreen(this));
            return;
        }
        if (section == selectedSection) return;
        applyChanges();
        selectedSection = section;
        ChiseTweaksConfigScreen next = new ChiseTweaksConfigScreen(section);
        next.setParent(parent);
        minecraft.setScreen(next);
    }

    private void toggleBulk() {
        controller.toggleBulk(selectedSection);
        markDirty();
    }

    private void resetCurrentSection() {
        if (!controller.resetSection(selectedSection)) return;
        markDirty();
    }

    private void markDirty() {
        dirty = true;
        refreshRowButtons();
    }

    private void applyChanges() {
        if (!dirty) return;
        controller.saveFeatureConfig();
        dirty = false;
        if (applyButton != null) applyButton.active = false;
    }

    @Override
    public void onClose() {
        applyChanges();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount) {
        if (geometry != null
                && mouseX >= geometry.panel().x()
                && mouseX <= geometry.panel().right()
                && mouseY >= geometry.panel().y()
                && mouseY <= geometry.panel().bottom()) {
            scrollOffset = Mth.clamp(scrollOffset + (int) (-verticalAmount * 30), 0, maxScroll);
            updateRowPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        if (geometry == null) return;
        var panel = geometry.panel();
        extractor.fill(panel.x(), panel.y(), panel.right(), panel.bottom(), 0xC8121212);
        extractor.fill(panel.x(), panel.y(), panel.right(), panel.y() + 1, 0xFF808080);
        extractor.fill(panel.x(), panel.bottom() - 1, panel.right(), panel.bottom(), 0xFF4C4C4C);

        for (ChiseTweaksSettingRowView row : filteredRows) {
            if (!row.renderVisible || row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) continue;
            int y = row.screenY;
            extractor.fill(
                    panel.x() + 8,
                    y,
                    panel.right() - 24,
                    y + geometry.rowHeight() - 2,
                    0x8A202020);
            extractor.fill(
                    panel.x() + 8,
                    y + geometry.rowHeight() - 3,
                    panel.right() - 24,
                    y + geometry.rowHeight() - 2,
                    0x553F3F3F);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        if (geometry == null) return;

        extractor.text(
                font,
                ChiseTweaksMetadata.MOD_NAME + " " + ChiseTweaksMetadata.MOD_VERSION,
                geometry.content().x(),
                14,
                0xFFFFFFFF);

        for (ChiseTweaksSettingRowView row : filteredRows) {
            if (!row.renderVisible) continue;
            if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
                extractor.text(
                        font,
                        row.definition.name(),
                        geometry.panel().x() + 12,
                        row.screenY + 6,
                        0xFF78AFFF);
                continue;
            }
            renderRowText(extractor, row);
        }
        renderScrollbar(extractor);
    }

    private void renderRowText(GuiGraphicsExtractor extractor, ChiseTweaksSettingRowView row) {
        int lineHeight = font.lineHeight;
        if (geometry.stackedText()) {
            int nameY = row.screenY + 5;
            drawCenteredText(
                    extractor,
                    row.definition.name(),
                    geometry.nameX(),
                    geometry.nameWidth(),
                    nameY,
                    0xFFFFFFFF);

            int descriptionLines = row.renderedDescriptionLineCount();
            int descriptionY = row.screenY + (descriptionLines > 1 ? 22 : 27);
            drawDescriptionLines(extractor, row, descriptionY, lineHeight);
            return;
        }

        int nameY = row.screenY + Math.max(0, (geometry.rowHeight() - lineHeight) / 2);
        drawCenteredText(
                extractor,
                row.definition.name(),
                geometry.nameX(),
                geometry.nameWidth(),
                nameY,
                0xFFFFFFFF);

        int descriptionLines = row.renderedDescriptionLineCount();
        int descriptionHeight = Math.max(1, descriptionLines) * lineHeight;
        int descriptionY = row.screenY + Math.max(0, (geometry.rowHeight() - descriptionHeight) / 2);
        drawDescriptionLines(extractor, row, descriptionY, lineHeight);
    }

    private void drawDescriptionLines(
            GuiGraphicsExtractor extractor,
            ChiseTweaksSettingRowView row,
            int firstY,
            int lineHeight) {
        drawCenteredText(
                extractor,
                row.renderedDescriptionLine1,
                geometry.descriptionX(),
                geometry.descriptionWidth(),
                firstY,
                0xFFC8C8C8);
        if (!row.renderedDescriptionLine2.isEmpty()) {
            drawCenteredText(
                    extractor,
                    row.renderedDescriptionLine2,
                    geometry.descriptionX(),
                    geometry.descriptionWidth(),
                    firstY + lineHeight,
                    0xFFC8C8C8);
        }
    }

    private void drawCenteredText(
            GuiGraphicsExtractor extractor,
            String text,
            int regionX,
            int regionWidth,
            int y,
            int color) {
        if (text == null || text.isEmpty()) return;
        int x = regionX + Math.max(0, (regionWidth - font.width(text)) / 2);
        extractor.text(font, text, x, y, color);
    }

    private void renderScrollbar(GuiGraphicsExtractor extractor) {
        int viewport = Math.max(1, geometry.panelContentBottom() - geometry.panelContentTop());
        if (maxScroll <= 0) return;
        int trackX = geometry.panel().right() - 10;
        int trackTop = geometry.panelContentTop();
        int trackBottom = geometry.panelContentBottom();
        extractor.fill(trackX, trackTop, trackX + 5, trackBottom, 0xAA101010);
        int content = viewport + maxScroll;
        int thumbHeight = Math.max(24, viewport * viewport / content);
        int travel = viewport - thumbHeight;
        int thumbY = trackTop + scrollOffset * travel / maxScroll;
        extractor.fill(trackX, thumbY, trackX + 5, thumbY + thumbHeight, 0xFFD0D0D0);
    }

    private void rebuildFilteredRows() {
        filteredRows.clear();
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            filteredRows.addAll(rows);
            return;
        }

        ChiseTweaksSettingRowView pendingHeader = null;
        boolean headerAdded = false;
        for (ChiseTweaksSettingRowView row : rows) {
            if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
                pendingHeader = row;
                headerAdded = false;
                continue;
            }
            if (!row.searchableText.contains(query)) continue;
            if (pendingHeader != null && !headerAdded) {
                filteredRows.add(pendingHeader);
                headerAdded = true;
            }
            filteredRows.add(row);
        }
    }

    private void refreshDescriptionCache() {
        for (ChiseTweaksSettingRowView row : rows) {
            row.cacheDescription(ChiseTweaksRowTextLayout.wrap(
                    row.definition.description(),
                    geometry.descriptionWidth(),
                    font::width));
        }
    }

    private void updateRowPositions() {
        if (geometry == null) return;
        int viewportTop = geometry.panelContentTop();
        int viewportBottom = geometry.panelContentBottom();
        int viewport = Math.max(1, viewportBottom - viewportTop);
        int contentHeight = contentHeight();
        maxScroll = Math.max(0, contentHeight - viewport);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        for (ChiseTweaksSettingRowView row : rows) {
            row.renderVisible = false;
            row.setWidgetsVisible(false);
        }

        int offset = 0;
        for (ChiseTweaksSettingRowView row : filteredRows) {
            int rowHeight = row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER
                    ? geometry.headerHeight()
                    : geometry.rowHeight();
            int y = viewportTop + offset - scrollOffset;
            row.screenY = y;
            row.renderVisible = y >= viewportTop && y + rowHeight <= viewportBottom;
            if (row.renderVisible && row.definition.kind() != ChiseTweaksSettingRowDefinition.Kind.HEADER) {
                positionWidgets(row, y);
            }
            offset += rowHeight;
        }
    }

    private int contentHeight() {
        int height = 0;
        for (ChiseTweaksSettingRowView row : filteredRows) {
            height += row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER
                    ? geometry.headerHeight()
                    : geometry.rowHeight();
        }
        return height;
    }

    private void positionWidgets(ChiseTweaksSettingRowView row, int y) {
        int controlY = y + Math.max(0, (geometry.rowHeight() - 18) / 2);
        switch (row.definition.kind()) {
            case BOOLEAN -> {
                row.primary.setPosition(geometry.booleanControlX(), controlY);
                row.primary.visible = true;
            }
            case INTEGER -> {
                int x = geometry.controlX();
                row.minus.setPosition(x, controlY);
                row.value.setPosition(x + 28, controlY);
                row.plus.setPosition(x + 86, controlY);
                row.minus.visible = true;
                row.value.visible = true;
                row.plus.visible = true;
            }
            case HEADER -> { }
        }
    }

    private void refreshRowButtons() {
        for (ChiseTweaksSettingRowView row : rows) {
            if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.BOOLEAN
                    && row.primary != null
                    && row.definition.booleanConfig() != null) {
                row.primary.setMessage(toggleMessage(row.definition.booleanConfig()));
            } else if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.INTEGER
                    && row.value != null
                    && row.definition.integerConfig() != null) {
                row.value.setMessage(Component.literal(Integer.toString(
                        row.definition.integerConfig().getIntegerValue())));
            }
        }
        if (bulkButton != null) bulkButton.setMessage(bulkMessage());
        if (applyButton != null) applyButton.active = dirty;
    }

    private Component bulkMessage() {
        boolean turnOn = controller.shouldTurnBulkOn(selectedSection);
        return Component.literal(
                (controller.japanese() ? "一括選択：" : "Select all: ") + (turnOn ? "ON" : "OFF"));
    }

    private static Component toggleMessage(ChiseBooleanSetting value) {
        boolean enabled = value.getBooleanValue();
        return Component.literal(enabled ? "ON" : "OFF")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
