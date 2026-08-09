package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ConfigUiLocalization;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IHotkeyTogglable;
import fi.dy.masa.malilib.config.options.BooleanHotkeyGuiWrapper;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Chise-first settings navigation.
 *
 * <p>The UI exposes five task-oriented destinations. Category pages deliberately render compact
 * boolean-only feature controls; full hotkey editors live only on the Keybinds page. The content
 * area is capped and centered so large GUI scales and ultrawide windows do not stretch every
 * control across the screen.</p>
 */
public final class ChiseTweaksConfigScreen extends GuiConfigsBase {
    private static final int NAV_Y = 28;
    private static final int LIST_Y = 56;
    private static final int HORIZONTAL_MARGIN = 12;
    private static final int BOTTOM_MARGIN = 12;
    private static final int MAX_GROUP_WIDTH = 1080;
    private static final int BULK_BUTTON_WIDTH = 126;
    private static final int BULK_GAP = 8;

    private static ChiseTweaksUiSection selectedSection = ChiseTweaksUiSection.PLACEMENT;

    public ChiseTweaksConfigScreen() {
        this(selectedSection);
    }

    public ChiseTweaksConfigScreen(ChiseTweaksUiSection initialSection) {
        super(HORIZONTAL_MARGIN, LIST_Y, ChiseTweaksMetadata.MOD_ID, null,
                ChiseTweaksMetadata.MOD_NAME + " %s", ChiseTweaksMetadata.MOD_VERSION);
        if (initialSection != null && initialSection != ChiseTweaksUiSection.HELP) {
            selectedSection = initialSection;
        }
        VisualTargetSettings.resetTransientControls();
    }

    @Override
    public void initGui() {
        LocalFeatureSettings.init();
        VisualTargetSettings.init();
        ConfigUiLocalization.refresh();

        // The list position depends on the current scaled window width. Recreate it before
        // MaLiLib initializes children so resize/GUI-scale changes use the new compact geometry.
        setListPosition(groupX(), LIST_Y);
        reCreateListWidget();
        super.initGui();
        clearOptions();

        int tabX = groupX();
        for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values()) {
            tabX += createSectionButton(tabX, NAV_Y, section);
        }

        if (selectedSection.isCategoryPage()) {
            createBulkToggleButton(
                    groupX() + getBrowserWidth() + BULK_GAP,
                    LIST_Y + 1);
        }
    }

    private int createSectionButton(int x, int y, ChiseTweaksUiSection section) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, section.getDisplayName());
        button.setEnabled(section == ChiseTweaksUiSection.HELP || selectedSection != section);
        addButton(button, new SectionButtonListener(section, this));
        return button.getWidth() + 4;
    }

    private void createBulkToggleButton(int x, int y) {
        boolean turnOn = !areAllCategoryTargetsEnabled(selectedSection);
        ButtonGeneric button = new ButtonGeneric(
                x, y, BULK_BUTTON_WIDTH, 20, bulkLabel(turnOn));
        addButton(button, new BulkTargetButtonListener(selectedSection, this));
    }

    @Override
    protected int getBrowserWidth() {
        int reserved = selectedSection.isCategoryPage() ? BULK_BUTTON_WIDTH + BULK_GAP : 0;
        return Math.max(160, groupWidth() - reserved);
    }

    @Override
    protected int getBrowserHeight() {
        return Math.max(80, this.height - LIST_Y - BOTTOM_MARGIN);
    }

    @Override
    protected int getConfigWidth() {
        int preferred = selectedSection == ChiseTweaksUiSection.HOTKEYS ? 220 : 180;
        int minimum = selectedSection == ChiseTweaksUiSection.HOTKEYS ? 150 : 110;
        int responsive = Math.max(minimum, getBrowserWidth() / 3);
        return Math.min(preferred, responsive);
    }

    @Override
    protected boolean useKeybindSearch() {
        // Category pages still get MaLiLib's normal text search. Only Keybinds needs the extended
        // key-search control, which otherwise adds a large NONE/key-capture control beside search.
        return selectedSection == ChiseTweaksUiSection.HOTKEYS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return switch (selectedSection) {
            case PLACEMENT -> createPlacementOptions();
            case RESOURCES -> createResourceOptions();
            case VISIBILITY -> createVisibilityOptions();
            case HOTKEYS -> createHotkeyOptions();
            case HELP -> List.of();
        };
    }

    private List<ConfigOptionWrapper> createPlacementOptions() {
        ArrayList<ConfigOptionWrapper> rows = new ArrayList<>();

        addHeader(rows, translated("Placement helpers", "設置機能"));
        addFeatureToggle(rows, FeatureSwitches.PUMPKIN_SCAFFOLD);
        addConfigs(rows, LocalFeatureSettings.BUILDING_OPTIONS);

        addFeatureToggle(rows, FeatureSwitches.PLACEMENT_GUIDE);
        addHeader(rows, translated("Placement Guide targets", "設置方向ガイドの対象"));
        addConfigs(rows, visualTargetsFor(ChiseTweaksUiSection.PLACEMENT));
        return List.copyOf(rows);
    }

    private List<ConfigOptionWrapper> createResourceOptions() {
        ArrayList<ConfigOptionWrapper> rows = new ArrayList<>();

        addHeader(rows, translated("Resource visibility", "資源の見やすさ"));
        addFeatureToggle(rows, FeatureSwitches.MATERIAL_HIGHLIGHTS);
        addHeader(rows, translated("Highlight targets", "ハイライト対象"));
        addConfigs(rows, visualTargetsFor(ChiseTweaksUiSection.RESOURCES));
        addFeatureToggle(rows, FeatureSwitches.NETHER_PALETTE);
        return List.copyOf(rows);
    }

    private List<ConfigOptionWrapper> createVisibilityOptions() {
        ArrayList<ConfigOptionWrapper> rows = new ArrayList<>();

        addHeader(rows, translated("Visibility helpers", "見やすさ"));
        addFeatureToggle(rows, FeatureSwitches.FINE_THREAD_TRACE);
        addFeatureToggle(rows, FeatureSwitches.HIDDEN_SURFACE_TRACE);
        addHeader(rows, translated("Hidden-surface targets", "見えにくいブロックの対象"));
        addConfigs(rows, visualTargetsFor(ChiseTweaksUiSection.VISIBILITY));
        addFeatureToggle(rows, FeatureSwitches.GLASS_INSPECTION);

        addFeatureToggle(rows, FeatureSwitches.BUILDER_FOCUS_BLOCKS);
        addFeatureToggle(rows, FeatureSwitches.BUILDER_FOCUS_ENTITIES);
        addHeader(rows, translated("Scene Filter rules", "表示を絞る対象"));
        addConfigs(rows, BuilderFocusConfig.RULE_OPTIONS);

        addHeader(rows, translated("Lava and scan details", "溶岩・視認の詳細設定"));
        addConfigs(rows, List.of(LocalFeatureSwitches.LAVA_HIGHLIGHT));
        addConfigs(rows, LocalFeatureSettings.RENDERING_OPTIONS);
        return List.copyOf(rows);
    }

    /** Keeps the full MaLiLib multi-key editor isolated to the dedicated Keybinds page. */
    private List<ConfigOptionWrapper> createHotkeyOptions() {
        ArrayList<ConfigOptionWrapper> rows = new ArrayList<>();
        addHeader(rows, translated("Feature keybinds", "機能のキー設定"));
        for (FeatureSwitch feature : FeatureSwitches.VALUES) {
            addFeatureHotkey(rows, feature);
        }
        return List.copyOf(rows);
    }

    private static void addFeatureToggle(List<ConfigOptionWrapper> rows, FeatureSwitch feature) {
        rows.add(new ConfigOptionWrapper(feature.booleanGuiView()));
    }

    private static void addFeatureHotkey(List<ConfigOptionWrapper> rows, FeatureSwitch feature) {
        rows.addAll(ConfigOptionWrapper.createFor(List.of(wrapConfig(feature))));
    }

    private static void addConfigs(List<ConfigOptionWrapper> rows, List<? extends IConfigBase> configs) {
        if (configs == null || configs.isEmpty()) return;
        rows.addAll(ConfigOptionWrapper.createFor(new ArrayList<>(configs)));
    }

    private static void addHeader(List<ConfigOptionWrapper> rows, String title) {
        rows.add(new ConfigOptionWrapper(title));
    }

    private static BooleanHotkeyGuiWrapper wrapConfig(IHotkeyTogglable config) {
        return new BooleanHotkeyGuiWrapper(config.getName(), config, config.getKeybind());
    }

    private static List<IConfigBase> visualTargetsFor(ChiseTweaksUiSection section) {
        String prefix = targetPrefix(section);
        if (prefix.isEmpty()) return List.of();

        ArrayList<IConfigBase> result = new ArrayList<>();
        for (IConfigBase option : VisualTargetSettings.ALL_OPTIONS) {
            if (option.getName() != null && option.getName().startsWith(prefix)) {
                result.add(option);
            }
        }
        return List.copyOf(result);
    }

    private static boolean areAllCategoryTargetsEnabled(ChiseTweaksUiSection section) {
        List<IConfigBase> targets = visualTargetsFor(section);
        if (targets.isEmpty()) return false;
        for (IConfigBase target : targets) {
            if (target instanceof IConfigBoolean booleanTarget && !booleanTarget.getBooleanValue()) {
                return false;
            }
        }
        return true;
    }

    private static void setAllCategoryTargets(ChiseTweaksUiSection section, boolean enabled) {
        if (section == ChiseTweaksUiSection.RESOURCES) {
            VisualTargetSettings.setAllOreHighlightTargets(enabled);
            return;
        }
        for (IConfigBase target : visualTargetsFor(section)) {
            if (target instanceof IConfigBoolean booleanTarget) {
                booleanTarget.setBooleanValue(enabled);
            }
        }
    }

    private static String targetPrefix(ChiseTweaksUiSection section) {
        return switch (section) {
            case PLACEMENT -> "visualTargetPlacement";
            case RESOURCES -> "visualTargetMaterial";
            case VISIBILITY -> "visualTargetHidden";
            case HOTKEYS, HELP -> "";
        };
    }

    private int groupWidth() {
        int available = Math.max(320, this.width - HORIZONTAL_MARGIN * 2);
        return Math.min(MAX_GROUP_WIDTH, available);
    }

    private int groupX() {
        return Math.max(HORIZONTAL_MARGIN, (this.width - groupWidth()) / 2);
    }

    private static String bulkLabel(boolean turnOn) {
        if (isJapanese()) {
            return "一括選択：" + (turnOn ? "ON" : "OFF");
        }
        return "Select all: " + (turnOn ? "ON" : "OFF");
    }

    private static String translated(String english, String japanese) {
        return isJapanese() ? japanese : english;
    }

    private static boolean isJapanese() {
        return "ja".equals(StringUtils.getTranslatedOrFallback(
                "screen.chisetweaks.help.language.probe", "en"));
    }

    private static void refreshList(ChiseTweaksConfigScreen parent) {
        parent.reCreateListWidget();
        if (parent.getListWidget() != null) {
            parent.getListWidget().resetScrollbarPosition();
        }
        parent.initGui();
    }

    private static final class SectionButtonListener implements IButtonActionListener {
        private final ChiseTweaksUiSection section;
        private final ChiseTweaksConfigScreen parent;

        private SectionButtonListener(ChiseTweaksUiSection section, ChiseTweaksConfigScreen parent) {
            this.section = section;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            if (section == ChiseTweaksUiSection.HELP) {
                Minecraft.getInstance().setScreen(new ChiseTweaksHelpScreen(parent));
                return;
            }
            if (selectedSection == section) return;
            selectedSection = section;
            VisualTargetSettings.resetTransientControls();
            refreshList(parent);
        }
    }

    private static final class BulkTargetButtonListener implements IButtonActionListener {
        private final ChiseTweaksUiSection section;
        private final ChiseTweaksConfigScreen parent;

        private BulkTargetButtonListener(ChiseTweaksUiSection section, ChiseTweaksConfigScreen parent) {
            this.section = section;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            boolean turnOn = !areAllCategoryTargetsEnabled(section);
            setAllCategoryTargets(section, turnOn);
            refreshList(parent);
        }
    }
}
