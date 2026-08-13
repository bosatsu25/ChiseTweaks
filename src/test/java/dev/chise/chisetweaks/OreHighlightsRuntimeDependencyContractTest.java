package dev.chise.chisetweaks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Version/dependency contract for Prism-safe client packaging. */
final class OreHighlightsRuntimeDependencyContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void fabricMetadataUsesOnlyProjectControlledVersionPlaceholders() throws IOException {
        JsonObject metadata = JsonParser.parseString(Files.readString(
                ROOT.resolve("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        JsonObject depends = metadata.getAsJsonObject("depends");
        JsonObject recommends = metadata.getAsJsonObject("recommends");

        assertEquals("${minecraft_version}", depends.get("minecraft").getAsString());
        assertEquals(">=${loader_version}", depends.get("fabricloader").getAsString());
        assertEquals(">=${fabric_api_version}", depends.get("fabric-api").getAsString());
        assertEquals(">=25", depends.get("java").getAsString());
        assertEquals(">=${sodium_compat_version}", recommends.get("sodium").getAsString());

        assertFalse(depends.has("sodium"));
        assertFalse(depends.has("iris"));
        assertFalse(depends.has("irisshaders"));
        assertFalse(depends.has("modmenu"));
    }

    @Test
    void resourceExpansionBindsMetadataToTheSameGradlePropertiesUsedByDependencies() throws IOException {
        String build = Files.readString(ROOT.resolve("build.gradle"));

        assertTrue(build.contains("minecraft \"com.mojang:minecraft:${project.minecraft_version}\""));
        assertTrue(build.contains("implementation \"net.fabricmc:fabric-loader:${project.loader_version}\""));
        assertTrue(build.contains("implementation \"net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}\""));

        assertTrue(build.contains("minecraft_version    : project.minecraft_version"));
        assertTrue(build.contains("loader_version       : project.loader_version"));
        assertTrue(build.contains("fabric_api_version   : project.fabric_api_version"));
        assertTrue(build.contains("sodium_compat_version: project.sodium_compat_version"));
        assertTrue(build.contains("filesMatching('fabric.mod.json') { expand(fabricMetadata) }"));
    }
}
