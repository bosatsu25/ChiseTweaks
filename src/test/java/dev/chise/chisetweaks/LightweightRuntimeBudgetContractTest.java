package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 実行時性能を犠牲にせず、配布用JARの軽量性を固定する契約テスト。 */
final class LightweightRuntimeBudgetContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void runtimeJarHasSingleFourHundredKilobyteLimitAndOptimizedIconContract() throws IOException {
        String budget = source("gradle/chise-lightweight-budget.gradle");
        String properties = source("gradle.properties");
        String build = source("build.gradle");
        String settings = source("settings.gradle");

        assertTrue(properties.contains("runtime_jar_max_bytes=400000"));
        assertTrue(properties.contains("runtime_icon_target_pixels=256"));
        assertTrue(budget.contains("project.property('runtime_jar_max_bytes')"));
        assertTrue(budget.contains("project.property('runtime_icon_target_pixels')"));
        assertTrue(budget.contains("if (size >= CHISE_RUNTIME_JAR_MAX_BYTES)"));
        assertTrue(budget.contains("CHISE_RUNTIME_ICON_PATH = 'assets/chisetweaks/icon.png'"));
        assertTrue(budget.contains("iconImage.width != CHISE_RUNTIME_ICON_PIXELS"));
        assertTrue(budget.contains("dependsOn 'jar'"));
        assertTrue(budget.contains("it.name == 'check' || it.name == 'qualityGate'"));
        assertTrue(build.contains("new File(outputs.files.singleFile, runtimeIconRelativePath)"));
        assertTrue(settings.contains("gradle/chise-lightweight-budget.gradle"));

        assertFalse(budget.contains("700000"));
        assertFalse(budget.contains("1_000_000L"));
        assertFalse(budget.contains("1_500_000L"));
    }

    @Test
    void runtimeJarUsesDeterministicMaximumDeflateWithoutRemovingRuntimeContents() throws IOException {
        String budget = source("gradle/chise-lightweight-budget.gradle");

        assertTrue(budget.contains("output.setLevel(9)"));
        assertTrue(budget.contains("targetEntry.setTime(0L)"));
        assertTrue(budget.contains("targetEntry.setMethod(ZipEntry.DEFLATED)"));
        assertTrue(budget.contains("sourceArchive.getInputStream(entry).bytes"));
        assertTrue(budget.contains("recompressedSize < originalSize"));
        assertTrue(budget.contains("StandardCopyOption.ATOMIC_MOVE"));
    }

    @Test
    void sizeBudgetDoesNotIntroduceRuntimeMinifiersOrBundledUpdaterBehavior() throws IOException {
        String budget = source("gradle/chise-lightweight-budget.gradle");
        String build = source("build.gradle");

        assertFalse(budget.contains("proguard"));
        assertFalse(budget.contains("shadowJar"));
        assertFalse(budget.contains("HttpClient"));
        assertFalse(budget.contains("URL("));
        assertFalse(build.contains("-g:none"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
