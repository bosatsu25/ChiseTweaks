package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;
import dev.chise.chisetweaks.feature.resource.VisibilityPack;

/** Bright Concrete built-in resource packと設定UIを接続する。 */
public final class WhiteConcreteVisibilitySetting extends ChiseBooleanSetting {
    public static final WhiteConcreteVisibilitySetting INSTANCE = new WhiteConcreteVisibilitySetting();

    private WhiteConcreteVisibilitySetting() {
        super("whiteConcreteVisibility", true, SettingPersistence.EXTERNAL);
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
