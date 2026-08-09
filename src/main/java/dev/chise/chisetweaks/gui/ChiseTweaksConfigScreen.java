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
 * <p>The UI deliberately exposes five user-facing destinations instead of a generic
 * Features/Lists hierarchy: Placement & Direction, Resources, Visibility, Keybinds and Guide.
 * Category pages keep the feature switch and its target/settings rows together, so users do not
 * have to understand the internal distinction between a feature and its target list.</p>
 */
public final class ChiseTweaksConfigScreen extends GuiConfigsBase {
    private static ChiseTweaksUiSection selectedSection = ChiseTweaksUiSection.PLACEMENT;

    public ChiseTweaksConfigScreen() {
        this(selectedSection);
    }

    public ChiseTweaksConfigScreen(ChiseTweaksUiSection initialSection) {
        // One shared navigation row plus one search/bulk-action row.
        super(10, 76, ChiseTweaksMetadata.MOD_ID, null,
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
        super.initGui();
        clearOptions();

        int tabX = 10;
        for (ChiseTweaksUiSection section : ChiseTweaksUiSection.values()) {
            tabX += createSectionButton(tabX, 26, section);
        }

        // Category pages use one context-aware bulk control. It intentionally occupies the same
        // horizontal band as search instead of creating a third navigation row.
        if (selectedSection.isCategoryPage()) {
            createBulkToggleButton(Math.max(10, this.width - 132), 50);
        }
    }

    private int createSectionButton(int x, int y, ChiseTweaksUiSection section) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, section.getDisplayName());
        button.setEnabled(section == ChiseTweaksUiSection.HELP || selectedSection != section);
        addButton(button, new SectionButtonListener(section, this));
        return button.getWidth() + 2;
    }

    private void createBulkToggleButton(int x, int y) {
        boolean turnOn = !areAllCategoryTargetsEnabled(selectedSection);
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, bulkLabel(turnOn));
        addButton(button, new BulkTargetButtonListener(selectedSection, this));
    }

    @Override
    protected int getConfigWidth() {
        return switch (selectedSection) {
            case PLACEMENT, RESOURCES, VISIBILITY -> 470;
            case HOTKEYS -> 300;
            case HELP -> 220;
        };
    }

    @Override
    protected boolean useKeybindSearch() {
        return selectedSection != ChiseTweaksUiSection.HELP;
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
        addFeature(rows, FeatureSwitches.PUMPKIN_SCAFFOLD);
        addConfigs(rows, LocalFeatureSettings.BUILDING_OPTIONS);

        addSpacer(rows);
        addFeature(rows, FeatureSwitches.PLACEMENT_GUIDE);
        addHeader(rows, translated("Placement Guide targets", "設置方向ガイドの対象"));
        addConfigs(rows, visualTargetsFor(ChiseTweaksUiSection.PLACEMENT));
        return List.copyOf(rows);
    }

    private List<ConfigOptionWrapper> createResourceOptions() {
        ArrayList<ConfigOptionWrapper> rows = new ArrayList<>();

        addHeader(rows, translated("Resource visibility", "資源の見やすさ"));
        addFeature(rows, FeatureSwitches.MATERIAL_HIGHLIGHTS);
        addHeader(rows, translated("Highlight targets", "ハイライト対象"));
        addConfigs(rows, visualTargetsFor(ChiseTweaksUiSection.RESOURCES));

        addSpacer(rows);
        addFeature(rows, FeatureSwitches.NETHER_PALETTE);
        return List.copyOf(rows);
    }

    private List<ConfigOptionWrapper> createVisibilityOptions() {
        ArrayList<ConfigOptionWrapper> rows = new ArrayList<>();

        addHeader(rows, translated("Visibility helpers", "見やすさ"));
        addFeature(rows, FeatureSwitches.FINE_THREAD_TRACE);

        addSpacer(rows);
        addFeature(rows, FeatureSwitches.HIDDEN_SURFACE_TRACE);
        addHeader(rows, translated("Hidden-surface targets", "見えにくいブロックの対象"));
        addConfigs(rows, visualTargetsFor(ChiseTweaksUiSection.VISIBILITY));

        addSpacer(rows);
        addFeature(rows, FeatureSwitches.GLASS_INSPECTION);

        addSpacer(rows);
        addFeature(rows, FeatureSwitches.BUILDER_FOCUS_BLOCKS);
        addFeature(rows, FeatureSwitches.BUILDER_FOCUS_ENTITIES);
        addHeader(rows, translated("Scene Filter rules", "表示を絞る対象"));
        addConfigs(rows, BuilderFocusConfig.RULE_OPTIONS);

        addSpacer(rows);
        addHeader(rows, translated("Lava and scan details", "溶岩・視認の詳細設定"));
        addConfigs(rows, List.of(LocalFeatureSwitches.LAVA_HIGHLIGHT));
        addConfigs(rows, LocalFeatureSettings.RENDERING_OPTIONS);
        return List.copyOf(rows);
    }

    /**
     * MaLiLib currently exposes boolean+hotkey rows through one stable wrapper. Keeping those rows
     * on the dedicated Keybinds page preserves the established multi-key editor while the Chise
     * navigation no longer mixes target-list discovery with key configuration.
     */
    private List<ConfigOptionWrapper> createHotkeyOptions() {
        ArrayList<ConfigOptionWrapper> rows = new ArrayList<>();
        addHeader(rows, translated("Feature keybinds", "機能のキー設定"));
        for (FeatureSwitch feature : FeatureSwitches.VALUES) {
            addFeature(rows, feature);
        }
        return List.copyOf(rows);
    }

    private static void addFeature(List<ConfigOptionWrapper> rows, FeatureSwitch feature) {
        rows.addAll(ConfigOptionWrapper.createFor(List.of(wrapConfig(feature))));
    }

    private static void addConfigs(List<ConfigOptionWrapper> rows, List<? extends IConfigBase> configs) {
        if (configs == null || configs.isEmpty()) return;
        rows.addAll(ConfigOptionWrapper.createFor(new ArrayList<>(configs)));
    }

    private static void addHeader(List<ConfigOptionWrapper> rows, String title) {
        rows.add(new ConfigOptionWrapper(title));
    }

    private static void addSpacer(List<ConfigOptionWrapper> rows) {
        rows.add(new ConfigOptionWrapper(""));
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
            // One bounded config save for the largest target family.
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
