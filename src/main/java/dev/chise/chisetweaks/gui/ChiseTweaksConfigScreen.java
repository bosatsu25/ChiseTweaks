package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.ConfigUiLocalization;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Chise-owned compact settings UI matching the in-game visual mockup. */
public final class ChiseTweaksConfigScreen extends Screen {
    private static ChiseTweaksUiSection selectedSection = ChiseTweaksUiSection.PLACEMENT;

    private Screen parent;
    private ChiseTweaksSettingsLayout.Geometry geometry;
    private EditBox searchBox;
    private Button bulkButton;
    private Button applyButton;
    private final ArrayList<Row> rows = new ArrayList<>();
    private String searchQuery = "";
    private int scrollOffset;
    private int maxScroll;
    private boolean dirty;

    public ChiseTweaksConfigScreen() {
        this(selectedSection);
    }

    public ChiseTweaksConfigScreen(ChiseTweaksUiSection initialSection) {
        super(Component.literal(ChiseTweaksMetadata.MOD_NAME));
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
        LocalFeatureSettings.init();
        VisualTargetSettings.init();
        ConfigUiLocalization.refresh();
        geometry = ChiseTweaksSettingsLayout.calculate(width, height, selectedSection.isCategoryPage());

        createNavigation();
        createSearch();
        createFooter();
        createRows();
        updateRowPositions();
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

        Button selector = addRenderableWidget(Button.builder(
                Component.literal("ChiseTweaks ▼"), ignored -> {})
                .bounds(geometry.selector().x(), geometry.selector().y(),
                        geometry.selector().width(), geometry.selector().height())
                .build());
        selector.active = false;
    }

    private void createSearch() {
        searchBox = addRenderableWidget(new EditBox(
                font,
                geometry.search().x(), geometry.search().y(),
                geometry.search().width(), geometry.search().height(),
                Component.literal("検索")));
        searchBox.setHint(Component.literal(isJapanese() ? "検索..." : "Search..."));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(value -> {
            searchQuery = value == null ? "" : value;
            scrollOffset = 0;
            updateRowPositions();
        });

        if (selectedSection.isCategoryPage()) {
            bulkButton = addRenderableWidget(Button.builder(
                    bulkMessage(), ignored -> toggleBulk())
                    .bounds(geometry.bulk().x(), geometry.bulk().y(),
                            geometry.bulk().width(), geometry.bulk().height())
                    .build());
        }
    }

    private void createFooter() {
        int y = geometry.footer().y();
        int left = geometry.footer().x();
        int right = geometry.footer().right();

        Button reset = addRenderableWidget(Button.builder(
                Component.literal(isJapanese() ? "設定をリセット" : "Reset section"),
                ignored -> resetCurrentSection())
                .bounds(left, y, 112, 20)
                .build());
        reset.active = selectedSection != ChiseTweaksUiSection.HOTKEYS;

        applyButton = addRenderableWidget(Button.builder(
                Component.literal(isJapanese() ? "適用" : "Apply"), ignored -> applyChanges())
                .bounds(left + 122, y, 92, 20)
                .build());
        applyButton.active = dirty;

        addRenderableWidget(Button.builder(
                Component.literal(isJapanese() ? "完了" : "Done"), ignored -> onClose())
                .bounds(right - 132, y, 132, 20)
                .build());
    }

    private void createRows() {
        rows.clear();
        switch (selectedSection) {
            case PLACEMENT -> placementRows();
            case RESOURCES -> resourceRows();
            case VISIBILITY -> visibilityRows();
            case HOTKEYS -> hotkeyRows();
            case HELP -> { }
        }
    }

    private void placementRows() {
        header("設置・向き", "Placement & Direction");
        bool("pumpkin", FeatureSwitches.PUMPKIN_SCAFFOLD,
                "Pumpkin Scaffold", "かぼちゃを使った設置作業を補助する");
        integer("pumpkinRange", LocalFeatureSettings.PUMPKIN_SCAFFOLD_PLACEMENT_RANGE,
                "かぼちゃ設置距離", "かぼちゃを置く最大距離", 1);
        bool("placementGuide", FeatureSwitches.PLACEMENT_GUIDE,
                "設置方向ガイド", "ブロックの向きや設置状態を見やすくする");
        header("設置方向ガイドの対象", "Placement Guide targets");
        for (IConfigBase option : targets("visualTargetPlacement")) {
            boolTarget(option, "向きや設置状態を確認しやすくする");
        }
    }

    private void resourceRows() {
        header("資源", "Resources");
        bool("materials", FeatureSwitches.MATERIAL_HIGHLIGHTS,
                "鉱石ハイライト", "鉱石や資源を見つけやすくする");
        bool("nether", FeatureSwitches.NETHER_PALETTE,
                "ネザー配色ガイド", "ネザーの資源を見分けやすくする");
        header("ハイライト対象", "Highlight targets");
        for (IConfigBase option : targets("visualTargetMaterial")) {
            boolTarget(option, "対象資源を個別にON/OFFする");
        }
    }

    private void visibilityRows() {
        header("見やすさ", "Visibility");
        bool("thread", FeatureSwitches.FINE_THREAD_TRACE,
                "細線トレース", "細い補助線で輪郭を見やすくする");
        bool("hidden", FeatureSwitches.HIDDEN_SURFACE_TRACE,
                "隠面トレース", "見えにくいブロックを視認しやすくする");
        bool("glass", FeatureSwitches.GLASS_INSPECTION,
                "ガラス検査", "ガラスや板ガラスの境界を確認しやすくする");

        header("見えにくいブロックの対象", "Hidden-surface targets");
        for (IConfigBase option : targets("visualTargetHidden")) {
            boolTarget(option, targetDescription(option));
        }

        header("表示を絞る対象", "Scene Filter");
        bool("focusBlocks", FeatureSwitches.BUILDER_FOCUS_BLOCKS,
                "ブロック", "必要なブロックだけ見やすくする");
        bool("focusEntities", FeatureSwitches.BUILDER_FOCUS_ENTITIES,
                "エンティティ", "必要なエンティティだけを表示する");

        header("溶岩・視認の詳細設定", "Lava & visibility details");
        bool("lava", LocalFeatureSwitches.LAVA_HIGHLIGHT,
                "溶岩ハイライト", "溶岩やマグマを強調表示する");
        integer("scanRange", LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS,
                "視認スキャン範囲", "周辺を確認する水平範囲", 1);
        integer("scanInterval", LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL,
                "スキャン間隔", "周辺確認を行うtick間隔", 5);
    }

    private void hotkeyRows() {
        header("キー設定", "Keybinds");
        for (FeatureSwitch feature : FeatureSwitches.VALUES) {
            action(feature.getName(), display(feature),
                    isJapanese() ? "キー割り当てを編集する" : "Edit the key binding",
                    isJapanese() ? "編集" : "Edit",
                    () -> {
                        if (minecraft != null) minecraft.setScreen(new ChiseTweaksHotkeyScreen(this));
                    });
        }
    }

    private void header(String japanese, String english) {
        rows.add(Row.header(isJapanese() ? japanese : english));
    }

    private void bool(String id, IConfigBoolean config, String japaneseName, String japaneseDescription) {
        String name = isJapanese() ? japaneseName : display(config);
        String description = isJapanese() ? japaneseDescription : comment(config);
        Button button = addRenderableWidget(Button.builder(toggleMessage(config), ignored -> {
            config.setBooleanValue(!config.getBooleanValue());
            dirty = true;
            refreshRowButtons();
        }).bounds(0, 0, geometry.controlWidth(), 18).build());
        rows.add(Row.bool(id, name, description, config, button));
    }

    private void boolTarget(IConfigBase base, String description) {
        if (!(base instanceof IConfigBoolean config)) return;
        String name = compactTargetName(display(base));
        Button button = addRenderableWidget(Button.builder(toggleMessage(config), ignored -> {
            config.setBooleanValue(!config.getBooleanValue());
            dirty = true;
            refreshRowButtons();
        }).bounds(0, 0, geometry.controlWidth(), 18).build());
        rows.add(Row.bool(base.getName(), name,
                isJapanese() ? description : comment(base), config, button));
    }

    private void integer(String id, ConfigInteger config, String japaneseName,
                         String japaneseDescription, int step) {
        Button minus = addRenderableWidget(Button.builder(Component.literal("−"), ignored -> {
            config.setIntegerValue(config.getIntegerValue() - step);
            dirty = true;
            refreshRowButtons();
        }).bounds(0, 0, 24, 18).build());
        Button value = addRenderableWidget(Button.builder(
                Component.literal(Integer.toString(config.getIntegerValue())), ignored -> {})
                .bounds(0, 0, 54, 18).build());
        value.active = false;
        Button plus = addRenderableWidget(Button.builder(Component.literal("+"), ignored -> {
            config.setIntegerValue(config.getIntegerValue() + step);
            dirty = true;
            refreshRowButtons();
        }).bounds(0, 0, 24, 18).build());
        rows.add(Row.integer(id,
                isJapanese() ? japaneseName : display(config),
                isJapanese() ? japaneseDescription : comment(config),
                config, minus, value, plus));
    }

    private void action(String id, String name, String description, String label, Runnable action) {
        Button button = addRenderableWidget(Button.builder(Component.literal(label), ignored -> action.run())
                .bounds(0, 0, geometry.controlWidth(), 18).build());
        rows.add(Row.action(id, name, description, button));
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
        List<IConfigBase> targetOptions = categoryTargets();
        boolean turnOn = !allEnabled(targetOptions);
        if (selectedSection == ChiseTweaksUiSection.RESOURCES) {
            VisualTargetSettings.setAllOreHighlightTargets(turnOn);
        } else {
            for (IConfigBase option : targetOptions) {
                if (option instanceof IConfigBoolean value) value.setBooleanValue(turnOn);
            }
        }
        dirty = true;
        refreshRowButtons();
    }

    private void resetCurrentSection() {
        switch (selectedSection) {
            case PLACEMENT -> {
                FeatureSwitches.PUMPKIN_SCAFFOLD.resetToDefault();
                FeatureSwitches.PLACEMENT_GUIDE.resetToDefault();
                LocalFeatureSettings.PUMPKIN_SCAFFOLD_PLACEMENT_RANGE.resetToDefault();
                resetTargets("visualTargetPlacement");
            }
            case RESOURCES -> {
                FeatureSwitches.MATERIAL_HIGHLIGHTS.resetToDefault();
                FeatureSwitches.NETHER_PALETTE.resetToDefault();
                resetTargets("visualTargetMaterial");
            }
            case VISIBILITY -> {
                FeatureSwitches.FINE_THREAD_TRACE.resetToDefault();
                FeatureSwitches.HIDDEN_SURFACE_TRACE.resetToDefault();
                FeatureSwitches.GLASS_INSPECTION.resetToDefault();
                FeatureSwitches.BUILDER_FOCUS_BLOCKS.resetToDefault();
                FeatureSwitches.BUILDER_FOCUS_ENTITIES.resetToDefault();
                LocalFeatureSwitches.LAVA_HIGHLIGHT.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_HORIZONTAL_RADIUS.resetToDefault();
                LocalFeatureSettings.WORKSITE_VISIBILITY_INTERVAL.resetToDefault();
                resetTargets("visualTargetHidden");
            }
            case HOTKEYS, HELP -> { return; }
        }
        dirty = true;
        refreshRowButtons();
    }

    private void resetTargets(String prefix) {
        for (IConfigBase option : targets(prefix)) {
            if (option instanceof IConfigBoolean value) value.resetToDefault();
        }
    }

    private void applyChanges() {
        if (!dirty) return;
        FeatureConfig.saveToFile();
        dirty = false;
        if (applyButton != null) applyButton.active = false;
    }

    @Override
    public void onClose() {
        applyChanges();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (geometry != null && mouseX >= geometry.panel().x() && mouseX <= geometry.panel().right()
                && mouseY >= geometry.panel().y() && mouseY <= geometry.panel().bottom()) {
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

        for (Row row : visibleRows()) {
            if (!row.renderVisible) continue;
            if (row.kind == RowKind.HEADER) continue;
            int y = row.screenY;
            extractor.fill(panel.x() + 8, y, panel.right() - 24, y + geometry.rowHeight() - 2, 0x8A202020);
            extractor.fill(panel.x() + 8, y + geometry.rowHeight() - 3,
                    panel.right() - 24, y + geometry.rowHeight() - 2, 0x553F3F3F);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        if (geometry == null) return;

        extractor.text(font,
                ChiseTweaksMetadata.MOD_NAME + " " + ChiseTweaksMetadata.MOD_VERSION,
                geometry.content().x(), 14, 0xFFFFFFFF);

        int descriptionMaxWidth = Math.max(80, geometry.controlX() - geometry.descriptionX() - 16);
        for (Row row : visibleRows()) {
            if (!row.renderVisible) continue;
            if (row.kind == RowKind.HEADER) {
                extractor.text(font, row.name, geometry.panel().x() + 12, row.screenY + 6, 0xFF78AFFF);
                continue;
            }
            extractor.text(font, row.name, geometry.nameX(), row.screenY + 6, 0xFFFFFFFF);
            extractor.text(font, ellipsize(row.description, descriptionMaxWidth),
                    geometry.descriptionX(), row.screenY + 6, 0xFFC8C8C8);
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
        int thumbY = trackTop + (maxScroll == 0 ? 0 : scrollOffset * travel / maxScroll);
        extractor.fill(trackX, thumbY, trackX + 5, thumbY + thumbHeight, 0xFFD0D0D0);
    }

    private void updateRowPositions() {
        if (geometry == null) return;
        int offset = 0;
        List<Row> visible = visibleRows();
        int viewportTop = geometry.panelContentTop();
        int viewportBottom = geometry.panelContentBottom();
        for (Row row : rows) {
            row.renderVisible = false;
            row.setWidgetsVisible(false);
        }
        for (Row row : visible) {
            int height = row.kind == RowKind.HEADER ? geometry.headerHeight() : geometry.rowHeight();
            int y = viewportTop + offset - scrollOffset;
            row.screenY = y;
            row.renderVisible = y >= viewportTop && y + height <= viewportBottom;
            if (row.renderVisible && row.kind != RowKind.HEADER) positionWidgets(row, y);
            offset += height;
        }
        int viewport = Math.max(1, viewportBottom - viewportTop);
        maxScroll = Math.max(0, offset - viewport);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        refreshRowButtons();
    }

    private void positionWidgets(Row row, int y) {
        int controlY = y + 8;
        if (row.kind == RowKind.BOOLEAN || row.kind == RowKind.ACTION) {
            row.primary.setPosition(geometry.controlX(), controlY);
            row.primary.visible = true;
        } else if (row.kind == RowKind.INTEGER) {
            int total = 24 + 4 + 54 + 4 + 24;
            int x = geometry.panel().right() - 16 - total;
            row.minus.setPosition(x, controlY);
            row.value.setPosition(x + 28, controlY);
            row.plus.setPosition(x + 86, controlY);
            row.minus.visible = row.value.visible = row.plus.visible = true;
        }
    }

    private void refreshRowButtons() {
        for (Row row : rows) {
            if (row.kind == RowKind.BOOLEAN && row.primary != null && row.booleanConfig != null) {
                row.primary.setMessage(toggleMessage(row.booleanConfig));
            } else if (row.kind == RowKind.INTEGER && row.value != null && row.integerConfig != null) {
                row.value.setMessage(Component.literal(Integer.toString(row.integerConfig.getIntegerValue())));
            }
        }
        if (bulkButton != null) bulkButton.setMessage(bulkMessage());
        if (applyButton != null) applyButton.active = dirty;
    }

    private List<Row> visibleRows() {
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return List.copyOf(rows);

        ArrayList<Row> result = new ArrayList<>();
        Row pendingHeader = null;
        for (Row row : rows) {
            if (row.kind == RowKind.HEADER) {
                pendingHeader = row;
                continue;
            }
            String searchable = (row.name + " " + row.description).toLowerCase(Locale.ROOT);
            if (searchable.contains(query)) {
                if (pendingHeader != null && (result.isEmpty() || result.get(result.size() - 1) != pendingHeader)) {
                    result.add(pendingHeader);
                }
                result.add(row);
            }
        }
        return List.copyOf(result);
    }

    private List<IConfigBase> categoryTargets() {
        return switch (selectedSection) {
            case PLACEMENT -> targets("visualTargetPlacement");
            case RESOURCES -> targets("visualTargetMaterial");
            case VISIBILITY -> targets("visualTargetHidden");
            case HOTKEYS, HELP -> List.of();
        };
    }

    private static List<IConfigBase> targets(String prefix) {
        ArrayList<IConfigBase> result = new ArrayList<>();
        for (IConfigBase option : VisualTargetSettings.ALL_OPTIONS) {
            if (option.getName() != null && option.getName().startsWith(prefix)) result.add(option);
        }
        return List.copyOf(result);
    }

    private static boolean allEnabled(List<IConfigBase> options) {
        if (options.isEmpty()) return false;
        for (IConfigBase option : options) {
            if (option instanceof IConfigBoolean value && !value.getBooleanValue()) return false;
        }
        return true;
    }

    private Component bulkMessage() {
        boolean turnOn = !allEnabled(categoryTargets());
        return Component.literal((isJapanese() ? "一括選択：" : "Select all: ") + (turnOn ? "ON" : "OFF"));
    }

    private static Component toggleMessage(IConfigBoolean value) {
        boolean enabled = value.getBooleanValue();
        return Component.literal(enabled ? "ON" : "OFF")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private String display(IConfigBase config) {
        String value = config.getConfigGuiDisplayName();
        return value == null || value.isBlank() ? config.getName() : value;
    }

    private String comment(IConfigBase config) {
        String value = config.getComment();
        return value == null ? "" : value;
    }

    private String compactTargetName(String value) {
        if (!isJapanese() || value == null) return value == null ? "" : value;
        int colon = value.indexOf('：');
        return colon >= 0 && colon + 1 < value.length() ? value.substring(colon + 1) : value;
    }

    private String targetDescription(IConfigBase option) {
        String name = option.getName();
        if (name == null) return "見つけやすくする";
        if (name.endsWith("BlueIce")) return "見えにくい青氷を確認";
        if (name.endsWith("DeadCoral")) return "サンゴ系ブロックを確認";
        if (name.endsWith("PowderSnow")) return "粉雪を視認しやすくする";
        if (name.endsWith("SculkCatalyst")) return "周辺作業時に確認しやすくする";
        return "見つけやすくする";
    }

    private String ellipsize(String text, int maxWidth) {
        if (text == null || text.isBlank()) return "";
        if (font.width(text) <= maxWidth) return text;
        String suffix = "…";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = result.toString() + text.charAt(i) + suffix;
            if (font.width(candidate) > maxWidth) break;
            result.append(text.charAt(i));
        }
        return result + suffix;
    }

    private static boolean isJapanese() {
        return "ja".equals(fi.dy.masa.malilib.util.StringUtils.getTranslatedOrFallback(
                "screen.chisetweaks.help.language.probe", "en"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum RowKind { HEADER, BOOLEAN, INTEGER, ACTION }

    private static final class Row {
        private final RowKind kind;
        private final String id;
        private final String name;
        private final String description;
        private final IConfigBoolean booleanConfig;
        private final ConfigInteger integerConfig;
        private final Button primary;
        private final Button minus;
        private final Button value;
        private final Button plus;
        private int screenY;
        private boolean renderVisible;

        private Row(RowKind kind, String id, String name, String description,
                    IConfigBoolean booleanConfig, ConfigInteger integerConfig,
                    Button primary, Button minus, Button value, Button plus) {
            this.kind = kind;
            this.id = id;
            this.name = name;
            this.description = description;
            this.booleanConfig = booleanConfig;
            this.integerConfig = integerConfig;
            this.primary = primary;
            this.minus = minus;
            this.value = value;
            this.plus = plus;
        }

        static Row header(String name) {
            return new Row(RowKind.HEADER, name, name, "", null, null, null, null, null, null);
        }

        static Row bool(String id, String name, String description, IConfigBoolean config, Button button) {
            return new Row(RowKind.BOOLEAN, id, name, description, config, null,
                    button, null, null, null);
        }

        static Row integer(String id, String name, String description, ConfigInteger config,
                           Button minus, Button value, Button plus) {
            return new Row(RowKind.INTEGER, id, name, description, null, config,
                    null, minus, value, plus);
        }

        static Row action(String id, String name, String description, Button button) {
            return new Row(RowKind.ACTION, id, name, description, null, null,
                    button, null, null, null);
        }

        void setWidgetsVisible(boolean visible) {
            if (primary != null) primary.visible = visible;
            if (minus != null) minus.visible = visible;
            if (value != null) value.visible = visible;
            if (plus != null) plus.visible = visible;
        }
    }
}
