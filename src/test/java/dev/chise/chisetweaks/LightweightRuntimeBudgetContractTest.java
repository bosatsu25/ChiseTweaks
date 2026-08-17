package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the lightweight distribution budget without encouraging runtime-cost regressions. */
final class LightweightRuntimeBudgetContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void runtimeJarHasOneMegabyteTargetAndFifteenHundredKilobyteHardLimit() throws IOException {
        String budget = source("gradle/chise-lightweight-budget.gradle");
        String settings = source("settings.gradle");

        assertTrue(budget.contains("CHISE_RUNTIME_JAR_TARGET_BYTES = 1_000_000L"));
        assertTrue(budget.contains("CHISE_RUNTIME_JAR_HARD_LIMIT_BYTES = 1_500_000L"));
        assertTrue(budget.contains("if (size >= CHISE_RUNTIME_JAR_HARD_LIMIT_BYTES)"));
        assertTrue(budget.contains("dependsOn 'jar'"));
        assertTrue(budget.contains("it.name == 'check' || it.name == 'qualityGate'"));
        assertTrue(settings.contains("gradle/chise-lightweight-budget.gradle"));
    }

    @Test
    void sizeBudgetDoesNotIntroduceRuntimeMinifiersOrBundledUpdaterBehavior() throws IOException {
        String budget = source("gradle/chise-lightweight-budget.gradle");
        assertFalse(budget.contains("proguard"));
        assertFalse(budget.contains("shadowJar"));
        assertFalse(budget.contains("HttpClient"));
        assertFalse(budget.contains("URL("));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
