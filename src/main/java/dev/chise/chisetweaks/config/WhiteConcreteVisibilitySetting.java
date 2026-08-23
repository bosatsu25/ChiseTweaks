package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;
import dev.chise.chisetweaks.feature.resource.VisibilityPack;

/** White Concrete Visibility用の独立built-in resource packと設定UIを接続する。 */
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
        return ChiseTexturePackController.isEnabled(VisibilityPack.WHITE_CONCRETE);
    }

    @Override
    protected void writeValue(boolean value) {
        ChiseTexturePackController.setEnabled(VisibilityPack.WHITE_CONCRETE, value);
    }
}
