package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 検証済みmainだけから単一JARを自動公開するRelease契約を固定する。 */
final class ReleaseWorkflowContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void releaseRunsOnlyAfterSuccessfulMainPushCiAndUsesTheVerifiedCommit() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("workflow_run:"));
        assertTrue(release.contains("- CI"));
        assertTrue(release.contains("- completed"));
        assertTrue(release.contains("- main"));
        assertTrue(release.contains("github.event.workflow_run.conclusion == 'success'"));
        assertTrue(release.contains("github.event.workflow_run.event == 'push'"));
        assertTrue(release.contains("github.event.workflow_run.head_branch == 'main'"));
        assertTrue(release.contains("ref: ${{ github.event.workflow_run.head_sha }}"));
        assertTrue(release.contains("TARGET_SHA: ${{ github.event.workflow_run.head_sha }}"));
        assertTrue(release.contains("git ls-remote origin refs/heads/main"));
        assertTrue(release.contains("published_tag_sha"));
        assertTrue(release.contains("published_tag_sha\" != \"$TARGET_SHA"));
    }

    @Test
    void releasePublishesExactlyOneRuntimeJarAndPreventsDuplicates() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("permissions:\n  contents: write"));
        assertTrue(release.contains("gh release view \"$tag\""));
        assertTrue(release.contains("Tag '$tag' exists without a matching GitHub Release"));
        assertTrue(release.contains("gh release create \"$TAG\""));
        assertTrue(release.contains("\"build/libs/$RUNTIME_JAR\""));
        assertTrue(release.contains("asset_count"));
        assertTrue(release.contains("asset_count\" != '1'"));
        assertTrue(release.contains("Uploaded mod assets: `1`"));

        assertFalse(release.contains("actions/upload-artifact"));
        assertFalse(release.contains("workflow_dispatch:"));
        assertFalse(release.contains("confirm_release"));
        assertFalse(release.contains("cleanup_legacy_verified_tags"));
    }

    @Test
    void releaseRebuildsArtifactsWithoutRepeatingCiQualityTests() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("Rebuild release candidate from verified SHA"));
        assertTrue(release.contains("./gradlew --no-daemon --stacktrace clean assemble"));
        assertTrue(release.contains("python scripts/artifact_audit.py"));
        assertTrue(release.contains("python scripts/visual_asset_audit.py"));
        assertTrue(release.contains("python scripts/release_residue_audit.py"));
        assertFalse(release.contains("clean ciGate"));
        assertFalse(release.contains("--stacktrace test"));
        assertFalse(release.contains("jacocoTestReport"));
        assertFalse(release.contains("pitest"));
        assertFalse(release.contains("python scripts/quality_summary.py"));
    }

    @Test
    void releaseKeepsSemverStrictAndInfersExactPatchMinorOrMajorBumps() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("python scripts/version_policy.py"));
        assertTrue(release.contains("release_type='patch'"));
        assertTrue(release.contains("release_type='minor'"));
        assertTrue(release.contains("release_type='major'"));
        assertTrue(release.contains("not an exact patch/minor/major increment"));
        assertTrue(release.contains("--previous \"$previous_version\""));
        assertTrue(release.contains("--release-type \"$release_type\""));
    }

    @Test
    void releaseRedownloadsAndRevalidatesPublishedJar() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("gh release download \"$TAG\""));
        assertTrue(release.contains("sha256sum \"$published_runtime\""));
        assertTrue(release.contains("unzip -p \"$published_runtime\" fabric.mod.json"));
        assertTrue(release.contains("published_version"));
        assertTrue(release.contains("published_mc"));
        assertTrue(release.contains("published_loader"));
        assertTrue(release.contains("published_fabric_api"));
        assertTrue(release.contains("published_java"));
        assertTrue(release.contains("published_environment"));
        assertTrue(release.contains("published_server_required"));
        assertTrue(release.contains("Published JAR self-verification: `PASS`"));
    }

    @Test
    void releaseNotesStayFocusedOnPrismInstallMetadata() throws IOException {
        String release = source(".github/workflows/release.yml");

        assertTrue(release.contains("Prism Launcher の Mods"));
        assertTrue(release.contains("- Minecraft:"));
        assertTrue(release.contains("- Fabric Loader:"));
        assertTrue(release.contains("- Fabric API:"));
        assertTrue(release.contains("- Java:"));
        assertTrue(release.contains("- SHA-256:"));
        assertFalse(release.contains("manual Prism gameplay smoke test"));
    }

    @Test
    void normalCiRemainsReadOnlyRetainsRuntimeJarAndDoesNotPublish() throws IOException {
        String ci = source(".github/workflows/ci.yml");
        String verify = source(".github/workflows/verify-build.yml");

        assertTrue(ci.contains("permissions:\n  contents: read"));
        assertTrue(verify.contains("permissions:\n  contents: read"));
        assertTrue(verify.contains("actions/upload-artifact@"));
        assertTrue(verify.contains("path: build/libs/${{ steps.artifacts.outputs.runtime_jar }}"));
        assertTrue(verify.contains("archive: false"));
        assertFalse(ci.contains("contents: write"));
        assertFalse(verify.contains("gh release create"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
