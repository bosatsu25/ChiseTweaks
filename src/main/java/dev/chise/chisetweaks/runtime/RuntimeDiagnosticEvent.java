package dev.chise.chisetweaks.runtime;

/** 異常解析ログで使用するイベント名を固定し、文字列の揺れと誤記を防ぐ。 */
public enum RuntimeDiagnosticEvent {
    CLIENT_STARTUP("client-startup"),
    CLIENT_JOIN("client-join"),
    CLIENT_DISCONNECT("client-disconnect"),
    COMPONENT_INIT_QUARANTINE("component-init-quarantine"),
    COMPONENT_QUARANTINE("component-quarantine");

    private final String wireName;

    RuntimeDiagnosticEvent(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }
}
