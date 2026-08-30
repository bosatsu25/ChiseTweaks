package dev.chise.chisetweaks.core.policy;

import java.util.Map;

/**
 * Small curated Japanese UX layer for the Masa ecosystem.
 *
 * It intentionally does not reproduce upstream language files. Unknown text is returned unchanged.
 */
public final class MasaJapaneseTextPolicy {
    private static final Map<String, String> EXACT_TEXT = Map.ofEntries(
            Map.entry("Generic", "一般設定 (Generic)"),
            Map.entry("Visuals", "表示設定 (Visuals)"),
            Map.entry("Hotkeys", "キー設定 (Hotkeys)"),
            Map.entry("Lists", "リスト設定 (Lists)"),
            Map.entry("Config", "設定 (Config)"),
            Map.entry("Configs", "設定 (Configs)"),
            Map.entry("Reset", "リセット (Reset)"),
            Map.entry("RESET", "リセット (RESET)"),
            Map.entry("Cancel", "キャンセル (Cancel)"),
            Map.entry("Set Here", "ここに設定 (Set Here)"),
            Map.entry("All registered hotkeys", "登録済みキー設定一覧 (All registered hotkeys)"),
            Map.entry("Advanced Keybind settings", "詳細キー設定 (Advanced Keybind settings)"),
            Map.entry("Black List", "拒否リスト (Black List)"),
            Map.entry("White List", "許可リスト (White List)"),
            Map.entry("Layer", "レイヤー (Layer)"),
            Map.entry("Layer Range", "レイヤー範囲 (Layer Range)"),
            Map.entry("Single Layer", "単一レイヤー (Single Layer)"),
            Map.entry("All above", "上側すべて (All above)"),
            Map.entry("All below", "下側すべて (All below)"),
            Map.entry("Create a new directory", "新しいフォルダーを作成 (Create a new directory)"),
            Map.entry("View Syncmatics", "共有設計図を表示 (View Syncmatics)"),
            Map.entry("Material Collections", "材料収集 (Material Collections)"),
            Map.entry("Load", "読み込む (Load)"),
            Map.entry("Unload", "表示解除 (Unload)"),
            Map.entry("Remove", "削除 (Remove)"),
            Map.entry("Download", "ダウンロード (Download)"),
            Map.entry("Downloading...", "ダウンロード中... (Downloading...)"),
            Map.entry("Share", "共有 (Share)"),
            Map.entry("Materials", "材料 (Materials)"),
            Map.entry("Manage Server Placements", "サーバー共有配置を管理 (Manage Server Placements)"),
            Map.entry("File Name", "ファイル名 (File Name)"),
            Map.entry("Origin (xyz)", "原点座標 (Origin xyz)"),
            Map.entry("Dimension", "ディメンション (Dimension)")
    );

    private static final Map<String, String> EXACT_KEY = Map.ofEntries(
            Map.entry("litematica.config.generic.name.easyPlaceMode", "Easy Place設定 (easyPlaceMode)"),
            Map.entry("litematica.config.generic.name.placementRestriction", "配置制限 (placementRestriction)"),
            Map.entry("litematica.config.generic.name.materialListIgnoreState", "材料リストでBlockStateを無視 (materialListIgnoreState)"),
            Map.entry("litematica.config.generic.name.renderMaterialListInGuis", "GUIに材料リストを表示 (renderMaterialListInGuis)"),
            Map.entry("litematica.config.generic.name.pickBlockEnabled", "Pick Blockを有効化 (pickBlockEnabled)"),
            Map.entry("litematica.config.visuals.name.enableRendering", "Litematica描画を有効化 (enableRendering)"),
            Map.entry("litematica.config.visuals.name.enableSchematicRendering", "設計図を描画 (enableSchematicRendering)"),
            Map.entry("litematica.config.visuals.name.enableSchematicOverlay", "設計図Overlayを表示 (enableSchematicOverlay)"),
            Map.entry("tweakeroo.config.generic.name.accuratePlacementProtocol", "正確配置Protocol (accuratePlacementProtocol)"),
            Map.entry("tweakeroo.config.generic.name.placementGridSize", "配置Gridサイズ (placementGridSize)"),
            Map.entry("tweakeroo.config.generic.name.placementLimit", "配置上限 (placementLimit)"),
            Map.entry("tweakeroo.config.generic.name.placementRestrictionMode", "配置制限Mode (placementRestrictionMode)"),
            Map.entry("tweakeroo.config.generic.name.gammaOverrideValue", "Gamma Override値 (gammaOverrideValue)"),
            Map.entry("tweakeroo.config.generic.name.toolSwitchableSlots", "Tool Switch対象Slot (toolSwitchableSlots)"),
            Map.entry("tweakeroo.config.generic.name.toolSwitchIgnoredSlots", "Tool Switch除外Slot (toolSwitchIgnoredSlots)"),
            Map.entry("malilib.config.name.openGuiConfigs", "MaLiLib設定を開く (openGuiConfigs)")
    );

    private MasaJapaneseTextPolicy() {}

    public static boolean effectiveJapanese(int mode, String languageCode) {
        MasaJapaneseUiMode resolved = MasaJapaneseUiMode.fromId(mode);
        if (resolved == MasaJapaneseUiMode.ENABLED) return true;
        if (resolved == MasaJapaneseUiMode.DISABLED) return false;
        String language = languageCode == null ? "" : languageCode.trim().toLowerCase(java.util.Locale.ROOT);
        return language.equals("ja_jp") || language.startsWith("ja_");
    }

    public static String translate(String key, String original, int mode, String languageCode) {
        String fallback = original == null ? "" : original;
        if (!effectiveJapanese(mode, languageCode)) return fallback;

        String byKey = EXACT_KEY.get(key == null ? "" : key);
        if (byKey != null) return byKey;
        String byText = EXACT_TEXT.get(fallback);
        return byText == null ? fallback : byText;
    }
}
