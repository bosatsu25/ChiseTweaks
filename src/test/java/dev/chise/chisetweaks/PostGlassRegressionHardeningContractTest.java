package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the startup, zero-scan and release-pipeline hardening added after Glass Highlight. */
final class PostGlassRegressionHardeningContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void runtimeRegistersOnlyAvailableTickFeaturesAndDoesNotReviveLockedWorksiteStack() throws IOException {
        String manager = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureManager.java");
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        assertTrue(manager.contains("PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.LAVA_HIGHLIGHT)"));
        assertTrue(manager.contains("hasAvailableWorksiteVisibilityFeature()"));
        assertTrue(manager.contains("definition.isWorksiteVisibilityMode()"));
        assertTrue(manager.contains("PreReleaseFeaturePolicy.isAvailable(definition)"));
        assertTrue(manager.contains("if (tickSchedule.length != 0)"));
        assertTrue(bindings.contains(".filter(toggle -> PreReleaseFeaturePolicy.isAvailable(toggle.definition()))"));
        assertTrue(bindings.contains("if (!WORKSITE_VISIBILITY_TOGGLES.isEmpty()) bindWorksiteVisibilityCallbacks();"));
    }

    @Test
    void lockedBuilderFocusDoesNotCompileRegistryRulesOrWireCallbacksAtStartup() throws IOException {
        String config = source("src/main/java/dev/chise/chisetweaks/config/FeatureConfig.java");
        String bindings = source("src/main/java/dev/chise/chisetweaks/runtime/FeatureControlBindings.java");
        for (String source : new String[] {config, bindings}) {
            assertTrue(source.contains("PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.BUILDER_FOCUS_BLOCKS)"));
            assertTrue(source.contains("PreReleaseFeaturePolicy.isAvailable(FeatureDefinition.BUILDER_FOCUS_ENTITIES)"));
        }
        assertTrue(config.contains("if (builderFocusAvailable()) BuilderFocusVisibility.applyConfig();"));
        assertTrue(bindings.contains("if (builderFocusAvailable())"));
    }

    @Test
    void lockedCompatibilitySensitiveMixinsAreFailClosedBeforeApplication() throws IOException {
        String config = source("src/main/resources/chisetweaks.features.mixins.json");
        String plugin = source("src/main/java/dev/chise/chisetweaks/mixin/PreReleaseMixinConfigPlugin.java");
        assertTrue(config.contains("\"plugin\": \"dev.chise.chisetweaks.mixin.PreReleaseMixinConfigPlugin\""));
        assertTrue(plugin.contains("BuilderFocusBlockMixin"));
        assertTrue(plugin.contains("BuilderFocusEntityMixin"));
        assertTrue(plugin.contains("FireVisibilityMixin"));
        assertTrue(plugin.contains("return feature != null && PreReleaseFeaturePolicy.isAvailable(feature);"));
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
    void visualAssetsAreAuditedFromTheBuiltJarAndShippedAsEvidence() throws IOException {
        String audit = source("scripts/visual_asset_audit.py");
        String verify = source(".github/workflows/verify-build.yml");
        String ci = source(".github/workflows/ci.yml");
        String release = source(".github/workflows/release.yml");
        assertTrue(audit.contains("zipfile.ZipFile(jar)"));
        assertTrue(audit.contains("KELP_ANIMATION"));
        assertTrue(audit.contains("GLASS_BLOCK_MODEL"));
        assertTrue(audit.contains("GLASS_PANE_MODEL"));
        assertTrue(verify.contains("python scripts/visual_asset_audit.py"));
        assertTrue(verify.contains("cp build/ci/visual-asset-audit.json build/verified/"));
        assertTrue(ci.contains("ensure_asset 'release/visual-asset-audit.json'"));
        assertTrue(ci.contains("artifact-audit.json visual-asset-audit.json quality-summary.md"));
        assertTrue(release.contains("Verified visual asset audit evidence is missing"));
        assertTrue(release.contains("\"release/visual-asset-audit.json\""));
    }

    @Test
    void verifiedMainPublicationCannotBeCancelledByANewerPush() throws IOException {
        String ci = source(".github/workflows/ci.yml");
        assertTrue(ci.contains("cancel-in-progress: ${{ github.event_name == 'pull_request' }}"));
        assertFalse(ci.contains("cancel-in-progress: true"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
