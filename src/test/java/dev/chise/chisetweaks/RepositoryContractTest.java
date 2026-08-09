package dev.chise.chisetweaks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Repository-wide security, compatibility, performance and release contracts. */
final class RepositoryContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Path MAIN = ROOT.resolve("src/main/java");
    private static final Path RESOURCES = ROOT.resolve("src/main/resources");
    private static final Path LANG = RESOURCES.resolve("assets/chisetweaks/lang");
    private static final Path WORKFLOWS = ROOT.resolve(".github/workflows");
    private static final Gson GSON = new Gson();

    @Test
    void repositoryContainsNoStaleExternalProjectIdentityTokens() throws IOException {
        List<String> longTokens = List.of(
                chars(116, 97, 105, 99, 104, 105),
                chars(97, 109, 97, 116, 101, 114, 97, 115),
                chars(97, 115, 116, 114, 97, 108));
        List<String> shortTokens = List.of(chars(97, 109, 97), chars(97, 115, 116));
        Set<String> skipDirs = Set.of(".git", ".gradle", "build", "out", "run", "runs");
        Set<String> binarySuffixes = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".jar", ".class", ".zip");
        List<String> failures = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(ROOT)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                Path relative = ROOT.relativize(path);
                if (relative.iterator().hasNext() && containsAnyPart(relative, skipDirs)) continue;
                String fileName = path.getFileName().toString().toLowerCase();
                if (binarySuffixes.stream().anyMatch(fileName::endsWith)) continue;
                String text;
                try {
                    text = Files.readString(path, StandardCharsets.UTF_8).toLowerCase();
                } catch (IOException failure) {
                    failures.add(relative + ": " + failure.getMessage());
                    continue;
                }
                if (longTokens.stream().anyMatch(text::contains)) {
                    failures.add(relative.toString());
                    continue;
                }
                for (String token : shortTokens) {
                    Pattern standalone = Pattern.compile("(?<![a-z0-9_])" + Pattern.quote(token) + "(?![a-z0-9_])");
                    if (standalone.matcher(text).find()) {
                        failures.add(relative.toString());
                        break;
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), () -> "stale external-project identity tokens: " + failures);
    }

    @Test
    void productionSourceRetainsSecurityPerformanceAndMaintainabilityContracts() throws IOException {
        List<Path> javaFiles = javaFiles();
        Map<Path, String> textByFile = new HashMap<>();
        for (Path path : javaFiles) textByFile.put(path, Files.readString(path, StandardCharsets.UTF_8));
        String allText = String.join("\n", textByFile.values());

        for (Map.Entry<Path, String> entry : textByFile.entrySet()) {
            String text = entry.getValue();
            assertFalse(Pattern.compile("\\b(?:TODO|FIXME|HACK)\\b").matcher(text).find(), entry.getKey().toString());
            assertFalse(Pattern.compile("catch\\s*\\(\\s*(?:Exception|Throwable)\\b").matcher(text).find(), entry.getKey().toString());
            assertFalse(Pattern.compile("catch\\s*\\([^)]*\\)\\s*\\{\\s*}", Pattern.DOTALL).matcher(text).find(), entry.getKey().toString());
        }

        for (String forbidden : List.of(
                "WorksiteVisibilityFeature",
                "getWorksiteVisibilityFeature",
                "ChiseConfigStorage",
                "SafeFileStorage",
                "hasChunkAt(")) {
            assertFalse(allText.contains(forbidden), forbidden);
        }

        Path secureStorage = MAIN.resolve("dev/chise/chisetweaks/core/security/SecureConfigStorage.java");
        assertTrue(Files.isRegularFile(secureStorage));
        for (String relative : List.of(
                "dev/chise/chisetweaks/config/FeatureConfig.java",
                "dev/chise/chisetweaks/config/LocalFeatureConfig.java")) {
            String text = textByFile.get(MAIN.resolve(relative));
            assertNotNull(text, relative);
            assertTrue(text.contains("SecureConfigStorage"), relative);
            assertTrue(text.contains("FabricLoader.getInstance().getConfigDir()"), relative);
        }

        String featureManager = textByFile.get(MAIN.resolve("dev/chise/chisetweaks/runtime/FeatureManager.java"));
        int activeCall = featureManager.indexOf("boolean active = component.isActive();");
        assertTrue(activeCall >= 0);
        int searchStart = Math.max(0, activeCall - 200);
        assertTrue(featureManager.substring(searchStart, activeCall).contains("try {"));

        String worksiteText = joinUnder(textByFile, "/feature/rendering/worksite/");
        assertFalse(Pattern.compile("Map\\s*<\\s*(?:BlockPos|Vec3)").matcher(worksiteText).find());
        assertFalse(worksiteText.contains("String.format("));
        assertFalse(worksiteText.contains(".stream()"));

        String pumpkin = textByFile.get(MAIN.resolve("dev/chise/chisetweaks/feature/building/PumpkinScaffoldFeature.java"));
        assertFalse(pumpkin.contains("implements TickingFeature"));
        assertFalse(pumpkin.contains("void tick("));

        String scanner = textByFile.get(MAIN.resolve("dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java"));
        for (String required : List.of(
                "MutableBlockPos",
                "PriorityQueue<WorksiteScanCandidate>",
                "candidatePool",
                "MAX_SCAN_CANDIDATES",
                "MAX_LINE_OF_SIGHT_RAYS_PER_SCAN",
                "getChunkSource().hasChunk(")) {
            assertTrue(scanner.contains(required), required);
        }
        assertFalse(scanner.contains("origin.offset("));
        assertFalse(scanner.contains("new ScanCandidate("));

        String buildGradle = Files.readString(ROOT.resolve("build.gradle"), StandardCharsets.UTF_8);
        assertTrue(buildGradle.contains("dev.chise.chisetweaks.core.security.SecureConfigStorage"));
        assertTrue(buildGradle.contains("options.release = 25"));
    }

    @Test
    void standaloneMetadataTranslationsMixinsAndWrapperRemainConsistent() throws IOException {
        for (String name : List.of("LICENSE", "LICENSE_MIT", "LICENSE_APACHE-2.0", "NOTICE")) {
            Path path = ROOT.resolve(name);
            assertTrue(Files.isRegularFile(path) && !Files.readString(path).isBlank(), name);
        }

        Map<String, String> properties = readProperties(ROOT.resolve("gradle.properties"));
        assertTrue(Set.of("unresolved", "resolved").contains(properties.get("provenance_status")));
        for (String key : List.of(
                "mod_version", "minecraft_version", "loader_version", "fabric_api_version",
                "sodium_version", "sodium_compat_version", "jacoco_version")) {
            assertTrue(properties.containsKey(key) && !properties.get(key).isBlank(), key);
        }
        assertFalse(properties.containsKey("malilib_version"));
        assertFalse(properties.containsKey("modmenu_version"));

        Map<String, String> en = jsonStringMap(LANG.resolve("en_us.json"));
        Map<String, String> ja = jsonStringMap(LANG.resolve("ja_jp.json"));
        assertEquals(en.keySet(), ja.keySet());
        en.forEach((key, value) -> assertFalse(value.isBlank(), "en_us:" + key));
        ja.forEach((key, value) -> assertFalse(value.isBlank(), "ja_jp:" + key));

        Pattern translation = Pattern.compile("(?:translatable|translate)\\(\\s*\"([a-z0-9_.-]+)\"");
        for (Path path : javaFiles()) {
            Matcher matcher = translation.matcher(Files.readString(path, StandardCharsets.UTF_8));
            while (matcher.find()) {
                String key = matcher.group(1);
                if (key.startsWith("chisetweaks.") || key.startsWith("config.") || key.startsWith("help.")) {
                    assertTrue(en.containsKey(key), path + " -> " + key);
                }
            }
        }

        JsonObject fabric = jsonObject(RESOURCES.resolve("fabric.mod.json"));
        assertEquals("client", fabric.get("environment").getAsString());
        assertEquals("${version}", fabric.get("version").getAsString());

        JsonObject entrypoints = fabric.getAsJsonObject("entrypoints");
        assertTrue(entrypoints.has("client"));
        assertFalse(entrypoints.has("main"));
        assertFalse(entrypoints.has("modmenu"));

        JsonObject depends = fabric.getAsJsonObject("depends");
        assertEquals("${minecraft_version}", depends.get("minecraft").getAsString());
        assertEquals(">=${loader_version}", depends.get("fabricloader").getAsString());
        assertEquals(">=${fabric_api_version}", depends.get("fabric-api").getAsString());
        assertEquals(">=25", depends.get("java").getAsString());
        assertFalse(depends.has("malilib"));
        assertFalse(depends.has("modmenu"));

        JsonObject recommends = fabric.getAsJsonObject("recommends");
        assertEquals(">=${sodium_compat_version}", recommends.get("sodium").getAsString());
        assertFalse(recommends.has("modmenu"));

        JsonObject chiseMetadata = fabric.getAsJsonObject("custom").getAsJsonObject("chisetweaks");
        assertFalse(chiseMetadata.get("serverInstallationRequired").getAsBoolean());
        assertFalse(chiseMetadata.get("customPlayProtocol").getAsBoolean());
        assertEquals("standalone", chiseMetadata.get("settingsOwnership").getAsString());
        assertFalse(chiseMetadata.get("externalConfigLibraryRequired").getAsBoolean());
        assertFalse(chiseMetadata.get("modMenuRequired").getAsBoolean());

        Set<String> configuredMixins = new HashSet<>();
        fabric.getAsJsonArray("mixins").forEach(value -> configuredMixins.add(value.getAsString()));
        Set<String> resourceMixins = new HashSet<>();
        try (Stream<Path> files = Files.list(RESOURCES)) {
            files.filter(path -> path.getFileName().toString().endsWith(".mixins.json"))
                    .forEach(path -> resourceMixins.add(path.getFileName().toString()));
        }
        assertEquals(resourceMixins, configuredMixins);
        for (String configName : configuredMixins) {
            JsonObject config = jsonObject(RESOURCES.resolve(configName));
            assertEquals("JAVA_25", config.get("compatibilityLevel").getAsString(), configName);
            assertFalse(config.get("required").getAsBoolean(), configName);
            assertEquals(0, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt(), configName);
            if (config.has("plugin")) {
                Path pluginPath = MAIN.resolve(config.get("plugin").getAsString().replace('.', '/') + ".java");
                assertTrue(Files.isRegularFile(pluginPath), pluginPath.toString());
            }
        }

        String wrapper = Files.readString(ROOT.resolve("gradle/wrapper/gradle-wrapper.properties"), StandardCharsets.UTF_8);
        assertTrue(wrapper.contains("distributionSha256Sum="));
        assertTrue(wrapper.contains("validateDistributionUrl=true"));
    }

    @Test
    void githubActionsKeepLeastPrivilegePinnedDependenciesAndAutomatedReleaseMetadata() throws IOException {
        for (String workflow : List.of("ci.yml", "verify-build.yml", "release.yml")) {
            assertTrue(Files.isRegularFile(WORKFLOWS.resolve(workflow)), workflow);
        }
        String ci = Files.readString(WORKFLOWS.resolve("ci.yml"), StandardCharsets.UTF_8);
        String verify = Files.readString(WORKFLOWS.resolve("verify-build.yml"), StandardCharsets.UTF_8);
        String release = Files.readString(WORKFLOWS.resolve("release.yml"), StandardCharsets.UTF_8);
        String all = ci + "\n" + verify + "\n" + release;

        assertFalse(all.contains("pull_request_target:"));
        assertFalse(Pattern.compile("uses:\\s+[^\\s]+@v\\d+\\s*$", Pattern.MULTILINE).matcher(all).find());
        assertTrue(ci.contains("contents: read"));
        assertTrue(verify.contains("contents: read"));
        assertTrue(release.contains("contents: write"));

        for (String action : List.of(
                "actions/checkout@v7.0.1",
                "actions/setup-java@v5.7.0",
                "actions/setup-python@v6.0.0",
                "gradle/actions/setup-gradle@v6.3.0",
                "actions/upload-artifact@v7.0.1")) {
            assertTrue(verify.contains(action), action);
        }
        assertTrue(release.contains("actions/download-artifact@v8.0.1"));
        assertTrue(verify.contains("./gradlew --no-daemon --stacktrace clean qualityGate build"));
        assertTrue(release.contains("sha256sum --check SHA256SUMS.txt"));
        assertTrue(release.contains("workflow_dispatch:"));
        assertTrue(release.contains("github.ref != 'refs/heads/main'"));
        assertTrue(release.contains("needs.verify.outputs.minecraft_version"));
        assertTrue(release.contains("tag=v${VERSION}"));
        assertTrue(release.contains("ChiseTweaks ${DISPLAY_VERSION} — Minecraft ${MC_VERSION}"));
        assertTrue(release.contains("release/quality-summary.md"));

        String buildGradle = Files.readString(ROOT.resolve("build.gradle"), StandardCharsets.UTF_8);
        for (String contract : List.of(
                "preserveFileTimestamps = false",
                "reproducibleFileOrder = true",
                "jacocoTestCoverageVerification",
                "minimum = 0.96",
                "mutationThreshold = 96",
                "testStrengthThreshold = 96")) {
            assertTrue(buildGradle.contains(contract), contract);
        }
    }

    private static List<Path> javaFiles() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".java")).toList();
        }
    }

    private static String joinUnder(Map<Path, String> textByFile, String marker) {
        return textByFile.entrySet().stream()
                .filter(entry -> entry.getKey().toString().replace('\\', '/').contains(marker))
                .map(Map.Entry::getValue)
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private static boolean containsAnyPart(Path path, Set<String> parts) {
        for (Path part : path) if (parts.contains(part.toString())) return true;
        return false;
    }

    private static String chars(int... values) {
        StringBuilder builder = new StringBuilder(values.length);
        for (int value : values) builder.append((char) value);
        return builder.toString();
    }

    private static Map<String, String> readProperties(Path path) throws IOException {
        Map<String, String> result = new HashMap<>();
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int separator = line.indexOf('=');
            assertTrue(separator > 0, "malformed property: " + raw);
            result.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
        }
        return result;
    }

    private static JsonObject jsonObject(Path path) throws IOException {
        JsonElement parsed = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonElement.class);
        assertNotNull(parsed, path.toString());
        assertTrue(parsed.isJsonObject(), path.toString());
        return parsed.getAsJsonObject();
    }

    private static Map<String, String> jsonStringMap(Path path) throws IOException {
        JsonObject object = jsonObject(path);
        Map<String, String> result = new HashMap<>();
        object.entrySet().forEach(entry -> {
            assertTrue(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString(), entry.getKey());
            result.put(entry.getKey(), entry.getValue().getAsString());
        });
        return result;
    }
}
