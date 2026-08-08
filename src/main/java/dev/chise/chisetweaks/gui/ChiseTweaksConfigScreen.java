package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.ConfigUiLocalization;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
import dev.chise.chisetweaks.config.LocalFeatureSwitch;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
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

/** Compact settings UI for the bounded ChiseTweaks client features. */
public final class ChiseTweaksConfigScreen extends GuiConfigsBase {
    private static ConfigGuiTab selectedTab = ConfigGuiTab.FEATURES;

    public ChiseTweaksConfigScreen() {
        super(10, 52, ChiseTweaksMetadata.MOD_ID, null,
                ChiseTweaksMetadata.MOD_NAME + " %s", ChiseTweaksMetadata.MOD_VERSION);
    }

    @Override
    public void initGui() {
        LocalFeatureSettings.init();
        ConfigUiLocalization.refresh();
        super.initGui();
        clearOptions();
        int x = 10;
        for (ConfigGuiTab tab : ConfigGuiTab.values()) x += createButton(x, 26, tab);
    }

    private int createButton(int x, int y, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, tab.getDisplayName());
        button.setEnabled(tab == ConfigGuiTab.HELP || selectedTab != tab);
        addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    @Override
    protected int getConfigWidth() {
        return switch (selectedTab) {
            case ALL, FEATURES -> 350;
            case LISTS -> 320;
            case HOTKEYS -> 260;
            case HELP -> 220;
        };
    }

    @Override
    protected boolean useKeybindSearch() {
        return selectedTab == ConfigGuiTab.ALL
                || selectedTab == ConfigGuiTab.FEATURES
                || selectedTab == ConfigGuiTab.HOTKEYS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return switch (selectedTab) {
            case ALL -> createAllOptions();
            case FEATURES -> createFeatureOptions(true);
            case LISTS -> ConfigOptionWrapper.createFor(BuilderFocusConfig.RULE_OPTIONS);
            case HOTKEYS -> createHotkeyOptions();
            case HELP -> List.of();
        };
    }

    private List<ConfigOptionWrapper> createAllOptions() {
        ArrayList<ConfigOptionWrapper> result = new ArrayList<>();
        result.addAll(createFeatureOptions(true));
        result.addAll(ConfigOptionWrapper.createFor(BuilderFocusConfig.RULE_OPTIONS));
        return List.copyOf(result);
    }

    private List<ConfigOptionWrapper> createFeatureOptions(boolean includeSettings) {
        ArrayList<ConfigOptionWrapper> result = new ArrayList<>();
        ArrayList<BooleanHotkeyGuiWrapper> toggles = new ArrayList<>();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) toggles.add(wrapConfig(toggle));
        result.addAll(ConfigOptionWrapper.createFor(toggles));

        ArrayList<IConfigBase> options = new ArrayList<>();
        options.addAll(LocalFeatureSwitches.VALUES);
        if (includeSettings) {
            options.addAll(BuilderFocusConfig.GENERAL_OPTIONS);
            options.addAll(LocalFeatureSettings.ALL_OPTIONS);
        }
        result.addAll(ConfigOptionWrapper.createFor(options));
        return List.copyOf(result);
    }

    private List<ConfigOptionWrapper> createHotkeyOptions() {
        ArrayList<BooleanHotkeyGuiWrapper> toggles = new ArrayList<>();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) toggles.add(wrapConfig(toggle));
        return ConfigOptionWrapper.createFor(toggles);
    }

    private BooleanHotkeyGuiWrapper wrapConfig(IHotkeyTogglable config) {
        return new BooleanHotkeyGuiWrapper(config.getName(), config, config.getKeybind());
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
            parent.reCreateListWidget();
            parent.getListWidget().resetScrollbarPosition();
            parent.initGui();
        }
    }

    private enum ConfigGuiTab {
        ALL("All"), FEATURES("Features"), LISTS("Lists"), HOTKEYS("Hotkeys"), HELP("Feature Guide");
        private final String fallback;
        ConfigGuiTab(String fallback) { this.fallback = fallback; }
        String getDisplayName() {
            return StringUtils.getTranslatedOrFallback(
                    "gui.chisetweaks.tab." + name().toLowerCase(), fallback);
        }
    }
}
