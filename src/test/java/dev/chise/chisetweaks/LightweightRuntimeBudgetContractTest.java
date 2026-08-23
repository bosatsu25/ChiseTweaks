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
    void runtimeJarUsesVerifiedBaselineAndBoundedGrowthBudget() throws IOException {
        String budget = source("gradle/chise-lightweight-budget.gradle");
        String jarSize = source("gradle/chise-jar-size.gradle");
        String properties = source("gradle.properties");
        String build = source("build.gradle");
        String settings = source("settings.gradle");

        assertTrue(properties.contains("runtime_jar_target_bytes=400000"));
        assertTrue(properties.contains("runtime_jar_baseline_bytes=441198"));
        assertTrue(properties.contains("runtime_jar_max_growth_bytes=40000"));
        assertTrue(properties.contains("runtime_jar_max_bytes=500000"));
        assertTrue(properties.contains("runtime_icon_target_pixels=128"));
        assertTrue(budget.contains("project.property('runtime_jar_target_bytes')"));
        assertTrue(budget.contains("project.property('runtime_jar_baseline_bytes')"));
        assertTrue(budget.contains("project.property('runtime_jar_max_growth_bytes')"));
        assertTrue(budget.contains("project.property('runtime_jar_max_bytes')"));
        assertTrue(budget.contains("CHISE_RUNTIME_JAR_EFFECTIVE_MAX_BYTES = Math.min("));
        assertTrue(budget.contains("if (size >= CHISE_RUNTIME_JAR_TARGET_BYTES)"));
        assertTrue(budget.contains("if (size >= CHISE_RUNTIME_JAR_EFFECTIVE_MAX_BYTES)"));
        assertTrue(budget.contains("CHISE_RUNTIME_ICON_PATH = 'assets/chisetweaks/icon.png'"));
        assertTrue(budget.contains("iconImage.width != CHISE_RUNTIME_ICON_PIXELS"));
        assertTrue(budget.contains("dependsOn 'jar'"));
        assertTrue(budget.contains("it.name == 'check' || it.name == 'qualityGate'"));
        assertTrue(build.contains("new File(outputs.files.singleFile, runtimeIconRelativePath)"));
        assertTrue(settings.contains("gradle/chise-lightweight-budget.gradle"));
        assertTrue(settings.contains("gradle/chise-jar-size.gradle"));
        assertTrue(jarSize.contains("options.debug = true"));
        assertTrue(jarSize.contains("options.debugOptions.debugLevel = 'source,lines'"));
    }

    @Test
    void sizeBudgetDoesNotRewriteOrMinifyTheRuntimeJar() throws IOException {
        String budget = source("gradle/chise-lightweight-budget.gradle");
        String build = source("build.gradle");

        assertFalse(budget.contains("ZipOutputStream"));
        assertFalse(budget.contains("output.setLevel"));
        assertFalse(budget.contains("StandardCopyOption"));
        assertFalse(budget.contains("recompressRuntimeJar"));
        assertFalse(budget.contains("proguard"));
        assertFalse(budget.contains("shadowJar"));
        assertFalse(budget.contains("HttpClient"));
        assertFalse(budget.contains("URL("));
        assertFalse(build.contains("-g:none"));
    }

    @Test
    void ciInvokesJarBudgetAsAnExplicitRequiredGate() throws IOException {
        String verify = source(".github/workflows/verify-build.yml");
        assertTrue(verify.contains("id: size"));
        assertTrue(verify.contains("./gradlew --no-daemon --stacktrace verifyRuntimeJarBudget"));
        assertTrue(verify.contains("SIZE_OUTCOME: ${{ steps.size.outcome }}"));
        assertTrue(verify.contains("\"size:$SIZE_OUTCOME\""));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
