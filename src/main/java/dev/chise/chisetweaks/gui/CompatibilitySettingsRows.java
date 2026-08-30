package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.CompatibilityIntegrationSettings;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

final class CompatibilitySettingsRows {
    private CompatibilitySettingsRows() {}

    static List<ChiseTweaksSettingRowDefinition> rows() {
        boolean nvidiumInstalled = FabricLoader.getInstance().isModLoaded("nvidium");
        return List.of(
                ChiseTweaksSettingRowDefinition.header(
                        "compatibility.renderer.header", "Renderer Compatibility"),
                ChiseTweaksSettingRowDefinition.info(
                        "compatibility.nvidium.status",
                        "Nvidium",
                        nvidiumInstalled ? "Installed / 導入済み" : "Not installed / 未導入 - World Border Fixはno-opです"),
                ChiseTweaksSettingRowDefinition.bool(
                        "worldBorderFixEnabled",
                        "World Border Fix",
                        "ワールドボーダー付近・遠距離座標でNvidium描画が破綻する環境向け。危険領域だけNvidiumを一時停止します。",
                        CompatibilityIntegrationSettings.WORLD_BORDER_FIX_ENABLED),
                ChiseTweaksSettingRowDefinition.bool(
                        "worldBorderFixXray",
                        "World Border X-Ray Guard",
                        "ワールドボーダー接近時の透明化/X-Ray状描画を抑制します。",
                        CompatibilityIntegrationSettings.WORLD_BORDER_FIX_XRAY),
                ChiseTweaksSettingRowDefinition.integer(
                        "worldBorderFixDistance",
                        "Border Trigger Distance",
                        "この距離よりワールドボーダーへ近づいたとき抑制候補にします。",
                        CompatibilityIntegrationSettings.WORLD_BORDER_FIX_DISTANCE,
                        16),
                ChiseTweaksSettingRowDefinition.bool(
                        "worldBorderFixFarCoords",
                        "Far Coordinate Guard",
                        "大きなX/Z座標でのNvidium描画破綻を抑制します。",
                        CompatibilityIntegrationSettings.WORLD_BORDER_FIX_FAR_COORDS),
                ChiseTweaksSettingRowDefinition.integer(
                        "worldBorderFixCoordThreshold",
                        "Far Coordinate Threshold",
                        "|X|または|Z|がこの値以上で抑制候補にします。",
                        CompatibilityIntegrationSettings.WORLD_BORDER_FIX_COORD_THRESHOLD,
                        1000),
                ChiseTweaksSettingRowDefinition.bool(
                        "worldBorderFixAutoReenable",
                        "Immediate Nvidium Re-enable",
                        "危険領域を離れた直後に描画再読込してNvidiumを復帰します。既定OFF。安定性優先ならOFFを推奨します。",
                        CompatibilityIntegrationSettings.WORLD_BORDER_FIX_AUTO_REENABLE));
    }
}
