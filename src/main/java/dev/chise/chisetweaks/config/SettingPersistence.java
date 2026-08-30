package dev.chise.chisetweaks.config;

/** 設定変更をどの永続化境界へ書き込むかを表す。 */
public enum SettingPersistence {
    FEATURE_CONFIG(true),
    LOCAL_CONFIG(true),
    INTEGRATION_CONFIG(true),
    EXTERNAL(false);

    private final boolean applyManaged;

    SettingPersistence(boolean applyManaged) {
        this.applyManaged = applyManaged;
    }

    /** ChiseのApply操作がこのdomainの永続化を担当する場合true。 */
    public boolean isApplyManaged() {
        return applyManaged;
    }
}
