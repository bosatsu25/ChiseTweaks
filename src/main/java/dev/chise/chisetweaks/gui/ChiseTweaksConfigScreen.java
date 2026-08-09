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
 * Compact settings UI whose primary view is the former dedicated keybind list.
 *
 * <p>The first tab intentionally preserves the established MaLiLib row layout. Fine-grained
 * visual target selection belongs to Target Lists rather than duplicating parent feature rows.</p>
 */
public final class ChiseTweaksConfigScreen extends GuiConfigsBase {
    private static ConfigGuiTab selectedTab = ConfigGuiTab.FEATURES;

    public ChiseTweaksConfigScreen() {
        super(10, 52, ChiseTweaksMetadata.MOD_ID, null,
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
        int x = 10;
        for (ConfigGuiTab tab : ConfigGuiTab.values()) x += createButton(x, 26, tab);
        if (selectedTab == ConfigGuiTab.LISTS) {
            for (TargetListAction action : TargetListAction.values()) {
                x += createTargetActionButton(x, 26, action);
            }
        }
    }

    private int createButton(int x, int y, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, tab.getDisplayName());
        button.setEnabled(tab == ConfigGuiTab.HELP || selectedTab != tab);
        addButton(button, new ButtonListener(tab, this));
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
            case LISTS -> 360;
            case HELP -> 220;
        };
    }

    @Override
    protected boolean useKeybindSearch() {
        return selectedTab == ConfigGuiTab.FEATURES;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return switch (selectedTab) {
            case FEATURES -> createFeatureAndHotkeyOptions();
            case LISTS -> createTargetListOptions();
            case HELP -> List.of();
        };
    }

    /** Uses exactly the former Hotkeys-screen row model. */
    private List<ConfigOptionWrapper> createFeatureAndHotkeyOptions() {
        ArrayList<BooleanHotkeyGuiWrapper> toggles = new ArrayList<>();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) toggles.add(wrapConfig(toggle));
        return ConfigOptionWrapper.createFor(toggles);
    }

    /**
     * Target Lists owns Scene Filter rules and fine-grained visual target switches. Ore rows are
     * normal ON/OFF toggles; Solo only changes how an ON click is applied to Ore Highlights.
     */
    private List<ConfigOptionWrapper> createTargetListOptions() {
        ArrayList<IConfigBase> options = new ArrayList<>();
        options.addAll(VisualTargetSettings.ALL_OPTIONS);
        options.addAll(BuilderFocusConfig.RULE_OPTIONS);
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
                case SOLO -> VisualTargetSettings.toggleSoloOreSelection();
                case ALL_ON -> {
                    VisualTargetSettings.resetTransientControls();
                    VisualTargetSettings.setAllOreHighlightTargets(true);
                }
                case ALL_OFF -> {
                    VisualTargetSettings.resetTransientControls();
                    VisualTargetSettings.setAllOreHighlightTargets(false);
                }
            }
            refreshList(parent);
        }
    }

    private enum TargetListAction {
        SOLO,
        ALL_ON,
        ALL_OFF;

        String getDisplayName() {
            boolean japanese = "ja".equals(StringUtils.getTranslatedOrFallback(
                    "screen.chisetweaks.help.language.probe", "en"));
            return switch (this) {
                case SOLO -> japanese
                        ? "Solo選択: " + (VisualTargetSettings.isSoloOreSelectionEnabled() ? "ON" : "OFF")
                        : "Solo: " + (VisualTargetSettings.isSoloOreSelectionEnabled() ? "ON" : "OFF");
                case ALL_ON -> japanese ? "対象 全ON" : "Targets: All ON";
                case ALL_OFF -> japanese ? "対象 全OFF" : "Targets: All OFF";
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
}
