package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.config.ConfigUiLocalization;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureSettings;
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

/**
 * Compact settings UI whose primary view keeps the original hotkey-first row layout.
 *
 * <p>Feature toggles and keybinds share one tab. Non-hotkey feature settings remain below the
 * hotkey rows so existing controls stay reachable without reintroducing a duplicate Features tab.</p>
 */
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
            // Match the former dedicated Hotkeys view so toggle/keybind/reset controls stay compact.
            case FEATURES -> 260;
            case LISTS -> 320;
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
            case LISTS -> ConfigOptionWrapper.createFor(BuilderFocusConfig.RULE_OPTIONS);
            case HELP -> List.of();
        };
    }

    private List<ConfigOptionWrapper> createFeatureAndHotkeyOptions() {
        ArrayList<ConfigOptionWrapper> result = new ArrayList<>();

        // Keep the former Hotkeys screen as the primary layout: toggle + keybind + clear/reset.
        ArrayList<BooleanHotkeyGuiWrapper> toggles = new ArrayList<>();
        for (FeatureSwitch toggle : FeatureSwitches.VALUES) toggles.add(wrapConfig(toggle));
        result.addAll(ConfigOptionWrapper.createFor(toggles));

        // Preserve non-hotkey feature controls without creating another duplicate top-level tab.
        ArrayList<IConfigBase> options = new ArrayList<>();
        options.addAll(LocalFeatureSwitches.VALUES);
        options.addAll(BuilderFocusConfig.GENERAL_OPTIONS);
        options.addAll(LocalFeatureSettings.ALL_OPTIONS);
        result.addAll(ConfigOptionWrapper.createFor(options));
        return List.copyOf(result);
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
