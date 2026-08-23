package dev.chise.chisetweaks.config;

import dev.chise.chisetweaks.feature.resource.ChiseTexturePackController;

/** 設定画面からChise Textureの高視認テクスチャを切り替える。 */
public final class ChiseTextureVisibilitySetting extends ChiseBooleanSetting {
    public static final ChiseTextureVisibilitySetting INSTANCE = new ChiseTextureVisibilitySetting();

    private ChiseTextureVisibilitySetting() {
        super(
                "chestVisibility",
                true,
                "Chest Visibility",
                "チェスト視認性",
                "Toggle Chise Texture high-visibility textures for chests and white concrete.",
                "チェストと白色コンクリートの高視認テクスチャを切り替えます。");
    }

    @Override
    protected boolean readValue() {
        return ChiseTexturePackController.isEnabled();
    }

    @Override
    protected void writeValue(boolean value) {
        ChiseTexturePackController.setEnabled(value);
    }
}
