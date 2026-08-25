package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.ChiseBooleanSetting;
import dev.chise.chisetweaks.config.SettingPersistence;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ChiseTweaksConfigScreen extends Screen {
    private final ChiseTweaksSettingsController controller;
    private final EnumMap<ChiseTweaksSettingsController.Surface, ArrayList<ChiseTweaksSettingRowView>> rowsBySurface =
            new EnumMap<>(ChiseTweaksSettingsController.Surface.class);
    private final EnumSet<SettingPersistence> dirtyDomains = EnumSet.noneOf(SettingPersistence.class);
    private final ArrayList<Button> tabButtons = new ArrayList<>();
    private final CrosshairInspector inspector = new CrosshairInspector();
    private Screen parent;
    private ChiseTweaksSettingsController.Surface surface = ChiseTweaksSettingsController.Surface.HIGHLIGHT;
    private ChiseTweaksSettingsLayout.Geometry geometry;
    private Button contextButton;
    private String persistenceFeedback = "";
    private int scrollOffset;
    private int maxScroll;
    private boolean inspectorHelpVisible;
    private long patternRevision = Long.MIN_VALUE;

    public ChiseTweaksConfigScreen() {
        super(Component.literal(ChiseTweaksMetadata.MOD_NAME));
        this.controller = ChiseTweaksSettingsController.forCurrentLanguage();
    }

    public void setParent(Screen parent) {
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        controller.initialize();
        inspector.refresh(minecraft);
        geometry = ChiseTweaksSettingsLayout.calculate(width, height);
        createTabs();
        createFooter();
        createAllRows();
        selectSurface(surface, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (surface == ChiseTweaksSettingsController.Surface.INSPECTOR) {
            boolean changed = inspector.refresh(minecraft);
            long currentPatternRevision = PatternConsistencyInspector.currentRevision();
            if (currentPatternRevision != patternRevision) {
                patternRevision = currentPatternRevision;
                changed = true;
            }
            if (changed) rebuildInspectorRows();
        }
        refreshRowButtons();
    }

    private void createTabs() {
        tabButtons.clear();
        ChiseTweaksSettingsController.Surface[] surfaces = ChiseTweaksSettingsController.Surface.values();
        List<ChiseTweaksSettingsLayout.Rect> tabs = geometry.tabs();
        for (int index = 0; index < surfaces.length; index++) {
            ChiseTweaksSettingsController.Surface target = surfaces[index];
            ChiseTweaksSettingsLayout.Rect bounds = tabs.get(index);
            Button tab = addRenderableWidget(Button.builder(
                    Component.literal(controller.surfaceTitle(target)),
                    ignored -> selectSurface(target, true))
                    .bounds(bounds.x(), bounds.y(), bounds.width(), bounds.height())
                    .build());
            tabButtons.add(tab);
        }
    }

    private void createFooter() {
        var context = geometry.contextButton();
        var done = geometry.doneButton();
        contextButton = addRenderableWidget(Button.builder(
                Component.literal("設定をリセット"),
                ignored -> runContextAction())
                .bounds(context.x(), context.y(), context.width(), context.height())
                .build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.settings.done"),
                ignored -> onClose())
                .bounds(done.x(), done.y(), done.width(), done.height())
                .build());
    }

    private void createAllRows() {
        rowsBySurface.clear();
        for (ChiseTweaksSettingsController.Surface candidate : ChiseTweaksSettingsController.Surface.values()) {
            ArrayList<ChiseTweaksSettingRowView> views = new ArrayList<>();
            List<ChiseTweaksSettingRowDefinition> definitions =
                    candidate == ChiseTweaksSettingsController.Surface.INSPECTOR
                            ? controller.inspectorRows(inspector.snapshot(), inspectorHelpVisible)
                            : controller.rows(candidate);
            for (ChiseTweaksSettingRowDefinition definition : definitions) {
                ChiseTweaksSettingRowView row = createRow(candidate, definition);
                row.renderVisible = false;
                row.setWidgetsVisible(false);
                views.add(row);
            }
            rowsBySurface.put(candidate, views);
        }
    }

    private ChiseTweaksSettingRowView createRow(
            ChiseTweaksSettingsController.Surface owner,
            ChiseTweaksSettingRowDefinition definition) {
        ChiseTweaksSettingRowDefinition.Kind kind = definition.kind();
        ChiseTweaksSettingRowView row;
        if (kind == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
            row = ChiseTweaksSettingRowView.header(definition, null);
        } else if (kind == ChiseTweaksSettingRowDefinition.Kind.INFO) {
            row = new ChiseTweaksSettingRowView(definition, null, null, null, null);
        } else if (kind == ChiseTweaksSettingRowDefinition.Kind.BOOLEAN) {
            row = createBooleanRow(definition);
        } else if (kind == ChiseTweaksSettingRowDefinition.Kind.INTEGER) {
            row = createIntegerRow(definition);
        } else {
            row = createActionRow(definition);
        }
        if (definition.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO) {
            row.infoTextLayout = ChiseTweaksInfoTextLayout.create(
                    definition.name(),
                    definition.description(),
                    geometry.infoTextWidth(),
                    font.lineHeight,
                    geometry.infoRowHeight(),
                    (text, availableWidth) -> font.split(Component.literal(text), availableWidth));
        }
        applyTooltip(row);
        applyAvailabilityInteractivity(owner, row);
        return row;
    }

    private static void applyTooltip(ChiseTweaksSettingRowView row) {
        if (row == null || row.definition.description().isBlank()) return;
        Tooltip tooltip = Tooltip.create(Component.literal(row.definition.description()));
        if (row.primary != null) row.primary.setTooltip(tooltip);
        if (row.minus != null) row.minus.setTooltip(tooltip);
        if (row.value != null) row.value.setTooltip(tooltip);
        if (row.plus != null) row.plus.setTooltip(tooltip);
    }

    private void applyAvailabilityInteractivity(
            ChiseTweaksSettingsController.Surface owner,
            ChiseTweaksSettingRowView row) {
        boolean rowInteractive = UiAvailabilityPolicy.isRowInteractive(owner, row.definition);
        boolean actionInteractive = row.definition.action() != null
                && UiAvailabilityPolicy.isActionInteractive(owner, row.definition.action());
        ChiseTweaksSettingRowDefinition.Kind kind = row.definition.kind();
        if (kind == ChiseTweaksSettingRowDefinition.Kind.BOOLEAN) {
            if (row.primary != null) row.primary.active = rowInteractive;
        } else if (kind == ChiseTweaksSettingRowDefinition.Kind.INTEGER) {
            if (row.minus != null) row.minus.active = rowInteractive;
            if (row.plus != null) row.plus.active = rowInteractive;
            if (row.value != null) row.value.active = false;
        } else if (kind == ChiseTweaksSettingRowDefinition.Kind.ACTION && row.primary != null) {
            row.primary.active = actionInteractive;
        }
    }

    private ChiseTweaksSettingRowView createBooleanRow(ChiseTweaksSettingRowDefinition definition) {
        ChiseBooleanSetting config = definition.booleanConfig();
        Button button = addRenderableWidget(Button.builder(toggleMessage(config), ignored -> {
            boolean previous = config.getBooleanValue();
            config.toggleBooleanValue();
            finishSettingEdit(config.getBooleanValue() != previous, config.persistence());
        }).bounds(0, 0, geometry.toggleWidth(), 18).build());
        return new ChiseTweaksSettingRowView(definition, button, null, null, null);
    }

    private ChiseTweaksSettingRowView createIntegerRow(ChiseTweaksSettingRowDefinition definition) {
        var config = definition.integerConfig();
        int step = definition.step();
        Button minus = addRenderableWidget(Button.builder(Component.literal("−"), ignored -> {
            int previous = config.getIntegerValue();
            config.setIntegerValue(saturatedStep(previous, -step));
            finishSettingEdit(config.getIntegerValue() != previous, config.persistence());
        }).bounds(0, 0, 24, 18).build());
        Button value = addRenderableWidget(Button.builder(
                Component.literal(config.getFormattedValue()), ignored -> {})
                .bounds(0, 0, 54, 18)
                .build());
        value.active = false;
        Button plus = addRenderableWidget(Button.builder(Component.literal("+"), ignored -> {
            int previous = config.getIntegerValue();
            config.setIntegerValue(saturatedStep(previous, step));
            finishSettingEdit(config.getIntegerValue() != previous, config.persistence());
        }).bounds(0, 0, 24, 18).build());
        return new ChiseTweaksSettingRowView(definition, null, minus, value, plus);
    }

    private ChiseTweaksSettingRowView createActionRow(ChiseTweaksSettingRowDefinition definition) {
        return new ChiseTweaksSettingRowView(
                definition,
                addRenderableWidget(Button.builder(Component.literal(definition.actionLabel()),
                        ignored -> runRowAction(definition.action()))
                        .bounds(0, 0, geometry.actionWidth(), 18).build()),
                null, null, null);
    }

    private void runRowAction(ChiseTweaksSettingRowDefinition.Action action) {
        if (!UiAvailabilityPolicy.isActionInteractive(surface, action)) return;
        if (minecraft == null || action == null || !applyChanges()) return;
        if (action == ChiseTweaksSettingRowDefinition.Action.EDIT_BLOCK_FILTER) {
            minecraft.setScreen(new ChiseSceneFilterEditorScreen(
                    this, ChiseSceneFilterEditorScreen.Target.BLOCKS));
        } else if (action == ChiseTweaksSettingRowDefinition.Action.EDIT_ENTITY_FILTER) {
            minecraft.setScreen(new ChiseSceneFilterEditorScreen(
                    this, ChiseSceneFilterEditorScreen.Target.ENTITIES));
        } else if (action == ChiseTweaksSettingRowDefinition.Action.EDIT_ORE_COMPAT) {
            minecraft.setScreen(new ChiseOreCompatibilityScreen(this));
        } else if (action == ChiseTweaksSettingRowDefinition.Action.SELECT_PATTERN_REFERENCE) {
            PatternConsistencyInspector.selectReference(minecraft);
            patternRevision = PatternConsistencyInspector.currentRevision();
            rebuildInspectorRows();
        } else if (action == ChiseTweaksSettingRowDefinition.Action.CLEAR_PATTERN_REFERENCE) {
            PatternConsistencyInspector.clearReference();
            patternRevision = PatternConsistencyInspector.currentRevision();
            rebuildInspectorRows();
        }
    }

    private void selectSurface(ChiseTweaksSettingsController.Surface target, boolean resetScroll) {
        if (target == null) return;
        for (ArrayList<ChiseTweaksSettingRowView> views : rowsBySurface.values()) {
            for (ChiseTweaksSettingRowView row : views) {
                row.renderVisible = false;
                row.setWidgetsVisible(false);
            }
        }
        surface = target;
        if (resetScroll) scrollOffset = 0;
        if (surface == ChiseTweaksSettingsController.Surface.INSPECTOR) {
            inspector.invalidate();
            inspector.refresh(minecraft);
            rebuildInspectorRows();
        }
        maxScroll = 0;
        for (int index = 0; index < tabButtons.size(); index++) {
            tabButtons.get(index).active = ChiseTweaksSettingsController.Surface.values()[index] != surface;
        }
        updateRowPositions();
        refreshRowButtons();
    }

    private void runContextAction() {
        if (hasDirtyDomains()) {
            applyChanges();
            return;
        }
        if (surface == ChiseTweaksSettingsController.Surface.INSPECTOR) {
            inspectorHelpVisible = !inspectorHelpVisible;
            rebuildInspectorRows();
            refreshRowButtons();
            return;
        }
        resetCurrentSurface();
    }

    private void rebuildInspectorRows() {
        ArrayList<ChiseTweaksSettingRowView> previous = rowsBySurface.get(
                ChiseTweaksSettingsController.Surface.INSPECTOR);
        if (previous != null) {
            for (ChiseTweaksSettingRowView row : previous) {
                row.renderVisible = false;
                row.setWidgetsVisible(false);
            }
        }
        ArrayList<ChiseTweaksSettingRowView> next = new ArrayList<>();
        for (ChiseTweaksSettingRowDefinition definition
                : controller.inspectorRows(inspector.snapshot(), inspectorHelpVisible)) {
            next.add(createRow(ChiseTweaksSettingsController.Surface.INSPECTOR, definition));
        }
        rowsBySurface.put(ChiseTweaksSettingsController.Surface.INSPECTOR, next);
        if (surface == ChiseTweaksSettingsController.Surface.INSPECTOR) updateRowPositions();
    }

    private void resetCurrentSurface() {
        markDirty(controller.reset(surface));
    }

    private void finishSettingEdit(boolean changed, SettingPersistence persistence) {
        if (changed) {
            persistenceFeedback = "";
            if (persistence != null && persistence.isApplyManaged()) dirtyDomains.add(persistence);
        }
        refreshRowButtons();
    }

    private void markDirty(Set<SettingPersistence> persistenceDomains) {
        if (persistenceDomains != null) {
            for (SettingPersistence persistence : persistenceDomains) {
                if (persistence != null && persistence.isApplyManaged()) dirtyDomains.add(persistence);
            }
        }
        persistenceFeedback = "";
        refreshRowButtons();
    }

    private boolean applyChanges() {
        if (!hasDirtyDomains()) return true;
        EnumSet<SettingPersistence> attempted = EnumSet.copyOf(dirtyDomains);
        var result = controller.saveConfig(attempted);
        dirtyDomains.retainAll(result.failedDomains());
        if (!result.successful()) {
            persistenceFeedback = text("screen.chisetweaks.settings.save_failed");
            refreshRowButtons();
            return false;
        }
        persistenceFeedback = "";
        refreshRowButtons();
        return true;
    }

    private boolean hasDirtyDomains() {
        return !dirtyDomains.isEmpty();
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
        extractor.enableScissor(panel.x(), geometry.panelContentTop(), panel.right(), geometry.panelContentBottom());
        try {
            for (ChiseTweaksSettingRowView row : selectedRows()) {
                if (!row.renderVisible || row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
                    continue;
                }
                int y = row.screenY;
                int height = rowHeight(row);
                extractor.fill(panel.x() + 8, y, geometry.infoTextRight(), y + height - 2, 0x8A202020);
                extractor.fill(panel.x() + 8, y + height - 3,
                        geometry.infoTextRight(), y + height - 2, 0x553F3F3F);
            }
        } finally {
            extractor.disableScissor();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        if (geometry == null) return;
        extractor.text(font, ChiseTweaksMetadata.MOD_NAME, geometry.content().x(), 14, 0xFFFFFFFF);
        extractor.enableScissor(
                geometry.panel().x(),
                geometry.panelContentTop(),
                geometry.panel().right(),
                geometry.panelContentBottom());
        try {
            for (ChiseTweaksSettingRowView row : selectedRows()) {
                if (!row.renderVisible) continue;
                if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
                    extractor.text(font, row.definition.name(), geometry.panel().x() + 12,
                            row.screenY + 7, 0xFF78AFFF);
                    continue;
                }
                if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO) {
                    renderInfoText(extractor, row);
                    continue;
                }
                int y = row.screenY + Math.max(0, (geometry.rowHeight() - font.lineHeight) / 2);
                int color = rowTextInteractive(row.definition) ? 0xFFFFFFFF : 0xFF7A7A7A;
                extractor.text(font, row.definition.name(), geometry.nameX(), y, color);
            }
        } finally {
            extractor.disableScissor();
        }
        if (!persistenceFeedback.isEmpty()) {
            extractor.centeredText(font, Component.literal(persistenceFeedback), width / 2,
                    Math.max(24, height - 48), 0xFFFFD166);
        }
        renderScrollbar(extractor);
    }

    private void renderInfoText(GuiGraphicsExtractor extractor, ChiseTweaksSettingRowView row) {
        ChiseTweaksInfoTextLayout.Layout<FormattedCharSequence> layout = row.infoTextLayout;
        if (layout == null) return;
        for (int index = 0; index < layout.nameLines().size(); index++) {
            extractor.text(font, layout.nameLines().get(index), geometry.nameX(),
                    row.screenY + layout.nameLineY(index), 0xFFFFFFFF);
        }
        for (int index = 0; index < layout.descriptionLines().size(); index++) {
            extractor.text(font, layout.descriptionLines().get(index), geometry.nameX(),
                    row.screenY + layout.descriptionLineY(index), 0xFFB8B8B8);
        }
    }

    private boolean rowTextInteractive(ChiseTweaksSettingRowDefinition definition) {
        if (definition.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO) return true;
        if (definition.kind() == ChiseTweaksSettingRowDefinition.Kind.ACTION) {
            return UiAvailabilityPolicy.isActionInteractive(surface, definition.action());
        }
        return UiAvailabilityPolicy.isRowInteractive(surface, definition);
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
        for (ChiseTweaksSettingRowView row : selectedRows()) {
            row.renderVisible = false;
            row.setWidgetsVisible(false);
        }
        int offset = 0;
        for (ChiseTweaksSettingRowView row : selectedRows()) {
            int height = rowHeight(row);
            int y = viewportTop + offset - scrollOffset;
            row.screenY = y;
            row.renderVisible = y < viewportBottom && y + height > viewportTop;
            boolean fullyVisible = y >= viewportTop && y + height <= viewportBottom;
            if (fullyVisible) positionWidgets(row, y);
            offset += height;
        }
    }

    private int contentHeight() {
        int contentHeight = 0;
        for (ChiseTweaksSettingRowView row : selectedRows()) {
            contentHeight += rowHeight(row);
        }
        return contentHeight;
    }

    private int rowHeight(ChiseTweaksSettingRowView row) {
        if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.HEADER) {
            return geometry.headerHeight();
        }
        if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.INFO) {
            return row.infoTextLayout == null ? geometry.infoRowHeight() : row.infoTextLayout.rowHeight();
        }
        return geometry.rowHeight();
    }

    private void positionWidgets(ChiseTweaksSettingRowView row, int y) {
        int controlY = y + Math.max(0, (geometry.rowHeight() - 18) / 2);
        ChiseTweaksSettingRowDefinition.Kind kind = row.definition.kind();
        if (kind == ChiseTweaksSettingRowDefinition.Kind.BOOLEAN) {
            row.primary.setPosition(geometry.toggleX(), controlY);
            row.primary.visible = true;
        } else if (kind == ChiseTweaksSettingRowDefinition.Kind.INTEGER) {
            int x = geometry.integerX();
            row.minus.setPosition(x, controlY);
            row.value.setPosition(x + 28, controlY);
            row.plus.setPosition(x + 86, controlY);
            row.minus.visible = true;
            row.value.visible = true;
            row.plus.visible = true;
        } else if (kind == ChiseTweaksSettingRowDefinition.Kind.ACTION) {
            int x = geometry.toggleX() + geometry.toggleWidth() - geometry.actionWidth();
            row.primary.setPosition(x, controlY);
            row.primary.visible = true;
        }
    }

    private void refreshRowButtons() {
        for (ChiseTweaksSettingRowView row : selectedRows()) {
            if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.BOOLEAN
                    && row.primary != null
                    && row.definition.booleanConfig() != null) {
                row.primary.setMessage(toggleMessage(row.definition.booleanConfig()));
            } else if (row.definition.kind() == ChiseTweaksSettingRowDefinition.Kind.INTEGER
                    && row.value != null
                    && row.definition.integerConfig() != null) {
                row.value.setMessage(Component.literal(row.definition.integerConfig().getFormattedValue()));
            }
        }
        if (contextButton != null) {
            boolean dirty = hasDirtyDomains();
            String label;
            if (dirty) {
                label = "設定を適用";
            } else if (surface == ChiseTweaksSettingsController.Surface.INSPECTOR) {
                label = text(inspectorHelpVisible
                        ? "screen.chisetweaks.settings.help.hide"
                        : "screen.chisetweaks.settings.help.show");
            } else {
                label = "設定をリセット";
            }
            contextButton.setMessage(Component.literal(label));
            contextButton.visible = true;
            contextButton.active = true;
        }
    }

    private List<ChiseTweaksSettingRowView> selectedRows() {
        ArrayList<ChiseTweaksSettingRowView> rows = rowsBySurface.get(surface);
        return rows == null ? List.of() : rows;
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
