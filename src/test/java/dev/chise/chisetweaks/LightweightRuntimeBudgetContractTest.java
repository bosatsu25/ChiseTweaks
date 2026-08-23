package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 実行時性能・機能等価性を犠牲にせず、配布用JARの軽量性を固定する契約テスト。 */
final class LightweightRuntimeBudgetContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void runtimeJarUsesFrozen094BaselineAnd350KibGoal() throws IOException {
        String budget = source("gradle/chise-lightweight-budget.gradle");
        String jarSize = source("gradle/chise-jar-size.gradle");
        String properties = source("gradle.properties");
        String build = source("build.gradle");
        String settings = source("settings.gradle");

        assertTrue(properties.contains("runtime_jar_target_bytes=358400"));
        assertTrue(properties.contains("runtime_jar_baseline_bytes=446814"));
        assertTrue(properties.contains("runtime_jar_max_growth_bytes=0"));
        assertTrue(properties.contains("runtime_jar_max_bytes=446814"));
        assertTrue(properties.contains("runtime_icon_target_pixels=128"));
        assertTrue(budget.contains("project.property('runtime_jar_target_bytes')"));
        assertTrue(budget.contains("project.property('runtime_jar_baseline_bytes')"));
        assertTrue(budget.contains("project.property('runtime_jar_max_growth_bytes')"));
        assertTrue(budget.contains("project.property('runtime_jar_max_bytes')"));
        assertTrue(budget.contains("CHISE_RUNTIME_JAR_EFFECTIVE_MAX_BYTES = Math.min("));
        assertTrue(budget.contains("if (size > CHISE_RUNTIME_JAR_TARGET_BYTES)"));
        assertTrue(budget.contains("if (size > CHISE_RUNTIME_JAR_EFFECTIVE_MAX_BYTES)"));
        assertTrue(budget.contains("CHISE_RUNTIME_ICON_PATH = 'assets/chisetweaks/icon.png'"));
        assertTrue(budget.contains("iconImage.width != CHISE_RUNTIME_ICON_PIXELS"));
        assertTrue(budget.contains("dependsOn 'compactRuntimeJar'"));
        assertTrue(budget.contains("it.name == 'check' || it.name == 'qualityGate'"));
        assertTrue(build.contains("new File(outputs.files.singleFile, runtimeIconRelativePath)"));
        assertTrue(settings.contains("gradle/chise-lightweight-budget.gradle"));
        assertTrue(settings.contains("gradle/chise-jar-size.gradle"));
        assertTrue(jarSize.contains("options.debug = true"));
        assertTrue(jarSize.contains("options.debugOptions.debugLevel = 'source,lines'"));
    }

    @Test
    void mandatoryArtifactAuditEnforcesNoGrowthAndCiRunsItOnce() throws IOException {
        String audit = source("scripts/artifact_audit.py");
        String ci = source(".github/workflows/ci.yml");
        String release = source(".github/workflows/release.yml");

        assertTrue(audit.contains("runtime_jar_target_bytes"));
        assertTrue(audit.contains("runtime_jar_baseline_bytes"));
        assertTrue(audit.contains("runtime_jar_max_growth_bytes"));
        assertTrue(audit.contains("effective_max = min(absolute_max, baseline + max_growth)"));
        assertTrue(audit.contains("if size > effective_max:"));
        assertTrue(audit.contains("info.flag_bits & 0x08"));
        assertTrue(audit.contains("if info.extra"));
        assertTrue(audit.contains("Remaining to goal"));
        assertTrue(ci.contains("python scripts/artifact_audit.py"));
        assertTrue(release.contains("actions/download-artifact@"));
        assertFalse(release.contains("python scripts/artifact_audit.py"));
        assertFalse(release.contains("./gradlew"));
    }

    @Test
    void runtimeJarUsesDeterministicContainerCompressionWithoutShrinkingOrMinifyingPayloads() throws IOException {
        String jarSize = source("gradle/chise-jar-size.gradle");
        String build = source("build.gradle");

        assertTrue(jarSize.contains("ZipOutputStream"));
        assertTrue(jarSize.contains("output.setLevel(9)"));
        assertTrue(jarSize.contains("if (!sourceEntry.directory)"));
        assertTrue(jarSize.contains("stream.readAllBytes()"));
        assertTrue(jarSize.contains("new Deflater(9, true)"));
        assertTrue(jarSize.contains("new CRC32()"));
        assertTrue(jarSize.contains("setTimeLocal(LocalDateTime.of(1980, 1, 2, 0, 0))"));
        assertTrue(jarSize.contains("targetEntry.setCompressedSize"));
        assertTrue(jarSize.contains("targetEntry.setCrc"));
        assertTrue(jarSize.contains("ZipEntry.STORED : ZipEntry.DEFLATED"));
        assertTrue(jarSize.contains("tasks.register('compactRuntimeJar')"));
        assertTrue(jarSize.contains("dependsOn 'jar'"));
        assertTrue(jarSize.contains("task.name == 'assemble'"));
        assertTrue(jarSize.contains("finalizedBy compactRuntimeJar"));
        assertTrue(jarSize.contains("StandardCopyOption.REPLACE_EXISTING"));
        assertFalse(jarSize.toLowerCase().contains("proguard"));
        assertFalse(jarSize.toLowerCase().contains("shadowjar"));
        assertFalse(jarSize.contains("HttpClient"));
        assertFalse(jarSize.contains("URL("));
        assertFalse(build.contains("-g:none"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
