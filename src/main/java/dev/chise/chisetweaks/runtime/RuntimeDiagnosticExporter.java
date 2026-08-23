package dev.chise.chisetweaks.runtime;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** 個人情報を追加せず、診断snapshotだけをboundedなlocal fileとして保存する。 */
public final class RuntimeDiagnosticExporter {
    static final int MAX_RETAINED_REPORTS = 10;
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC);
    private static final String PREFIX = "chisetweaks-diagnostic-";
    private static final String SUFFIX = ".txt";

    private RuntimeDiagnosticExporter() {}

    public static String report(Minecraft client) {
        return RuntimeDiagnosticReport.format(RuntimeDiagnostics.capture(client));
    }

    public static String export(Minecraft client) throws IOException {
        Path directory = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("chisetweaks")
                .resolve("diagnostics");
        Files.createDirectories(directory);

        String fileName = PREFIX + FILE_TIMESTAMP.format(Instant.now()) + SUFFIX;
        Path destination = directory.resolve(fileName);
        Files.writeString(
                destination,
                report(client),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        pruneOldReports(directory);
        return fileName;
    }

    static void pruneOldReports(Path directory) throws IOException {
        if (directory == null || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return;
        List<Path> reports;
        try (var entries = Files.list(directory)) {
            reports = entries
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(PREFIX) && name.endsWith(SUFFIX);
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        int removeCount = Math.max(0, reports.size() - MAX_RETAINED_REPORTS);
        for (int index = 0; index < removeCount; index++) {
            Files.deleteIfExists(reports.get(index));
        }
    }
}
