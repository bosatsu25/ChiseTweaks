package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** S9: fail closed when direct networking, packet mutation, or input automation enters production code. */
final class ProductScopeSGradeContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path MAIN = ROOT.resolve("src/main/java");

    private static final Map<String, String> FORBIDDEN = Map.ofEntries(
            Map.entry("import java.net.", "runtime network access"),
            Map.entry("ClientPlayNetworking", "custom client networking"),
            Map.entry("Serverbound", "direct serverbound packet ownership"),
            Map.entry("handleInventoryMouseClick(", "inventory click automation"),
            Map.entry("KeyMapping.click(", "synthetic key click"),
            Map.entry(".setDown(true)", "synthetic held-key input"),
            Map.entry("startAttack(", "synthetic attack input"),
            Map.entry("startUseItem(", "synthetic use input"),
            Map.entry("getConnection().send(", "direct packet send"),
            Map.entry("player.connection.send(", "direct packet send")
    );

    @Test
    void productionCodeContainsNoDirectAutomationOrNetworkOwnership() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                for (Map.Entry<String, String> entry : FORBIDDEN.entrySet()) {
                    if (source.contains(entry.getKey())) {
                        violations.add(ROOT.relativize(path) + ": " + entry.getValue()
                                + " [" + entry.getKey() + "]");
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> "Product-scope violations:\n" + String.join("\n", violations));
    }

    @Test
    void fabricMetadataRemainsClientOnly() throws IOException {
        String metadata = Files.readString(ROOT.resolve("src/main/resources/fabric.mod.json"));
        assertTrue(metadata.contains("\"environment\": \"client\""),
                "fabric.mod.json must remain client-only");
    }
}
