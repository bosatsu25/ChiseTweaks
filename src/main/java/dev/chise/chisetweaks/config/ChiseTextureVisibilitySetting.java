package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;

/** Settings bridge for the independently controlled chest visibility pack. */
public final class ChiseTextureVisibilitySetting extends ChiseBooleanSetting {
    public static final ChiseTextureVisibilitySetting INSTANCE = new ChiseTextureVisibilitySetting();

    private ChiseTextureVisibilitySetting() {
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
        return ChiseTexturePackController.isChestEnabled();
    }

    @Override
    protected void writeValue(boolean value) {
        ChiseTexturePackController.setChestEnabled(value);
    }
}
