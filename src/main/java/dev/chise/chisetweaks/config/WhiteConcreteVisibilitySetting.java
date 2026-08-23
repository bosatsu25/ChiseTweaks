package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;
import dev.chise.chisetweaks.feature.resource.VisibilityPack;

/** Bright Concrete built-in resource packと設定UIを接続する。 */
public final class WhiteConcreteVisibilitySetting extends ChiseBooleanSetting {
    public static final WhiteConcreteVisibilitySetting INSTANCE = new WhiteConcreteVisibilitySetting();

    private WhiteConcreteVisibilitySetting() {
        super(
                "whiteConcreteVisibility",
                true,
                "Bright Concrete",
                "Bright Concrete",
                "Toggle the Bright Concrete built-in resource pack independently.",
                "Toggle the Bright Concrete built-in resource pack independently.");
    }

    @Override
    public SettingPersistence persistence() {
        return SettingPersistence.EXTERNAL;
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
