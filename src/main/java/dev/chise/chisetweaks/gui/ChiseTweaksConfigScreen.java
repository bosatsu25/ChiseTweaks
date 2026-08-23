package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;

public final class ChiseTweaksConfigScreen extends Screen {
    private final ChiseTweaksSettingsController controller;
    private final ChiseTweaksSettingsController.Surface surface;
    private final ArrayList<ChiseTweaksSettingRowView> rows = new ArrayList<>();
    private Screen parent;
    private ChiseTweaksSettingsLayout.Geometry geometry;
    private Button bulkButton;
    private Button applyButton;
    private String persistenceFeedback = "";
    private int scrollOffset;
    private int maxScroll;
    private boolean dirty;

    public ChiseTweaksConfigScreen() {
        this(ChiseTweaksSettingsController.Surface.MAIN);
    }

    private ChiseTweaksConfigScreen(ChiseTweaksSettingsController.Surface surface) {
        super(Component.literal(ChiseTweaksMetadata.MOD_NAME));
        this.surface = surface == null ? ChiseTweaksSettingsController.Surface.MAIN : surface;
        this.controller = ChiseTweaksSettingsController.forCurrentLanguage();
    }

    public void setParent(Screen parent) {
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        controller.initialize();
        geometry = ChiseTweaksSettingsLayout.calculate(width, height);
        createFooter();
        createRows();
        if (surface == ChiseTweaksSettingsController.Surface.MAIN) createHighlightBulkButton();
        updateRowPositions();
        refreshRowButtons();
    }

    private void createHighlightBulkButton() {
        bulkButton = addRenderableWidget(Button.builder(
                bulkMessage(), ignored -> toggleHighlightBulk())
                .bounds(
                        geometry.bulk().x(),
                        geometry.bulk().y(),
                        geometry.bulk().width(),
                        geometry.bulk().height())
                .build());
        bulkButton.active = PreReleaseUiPolicy.isHighlightBulkInteractive();
        bulkButton.visible = false;
    }

    private void createFooter() {
        var help = geometry.helpButton();
        var reset = geometry.resetButton();
        var apply = geometry.applyButton();
        var done = geometry.doneButton();
        if (surface == ChiseTweaksSettingsController.Surface.MAIN) {
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.chisetweaks.settings.guide"),
                    ignored -> openHelp())
                    .bounds(help.x(), help.y(), help.width(), help.height())
                    .build());
        } else {
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.chisetweaks.common.back"),
                    ignored -> onClose())
                    .bounds(help.x(), help.y(), help.width(), help.height())
                    .build());
        }
        addRenderableWidget(Button.builder(
                Component.translatable(surface == ChiseTweaksSettingsController.Surface.MAIN
                        ? "screen.chisetweaks.settings.reset_all"
                        : "screen.chisetweaks.settings.reset"),
                ignored -> resetCurrentSurface())
                .bounds(reset.x(), reset.y(), reset.width(), reset.height())
                .build());
        applyButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.settings.apply"),
                ignored -> applyChanges())
                .bounds(apply.x(), apply.y(), apply.width(), apply.height())
                .build());
        applyButton.active = dirty;
        addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.settings.done"),
                ignored -> onClose())
                .bounds(done.x(), done.y(), done.width(), done.height())
                .build());
    }

    private void createRows() {
        rows.clear();
        for (ChiseTweaksSettingRowDefinition definition : controller.rows(surface)) {
            rows.add(createRow(definition));
        }
    }

    private ChiseTweaksSettingRowView createRow(ChiseTweaksSettingRowDefinition definition) {
        ChiseTweaksSettingRowView row = switch (definition.kind()) {
            case HEADER -> createHeaderRow(definition);
            case BOOLEAN -> createBooleanRow(definition);
            case BOOLEAN_ACTION -> createBooleanActionRow(definition);
            case INTEGER -> createIntegerRow(definition);
            case ACTION -> createActionRow(definition);
        };
        applyPrereleaseInteractivity(row);
        return row;
    }

    private void applyPrereleaseInteractivity(ChiseTweaksSettingRowView row) {
        boolean interactive = PreReleaseUiPolicy.isRowInteractive(surface, row.definition);
        if (row.primary != null) row.primary.active = interactive;
        if (row.secondary != null) row.secondary.active = interactive;
        if (row.minus != null) row.minus.active = interactive;
        if (row.plus != null) row.plus.active = interactive;
        if (row.value != null) row.value.active = false;
    }

    private ChiseTweaksSettingRowView createHeaderRow(ChiseTweaksSettingRowDefinition definition) {
        Button actionButton = definition.action() == null ? null : addRenderableWidget(Button.builder(
                Component.literal(definition.actionLabel()), ignored -> runRowAction(definition.action()))
                .bounds(0, 0, geometry.actionWidth(), 18)
                .build());
        if (actionButton != null) actionButton.visible = false;
        return ChiseTweaksSettingRowView.header(definition, actionButton);
    }

    private ChiseTweaksSettingRowView createBooleanRow(ChiseTweaksSettingRowDefinition definition) {
        ChiseBooleanSetting config = definition.booleanConfig();
        Button button = addRenderableWidget(Button.builder(toggleMessage(config), ignored -> {
            config.toggleBooleanValue();
            markDirty();
        }).bounds(0, 0, geometry.toggleWidth(), 18).build());
        return new ChiseTweaksSettingRowView(definition, button, null, null, null, null);
    }

    private ChiseTweaksSettingRowView createBooleanActionRow(ChiseTweaksSettingRowDefinition definition) {
        ChiseBooleanSetting config = definition.booleanConfig();
        Button toggle = addRenderableWidget(Button.builder(toggleMessage(config), ignored -> {
            config.toggleBooleanValue();
            markDirty();
        }).bounds(0, 0, geometry.toggleWidth(), 18).build());
        Button action = addRenderableWidget(Button.builder(
                Component.literal(definition.actionLabel()), ignored -> runRowAction(definition.action()))
                .bounds(0, 0, geometry.actionWidth(), 18)
                .build());
        return new ChiseTweaksSettingRowView(definition, toggle, action, null, null, null);
    }

    private ChiseTweaksSettingRowView createIntegerRow(ChiseTweaksSettingRowDefinition definition) {
        var config = definition.integerConfig();
        int step = definition.step();
        Button minus = addRenderableWidget(Button.builder(Component.literal("−"), ignored -> {
            config.setIntegerValue(saturatedStep(config.getIntegerValue(), -step));
            markDirty();
        }).bounds(0, 0, 24, 18).build());
        Button value = addRenderableWidget(Button.builder(
                Component.literal(config.getFormattedValue()), ignored -> {})
                .bounds(0, 0, 54, 18)
                .build());
        value.active = false;
        Button plus = addRenderableWidget(Button.builder(Component.literal("+"), ignored -> {
            config.setIntegerValue(saturatedStep(config.getIntegerValue(), step));
            markDirty();
        }).bounds(0, 0, 24, 18).build());
        return new ChiseTweaksSettingRowView(definition, null, null, minus, value, plus);
    }

    private ChiseTweaksSettingRowView createActionRow(ChiseTweaksSettingRowDefinition definition) {
        return new ChiseTweaksSettingRowView(
                definition,
                addRenderableWidget(Button.builder(Component.literal(definition.actionLabel()),
                        ignored -> runRowAction(definition.action()))
                        .bounds(0, 0, geometry.actionWidth(), 18).build()),
                null, null, null, null);
    }

    private void runRowAction(ChiseTweaksSettingRowDefinition.Action action) {
        if (!PreReleaseUiPolicy.isActionInteractive(surface, action)) return;
        if (minecraft == null || action == null || !applyChanges()) return;
        switch (action) {
            case OPEN_HIGHLIGHT_DETAILS -> openDetail(ChiseTweaksSettingsController.Surface.HIGHLIGHT_DETAILS);
            case OPEN_LAVA_DETAILS -> openDetail(ChiseTweaksSettingsController.Surface.LAVA_DETAILS);
            case EDIT_BLOCK_FILTER -> minecraft.setScreen(new ChiseSceneFilterEditorScreen(
                    this, ChiseSceneFilterEditorScreen.Target.BLOCKS));
            case EDIT_ENTITY_FILTER -> minecraft.setScreen(new ChiseSceneFilterEditorScreen(
                    this, ChiseSceneFilterEditorScreen.Target.ENTITIES));
            case EDIT_ORE_COMPAT -> minecraft.setScreen(new ChiseOreCompatibilityScreen(this));
        }
    }

    private void openDetail(ChiseTweaksSettingsController.Surface target) {
        if (minecraft == null) return;
        ChiseTweaksConfigScreen detail = new ChiseTweaksConfigScreen(target);
        detail.setParent(this);
        minecraft.setScreen(detail);
    }

    private void openHelp() {
        if (minecraft == null || !applyChanges()) return;
        minecraft.setScreen(new ChiseTweaksHelpScreen(this));
    }

    private void toggleHighlightBulk() {
        if (!PreReleaseUiPolicy.isHighlightBulkInteractive()) return;
        controller.toggleHighlightBulk();
        markDirty();
    }

    private void resetCurrentSurface() {
        if (!controller.reset(surface)) return;
        markDirty();
    }

    private void markDirty() {
        dirty = true;
        persistenceFeedback = "";
        refreshRowButtons();
    }

    private boolean applyChanges() {
        if (!dirty) return true;
        if (!controller.saveConfig()) {
            persistenceFeedback = text("screen.chisetweaks.settings.save_failed");
            if (applyButton != null) applyButton.active = true;
            return false;
        }
        persistenceFeedback = "";
        dirty = false;
        if (applyButton != null) applyButton.active = false;
        return true;
    }

    @Override
    public void onClose() {
        if (!applyChanges()) return;
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
        for (ChiseTweaksSettingRowView row : rows) {
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
        String detailTitle = controller.surfaceTitle(surface);
        String title = ChiseTweaksMetadata.MOD_NAME + " " + ChiseTweaksMetadata.MOD_VERSION
                + (detailTitle.isEmpty() ? "" : " - " + detailTitle);
        extractor.text(font, title, geometry.content().x(), 14, 0xFFFFFFFF);
        for (ChiseTweaksSettingRowView row : rows) {
            if (!row.renderVisible) continue;
            if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
                extractor.text(
                        font,
                        row.definition.name(),
                        geometry.panel().x() + 12,
                        row.screenY + 7,
                        0xFF78AFFF);
                continue;
            }
            int y = row.screenY + Math.max(0, (geometry.rowHeight() - font.lineHeight) / 2);
            int color = PreReleaseUiPolicy.isRowInteractive(surface, row.definition)
                    ? 0xFFFFFFFF
                    : 0xFF7A7A7A;
            extractor.text(font, row.definition.name(), geometry.nameX(), y, color);
        }
        if (!persistenceFeedback.isEmpty()) {
            extractor.centeredText(
                    font,
                    Component.literal(persistenceFeedback),
                    width / 2,
                    Math.max(24, height - 48),
                    0xFFFFD166);
        }
        renderScrollbar(extractor);
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
        for (ChiseTweaksSettingRowView row : rows) {
            int rowHeight = row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER
                    ? geometry.headerHeight()
                    : geometry.rowHeight();
            int y = viewportTop + offset - scrollOffset;
            row.screenY = y;
            row.renderVisible = y >= viewportTop && y + rowHeight <= viewportBottom;
            if (row.renderVisible) positionWidgets(row, y);
            offset += rowHeight;
        }
        positionHighlightBulkButton();
    }

    private void positionHighlightBulkButton() {
        if (bulkButton == null) return;
        bulkButton.visible = false;
        for (ChiseTweaksSettingRowView row : rows) {
            if (!"header.highlight".equals(row.definition.id())) continue;
            if (!row.renderVisible) return;
            bulkButton.setPosition(geometry.bulk().x(), row.screenY + 3);
            bulkButton.visible = true;
            return;
        }
    }

    private int contentHeight() {
        int contentHeight = 0;
        for (ChiseTweaksSettingRowView row : rows) {
            contentHeight += row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER
                    ? geometry.headerHeight()
                    : geometry.rowHeight();
        }
        return contentHeight;
    }

    private void positionWidgets(ChiseTweaksSettingRowView row, int y) {
        int controlY = y + Math.max(0, (geometry.rowHeight() - 18) / 2);
        switch (row.definition.kind()) {
            case HEADER -> {
                if (row.primary != null) {
                    row.primary.setPosition(geometry.headerAction().x(), y + 3);
                    row.primary.visible = true;
                }
            }
            case BOOLEAN -> {
                row.primary.setPosition(geometry.toggleX(), controlY);
                row.primary.visible = true;
            }
            case BOOLEAN_ACTION -> {
                row.secondary.setPosition(geometry.actionX(), controlY);
                row.primary.setPosition(geometry.toggleX(), controlY);
                row.secondary.visible = true;
                row.primary.visible = true;
            }
            case INTEGER -> {
                int x = geometry.integerX();
                row.minus.setPosition(x, controlY);
                row.value.setPosition(x + 28, controlY);
                row.plus.setPosition(x + 86, controlY);
                row.minus.visible = true;
                row.value.visible = true;
                row.plus.visible = true;
            }
            case ACTION -> {
                int x = geometry.toggleX() + geometry.toggleWidth() - geometry.actionWidth();
                row.primary.setPosition(x, controlY);
                row.primary.visible = true;
            }
        }
    }

    private void refreshRowButtons() {
        for (ChiseTweaksSettingRowView row : rows) {
            if ((row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.BOOLEAN
                    || row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.BOOLEAN_ACTION)
                    && row.primary != null
                    && row.definition.booleanConfig() != null) {
                row.primary.setMessage(toggleMessage(row.definition.booleanConfig()));
            } else if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.INTEGER
                    && row.value != null
                    && row.definition.integerConfig() != null) {
                row.value.setMessage(Component.literal(
                        row.definition.integerConfig().getFormattedValue()));
            }
        }
        if (bulkButton != null) {
            bulkButton.setMessage(bulkMessage());
            bulkButton.active = PreReleaseUiPolicy.isHighlightBulkInteractive();
        }
        if (applyButton != null) applyButton.active = dirty;
    }

    private Component bulkMessage() {
        return Component.translatable(controller.shouldTurnHighlightBulkOn()
                ? "screen.chisetweaks.settings.bulk.on"
                : "screen.chisetweaks.settings.bulk.off");
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
    }

    private static int saturatedStep(int current, int step) {
        long next = (long) current + step;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, next));
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
