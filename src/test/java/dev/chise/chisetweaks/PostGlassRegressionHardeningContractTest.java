package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards startup, visual-feature isolation and release-pipeline hardening. */
final class PostGlassRegressionHardeningContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void runtimeKeepsWorksiteHighlightsIndependentWhileRetainingBoundedScanning() throws IOException {
        String manager = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java");
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        assertTrue(manager.contains("PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)"));
        assertTrue(manager.contains("hasAvailableWorksiteVisibilityFeature()"));
        assertTrue(manager.contains("definition.isWorksiteVisibilityMode()"));
        assertTrue(manager.contains("PreReleaseFeaturePolicy.isAvailable(definition)"));
        assertTrue(manager.contains("if (tickSchedule.length != 0)"));
        assertFalse(bindings.contains("bindExclusiveWorksiteMode"));
        assertFalse(bindings.contains("applyWorksiteModesAtomically"));
        assertFalse(bindings.contains("worksiteVisibilityExclusiveMode"));
    }

    @Test
    void releasedBuilderFocusRemainsIsolatedFromConfigLayer() throws IOException {
        String config = source("src/main/java/dev/chise/chisetweaks/config/FeatureConfig.java");
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");

        assertFalse(config.contains("BuilderFocusVisibility"));
        assertFalse(config.contains("PreReleaseFeaturePolicy"));
        assertTrue(bindings.contains("PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.BUILDER_FOCUS_BLOCKS)"));
        assertTrue(bindings.contains("PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.BUILDER_FOCUS_ENTITIES)"));
        assertTrue(bindings.contains("if (builderFocusAvailable())"));
        assertTrue(bindings.contains("BuilderFocusVisibility.applyConfig();"));
    }

    @Test
    void compatibilitySensitiveMixinsRemainPolicyGatedBeforeApplication() throws IOException {
        String config = source("src/main/resources/chisetweaks.features.mixins.json");
        String plugin = source("src/main/java/dev/chise/chisetweaks/mixin/FeatureAvailabilityMixinConfigPlugin.java");
        assertTrue(config.contains("\"plugin\": \"dev.chise.chisetweaks.mixin.FeatureAvailabilityMixinConfigPlugin\""));
        assertTrue(plugin.contains("BuilderFocusBlockMixin"));
        assertTrue(plugin.contains("BuilderFocusEntityMixin"));
        assertTrue(plugin.contains("FireVisibilityMixin"));
        assertTrue(plugin.contains("return feature != null && FeatureAvailabilityPolicy.isAvailable(feature);"));
        assertFalse(plugin.contains("import net.minecraft"));
    }

    @Test
    void reflectiveSourceAuditAccountsForMixinConfigPlugins() throws IOException {
        String audit = source("scripts/source_usage_audit.py");
        assertTrue(audit.contains("plugin = config.get(\"plugin\")"));
        assertTrue(audit.contains("result.add(plugin.strip())"));
    }

    @Test
    void externalOreRegistrationsAreBoundedAndValidated() throws IOException {
        String registry = source("src/main/java/dev/chise/chisetweaks/core/vision/OreHighlightExternalRegistry.java");
        assertTrue(registry.contains("MAX_API_BLOCKS = 2048"));
        assertTrue(registry.contains("MAX_API_TAGS = 256"));
        assertTrue(registry.contains("MAX_PUBLISHED_BLOCKS = 512"));
        assertTrue(registry.contains("Identifier.tryParse(normalized)"));
        assertTrue(registry.contains("\"minecraft\".equals(id.getNamespace())"));
        assertTrue(registry.contains("Ore Highlight API block registration limit exceeded"));
        assertTrue(registry.contains("Ore Highlight API tag registration limit exceeded"));
    }

    @Test
    void visualAssetsAreAuditedWhileCiRetainsOnlyTheRuntimeJar() throws IOException {
        String audit = source("scripts/visual_asset_audit.py");
        String verify = source(".github/workflows/verify-build.yml");
        String release = source(".github/workflows/release.yml");
        assertTrue(audit.contains("zipfile.ZipFile(jar)"));
        assertTrue(audit.contains("KELP_ANIMATION"));
        assertTrue(audit.contains("GLASS_BLOCK_MODEL"));
        assertTrue(audit.contains("GLASS_PANE_MODEL"));
        assertTrue(verify.contains("python scripts/visual_asset_audit.py"));
        assertTrue(release.contains("python scripts/visual_asset_audit.py"));
        assertTrue(verify.contains("actions/upload-artifact@"));
        assertTrue(verify.contains("path: build/libs/${{ steps.artifacts.outputs.runtime_jar }}"));
        assertTrue(verify.contains("archive: false"));
        assertFalse(verify.contains("build/ci/visual-asset-audit.json"));
        assertFalse(verify.contains("steps.artifacts.outputs.sources_jar"));
        assertFalse(release.contains("actions/upload-artifact"));
        assertFalse(release.contains("release/visual-asset-audit.json"));
    }

    @Test
    void obsoleteCiRunsCanBeCancelledBecauseCiNeverPublishes() throws IOException {
        String ci = source(".github/workflows/ci.yml");
        assertTrue(ci.contains("cancel-in-progress: true"));
        assertFalse(ci.contains("publish-verified-release"));
        assertFalse(ci.contains("gh release"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
