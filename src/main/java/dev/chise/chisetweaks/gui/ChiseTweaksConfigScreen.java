package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ConfigUiLocalization;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.VisualTargetSettings;
import fi.dy.masa.malilib.config.IConfigBase;
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
 * Task-oriented Chise settings UI.
 *
 * <p>The primary feature view keeps the established MaLiLib toggle/keybind rows. Target Lists add
 * a second navigation layer so placement, mining/resources, visual-support and explicit rule-list
 * settings are not mixed into one long list. This keeps the persistence/runtime model unchanged
 * while moving the screen closer to the compact category-based Chise UI.</p>
 */
public final class ChiseTweaksConfigScreen extends GuiConfigsBase {
    private static ConfigGuiTab selectedTab = ConfigGuiTab.FEATURES;
    private static TargetListCategory selectedTargetCategory = TargetListCategory.MINING_RESOURCES;

    public ChiseTweaksConfigScreen() {
        // Reserve two additional rows for Target Lists category and bulk-action navigation.
        super(10, 100, ChiseTweaksMetadata.MOD_ID, null,
                ChiseTweaksMetadata.MOD_NAME + " %s", ChiseTweaksMetadata.MOD_VERSION);
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
        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            tabX += createButton(tabX, 26, tab);
        }

        if (selectedTab == ConfigGuiTab.LISTS) {
            int categoryX = 10;
            for (TargetListCategory category : TargetListCategory.values()) {
                categoryX += createCategoryButton(categoryX, 50, category);
            }

            // Bulk controls are meaningful only for Ore Highlights resource families.
            if (selectedTargetCategory == TargetListCategory.MINING_RESOURCES) {
                int actionX = 10;
                for (TargetListAction action : TargetListAction.values()) {
                    actionX += createTargetActionButton(actionX, 74, action);
                }
            }
        }
    }

    private int createButton(int x, int y, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, tab.getDisplayName());
        button.setEnabled(tab == ConfigGuiTab.HELP || selectedTab != tab);
        addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    private int createCategoryButton(int x, int y, TargetListCategory category) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, category.getDisplayName());
        button.setEnabled(selectedTargetCategory != category);
        addButton(button, new CategoryButtonListener(category, this));
        return button.getWidth() + 2;
    }

    private int createTargetActionButton(int x, int y, TargetListAction action) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, action.getDisplayName());
        addButton(button, new TargetActionButtonListener(action, this));
        return button.getWidth() + 2;
    }

    @Override
    protected int getConfigWidth() {
        return switch (selectedTab) {
            case FEATURES -> 260;
            case LISTS -> 430;
            case HELP -> 220;
        };
    }

    @Override
    protected boolean useKeybindSearch() {
        // MaLiLib's built-in search also gives Target Lists the compact search-first workflow.
        return selectedTab == ConfigGuiTab.FEATURES || selectedTab == ConfigGuiTab.LISTS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return switch (selectedTab) {
            case FEATURES -> createFeatureAndHotkeyOptions();
            case LISTS -> createTargetListOptions();
            case HELP -> List.of();
        };
    }

    /** Uses exactly the established toggle/keybind row model. */
    private List<ConfigOptionWrapper> createFeatureAndHotkeyOptions() {
        ArrayList<BooleanHotkeyGuiWrapper> toggles = new ArrayList<>();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) {
            toggles.add(wrapConfig(toggle));
        }
        return ConfigOptionWrapper.createFor(toggles);
    }

    /**
     * Target Lists exposes one user-oriented category at a time. Fine-grained visual targets are
     * grouped by their stable config-name family; explicit Scene Filter allow/deny lists live in
     * Other so they do not get mixed with block-family switches.
     */
    private List<ConfigOptionWrapper> createTargetListOptions() {
        ArrayList<IConfigBase> options = new ArrayList<>();
        if (selectedTargetCategory == TargetListCategory.OTHER) {
            options.addAll(BuilderFocusConfig.RULE_OPTIONS);
        } else {
            for (IConfigBase option : VisualTargetSettings.ALL_OPTIONS) {
                if (selectedTargetCategory.matches(option.getName())) {
                    options.add(option);
                }
            }
        }
        return ConfigOptionWrapper.createFor(options);
    }

    private BooleanHotkeyGuiWrapper wrapConfig(IHotkeyTogglable config) {
        return new BooleanHotkeyGuiWrapper(config.getName(), config, config.getKeybind());
    }

    private static void refreshList(ChiseTweaksConfigScreen parent) {
        parent.reCreateListWidget();
        parent.getListWidget().resetScrollbarPosition();
        parent.initGui();
    }

    private static final class ButtonListener implements IButtonActionListener {
        private final ConfigGuiTab tab;
        private final ChiseTweaksConfigScreen parent;

        private ButtonListener(ConfigGuiTab tab, ChiseTweaksConfigScreen parent) {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            if (tab == ConfigGuiTab.HELP) {
                Minecraft.getInstance().setScreen(new ChiseTweaksHelpScreen(parent));
                return;
            }
            selectedTab = tab;
            if (tab != ConfigGuiTab.LISTS) {
                VisualTargetSettings.resetTransientControls();
            }
            refreshList(parent);
        }
    }

    private static final class CategoryButtonListener implements IButtonActionListener {
        private final TargetListCategory category;
        private final ChiseTweaksConfigScreen parent;

        private CategoryButtonListener(TargetListCategory category, ChiseTweaksConfigScreen parent) {
            this.category = category;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            if (selectedTargetCategory == category) {
                return;
            }
            VisualTargetSettings.resetTransientControls();
            selectedTargetCategory = category;
            refreshList(parent);
        }
    }

    private static final class TargetActionButtonListener implements IButtonActionListener {
        private final TargetListAction action;
        private final ChiseTweaksConfigScreen parent;

        private TargetActionButtonListener(TargetListAction action, ChiseTweaksConfigScreen parent) {
            this.action = action;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            switch (action) {
                case ALL_ON -> VisualTargetSettings.setAllOreHighlightTargets(true);
                case ALL_OFF -> VisualTargetSettings.setAllOreHighlightTargets(false);
            }
            refreshList(parent);
        }
    }

    private enum TargetListCategory {
        DECORATION("visualTargetPlacement", "Decoration", "装飾"),
        MINING_RESOURCES("visualTargetMaterial", "Mining / Resources", "採掘・資源"),
        VISUAL_SUPPORT("visualTargetHidden", "Visual Support", "視認支援"),
        OTHER("", "Other", "その他");

        private final String configPrefix;
        private final String english;
        private final String japanese;

        TargetListCategory(String configPrefix, String english, String japanese) {
            this.configPrefix = configPrefix;
            this.english = english;
            this.japanese = japanese;
        }

        boolean matches(String configName) {
            return this != OTHER && configName != null && configName.startsWith(configPrefix);
        }

        String getDisplayName() {
            return isJapanese() ? japanese : english;
        }
    }

    private enum TargetListAction {
        ALL_ON,
        ALL_OFF;

        String getDisplayName() {
            return switch (this) {
                case ALL_ON -> isJapanese() ? "対象 全ON" : "Targets: All ON";
                case ALL_OFF -> isJapanese() ? "対象 全OFF" : "Targets: All OFF";
            };
        }
    }

    private enum ConfigGuiTab {
        FEATURES("Features & Keybinds"), LISTS("Lists"), HELP("Feature Guide");

        private final String fallback;

        ConfigGuiTab(String fallback) {
            this.fallback = fallback;
        }

        String getDisplayName() {
            return StringUtils.getTranslatedOrFallback(
                    "gui.chisetweaks.tab." + name().toLowerCase(), fallback);
        }
    }

    private static boolean isJapanese() {
        return "ja".equals(StringUtils.getTranslatedOrFallback(
                "screen.chisetweaks.help.language.probe", "en"));
    }
}
