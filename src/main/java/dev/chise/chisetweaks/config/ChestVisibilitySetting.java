package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;
import dev.chise.chisetweaks.feature.resource.VisibilityPack;

/** Bright Chest built-in resource packと設定UIを接続する。 */
public final class ChestVisibilitySetting extends ChiseBooleanSetting {
    public static final ChestVisibilitySetting INSTANCE = new ChestVisibilitySetting();

    private ChestVisibilitySetting() {
        super(
                "chestVisibility",
                true,
                "Bright Chest",
                "Bright Chest",
                "Toggle the Bright Chest built-in resource pack independently.",
                "Toggle the Bright Chest built-in resource pack independently.");
    }

    @Override
    public boolean requiresApplyPersistence() {
        return false;
    }

    @Override
    protected boolean readValue() {
        return ChiseTexturePackController.isEnabled(VisibilityPack.CHEST);
    }

    @Override
    protected void writeValue(boolean value) {
        ChiseTexturePackController.setEnabled(VisibilityPack.CHEST, value);
    }
}
