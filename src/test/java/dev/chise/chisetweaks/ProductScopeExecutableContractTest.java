package dev.chise.chisetweaks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chise.chisetweaks.core.definition.FeatureArea;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.chise.chisetweaks.SourceContractSupport.read;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProductScopeExecutableContractTest {
    private static final Path MAIN = Path.of("src/main/java");

    @Test
    void runtimeFeatureRegistryRemainsRenderingInspectionOnly() {
        assertEquals(16, FeatureDefinition.VALUES.size());

        Set<String> ids = new HashSet<>();
        for (FeatureDefinition definition : FeatureDefinition.VALUES) {
            assertTrue(ids.add(definition.id()), () -> "duplicate feature id: " + definition.id());
            assertEquals(FeatureArea.RENDERING, definition.area());
            assertTrue(definition.dependency().isBlank());
            assertFalse(definition.id().contains("auto"));
            assertFalse(definition.englishName().toLowerCase(java.util.Locale.ROOT).contains("auto "));
        }
    }

    @Test
    void productionSourceCannotIntroduceAutomationNetworkingOrBackgroundExecution() throws IOException {
        List<String> forbidden = List.of(
                "ClientPlayNetworking",
                "ServerPlayNetworking",
                "CustomPacketPayload",
                "java.awt.Robot",
                "new Thread(",
                "Executors.",
                "KeyMapping.click(",
                ".sendCommand(");

        try (var paths = Files.walk(MAIN)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.toString().endsWith(".java"))
                    .toList()) {
                String source = Files.readString(path);
                for (String token : forbidden) {
                    assertFalse(source.contains(token),
                            () -> "product scope forbids '" + token + "' in " + path);
                }
            }
        }
    }

    @Test
    void fabricMetadataKeepsClientOnlyNoProtocolNoAutomationOwnership() throws IOException {
        JsonObject root = JsonParser.parseString(read("src/main/resources/fabric.mod.json")).getAsJsonObject();
        assertEquals("client", root.get("environment").getAsString());
        assertEquals(Set.of("client", "modmenu"), root.getAsJsonObject("entrypoints").keySet());

        JsonArray mixins = root.getAsJsonArray("mixins");
        assertEquals(List.of(
                        "chisetweaks.features.mixins.json",
                        "chisetweaks.integrations.mixins.json",
                        "chisetweaks.workflow.mixins.json"),
                mixins.asList().stream().map(element -> element.getAsString()).toList());

        JsonObject custom = root.getAsJsonObject("custom").getAsJsonObject("chisetweaks");
        assertEquals("client-only", custom.get("side").getAsString());
        assertFalse(custom.get("serverInstallationRequired").getAsBoolean());
        assertFalse(custom.get("customPlayProtocol").getAsBoolean());
        assertEquals("none", custom.get("serverProtocolOwnership").getAsString());
        assertFalse(custom.get("remoteModDetection").getAsBoolean());
        assertFalse(custom.get("backgroundThreads").getAsBoolean());
        assertFalse(custom.get("automaticModDownload").getAsBoolean());
        assertFalse(custom.get("automaticJarReplacement").getAsBoolean());
    }
}
