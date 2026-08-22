package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the separation between internal QA evidence and the one JAR users install in Prism. */
final class ReleaseDistributionContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void verificationEvidenceStaysCompleteInsideActionsArtifact() throws IOException {
        String verify = source(".github/workflows/verify-build.yml");

        assertTrue(verify.contains("-verification-evidence"));
        assertTrue(verify.contains("Stage verification evidence"));
        assertTrue(verify.contains("outputs.runtime_jar"));
        assertTrue(verify.contains("outputs.sources_jar"));
        assertTrue(verify.contains("SHA256SUMS.txt"));
        assertTrue(verify.contains("artifact-audit.json"));
        assertTrue(verify.contains("visual-asset-audit.json"));
        assertTrue(verify.contains("quality-summary.md"));
        assertTrue(verify.contains("actions/upload-artifact@v7.0.1"));
        assertFalse(verify.contains("-verified-JAR"));
    }

    @Test
    void verifiedPreReleaseExposesExactlyOneRuntimeJar() throws IOException {
        String ci = source(".github/workflows/ci.yml");

        assertTrue(ci.contains("endswith(\"-verification-evidence\")"));
        assertTrue(ci.contains("ensure_runtime_asset"));
        assertTrue(ci.contains("local file=\"release/$RUNTIME_JAR\""));
        assertTrue(ci.contains("asset_count=\"$(jq '[.assets[]?] | length'"));
        assertTrue(ci.contains("Verified release must expose exactly one user asset"));
        assertFalse(ci.contains("ensure_asset \"release/$SOURCES_JAR\""));
        assertFalse(ci.contains("ensure_asset 'release/SHA256SUMS.txt'"));
        assertFalse(ci.contains("ensure_asset 'release/artifact-audit.json'"));
        assertFalse(ci.contains("ensure_asset 'release/visual-asset-audit.json'"));
        assertFalse(ci.contains("ensure_asset 'release/quality-summary.md'"));
    }

    @Test
    void officialReleaseExposesExactlyOneRuntimeJar() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("endswith(\"-verification-evidence\")"));
        assertTrue(release.contains("gh release create \"$TAG\""));
        assertTrue(release.contains("\"release/$RUNTIME_JAR\""));
        assertTrue(release.contains("Official release must expose exactly one user asset"));
        assertFalse(release.contains("\"release/$SOURCES_JAR\""));
        assertFalse(release.contains("\"release/SHA256SUMS.txt\""));
        assertFalse(release.contains("\"release/artifact-audit.json\""));
        assertFalse(release.contains("\"release/visual-asset-audit.json\""));
        assertFalse(release.contains("\"release/quality-summary.md\""));
    }

    @Test
    void bothReleaseSurfacesTellPrismUsersToInstallOnlyTheRuntimeJar() throws IOException {
        String ci = source(".github/workflows/ci.yml");
        String release = source(".github/workflows/release.yml");

        for (String workflow : new String[] {ci, release}) {
            assertTrue(workflow.contains("Install this file / 導入するファイル"));
            assertTrue(workflow.contains("Prism Launcher の Mods には次の1ファイルだけを追加してください"));
            assertTrue(workflow.contains("RUNTIME_JAR"));
        }
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
