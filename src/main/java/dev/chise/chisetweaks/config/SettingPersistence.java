package dev.chise.chisetweaks.config;

/** 設定変更をどの永続化境界へ書き込むかをUIと共有する。 */
public enum SettingPersistence {
    FEATURE_CONFIG,
    LOCAL_CONFIG,
    EXTERNAL
}
