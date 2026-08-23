package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;

/** Settings bridge for the independently controlled white-concrete visibility pack. */
public final class WhiteConcreteVisibilitySetting extends ChiseBooleanSetting {
    public static final WhiteConcreteVisibilitySetting INSTANCE = new WhiteConcreteVisibilitySetting();

    private WhiteConcreteVisibilitySetting() {
        super(
                "whiteConcreteVisibility",
                true,
                "White Concrete Visibility",
                "白色コンクリート視認性",
                "Toggle the high-visibility White Concrete texture independently.",
                "高視認の白色コンクリートテクスチャだけを独立して切り替えます。");
    }

    @Override
    protected boolean readValue() {
        return ChiseTexturePackController.isWhiteConcreteEnabled();
    }

    @Override
    protected void writeValue(boolean value) {
        ChiseTexturePackController.setWhiteConcreteEnabled(value);
    }
}
