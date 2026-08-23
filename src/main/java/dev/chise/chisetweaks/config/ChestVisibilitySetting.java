package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;
import dev.chise.chisetweaks.feature.resource.VisibilityPack;

/** Chest Visibility用の独立built-in resource packと設定UIを接続する。 */
public final class ChestVisibilitySetting extends ChiseBooleanSetting {
    public static final ChestVisibilitySetting INSTANCE = new ChestVisibilitySetting();

    private ChestVisibilitySetting() {
        super(
                "chestVisibility",
                true,
                "Chest Visibility",
                "チェスト視認性",
                "Toggle the high-visibility chest textures independently.",
                "高視認のチェストテクスチャだけを独立して切り替えます。");
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
