package dev.chise.chisetweaks.runtime;

import java.util.Objects;

/** Clipboardやファイルへ安全に持ち出せる、単一行snapshot由来の診断reportを構築する。 */
public final class RuntimeDiagnosticReport {
    private static final int SCHEMA_VERSION = 1;

    private RuntimeDiagnosticReport() {}

    public static String format(RuntimeDiagnosticSnapshot snapshot) {
        RuntimeDiagnosticSnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
        return "ChiseTweaks Diagnostic Snapshot\n"
                + "schema=" + SCHEMA_VERSION + "\n"
                + checked.toLogLine() + "\n";
    }
}
