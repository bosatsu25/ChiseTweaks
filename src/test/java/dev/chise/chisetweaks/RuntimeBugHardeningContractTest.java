package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Source contracts for runtime bugs that depend on Minecraft/Sodium integration surfaces. */
final class RuntimeBugHardeningContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    @Test
    void sodiumLavaHighlightDoesNotTreatOpaqueWhiteAsNoHighlight() throws IOException {
        String mixin = read("src/main/java/dev/chise/chisetweaks/mixin/sodium/LavaHighlightRendererMixin.java");

        assertTrue(mixin.contains("boolean applyHighlight = false"));
        assertTrue(mixin.contains("if (!applyHighlight)"));
        assertFalse(mixin.contains("requestedColor = -1"));
        assertFalse(mixin.contains("requestedColor == -1"));
    }

    @Test
    void sceneFilterEntityRenderPathUsesCachedEntityTypesWithoutPerEntityCollections() throws IOException {
        String source = read("src/main/java/dev/chise/chisetweaks/feature/rendering/BuilderFocusVisibility.java");
        String hotPath = between(
                source,
                "public static boolean shouldHide(Entity entity)",
                "public static boolean applyPreset");

        assertTrue(hotPath.contains("FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue()"));
        assertTrue(hotPath.contains("entity == client.player"));
        assertTrue(hotPath.contains("entityRules.hides(type)"));
        assertFalse(hotPath.contains("BuilderEntityVisibilityPolicy.Input"));
        assertFalse(hotPath.contains("new LinkedHashSet"));
        assertFalse(hotPath.contains("Set.copyOf"));
        assertFalse(hotPath.contains(".toString()"));
    }

    @Test
    void sceneFilterBlockRenderPathUsesCompiledBlockSetsWithoutPerBlockStrings() throws IOException {
        String source = read("src/main/java/dev/chise/chisetweaks/feature/rendering/BuilderFocusVisibility.java");
        String hotPath = between(
                source,
                "public static boolean shouldHide(Block block)",
                "public static boolean shouldHide(EntityType<?> type)");

        assertTrue(hotPath.contains("blockRules.hides(block)"));
        assertTrue(hotPath.contains("BuiltInRegistries.BLOCK.getKey(block) == null"));
        assertFalse(hotPath.contains(".toString()"));
        assertFalse(hotPath.contains("Set.copyOf"));
        assertFalse(hotPath.contains("new "));
        assertTrue(source.contains("private static BlockRules compileBlockRules"));
        assertTrue(source.contains("for (Block block : BuiltInRegistries.BLOCK)"));
    }

    @Test
    void worksiteInspectorFailsOpenForMissingRegistryIdentity() throws IOException {
        String source = read("src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteBlockInspector.java");

        assertTrue(source.contains("Identifier registryId = BuiltInRegistries.BLOCK.getKey(block)"));
        assertTrue(source.contains("registryId == null ? \"\" : registryId.toString()"));
        assertTrue(source.contains("id.isEmpty()"));
        assertFalse(source.contains("BuiltInRegistries.BLOCK.getKey(block).toString()"));
    }

    @Test
    void featureConfigContainsRuntimeFailuresAtBothPersistenceBoundaries() throws IOException {
        String source = read("src/main/java/dev/chise/chisetweaks/config/FeatureConfig.java");

        assertEquals(2, occurrences(source, "catch (IOException | RuntimeException exception)"));
    }

    @Test
    void failedVisualResourceReloadDoesNotPretendTheStagedModelMaskWasApplied() throws IOException {
        String coordinator = read(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/model/VisualModelReloadCoordinator.java");

        assertTrue(coordinator.contains("stagedMaterialModelMask"));
        assertTrue(coordinator.contains("stagedStateKnown"));
        assertTrue(coordinator.contains("commitStagedState()"));
        assertTrue(coordinator.contains("discardStagedState()"));
        String failureBranch = between(coordinator, "} else {", "RELOAD_IN_FLIGHT.set(false)");
        assertTrue(failureBranch.contains("discardStagedState()"));
        assertTrue(failureBranch.contains("RELOAD_THROTTLE.onReloadFailed()"));
        assertFalse(failureBranch.contains("commitStagedState()"));
    }

    @Test
    void visualReloadThrottleSerializesTickAndCompletionState() throws IOException {
        String throttle = read(
                "src/main/java/dev/chise/chisetweaks/core/performance/VisualModelReloadThrottlePolicy.java");

        assertTrue(throttle.contains("public synchronized boolean shouldRequestReload"));
        assertTrue(throttle.contains("public synchronized void onReloadSucceeded"));
        assertTrue(throttle.contains("public synchronized void onReloadFailed"));
        assertTrue(throttle.contains("public synchronized void reset"));
        assertTrue(throttle.contains("public synchronized int retryCooldownTicks"));
    }

    @Test
    void sceneFilterEditorUsesBoundedValidatedListsAndCompactControls() throws IOException {
        String editor = read("src/main/java/dev/chise/chisetweaks/gui/ChiseSceneFilterEditorScreen.java");

        assertTrue(editor.contains("Identifier.tryParse(raw)"));
        assertTrue(editor.contains("ConfigListPolicy.sanitize(updated)"));
        assertTrue(editor.contains("ConfigListPolicy.MAX_ENTRIES"));
        assertTrue(editor.contains("if (!isRegisteredTarget(id))"));
        assertTrue(editor.contains("for (Block block : BuiltInRegistries.BLOCK)"));
        assertTrue(editor.contains("for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)"));
        assertTrue(editor.contains("case NONE -> ChiseRuleMode.BLACKLIST"));
        assertTrue(editor.contains("case BLACKLIST -> ChiseRuleMode.WHITELIST"));
        assertTrue(editor.contains("pageSize = Math.max(2, Math.min(10"));
        assertTrue(editor.contains(".bounds(panelX + 8, footerY, 58, 20)"));
        assertTrue(editor.contains(".bounds(panelX + 132, footerY, 104, 20)"));
        assertTrue(editor.contains(".bounds(panelX + panelWidth - 88, footerY, 80, 20)"));
    }

    @Test
    void helpDirectionsUseTheCurrentStandaloneNavigation() throws IOException {
        String ja = read("src/main/resources/assets/chisetweaks/lang/ja_jp.json");
        String en = read("src/main/resources/assets/chisetweaks/lang/en_us.json");

        assertTrue(ja.contains("「見やすさ」で「ブロックの対象」を開き"));
        assertTrue(ja.contains("「見やすさ」で「エンティティの対象」を開き"));
        assertTrue(ja.contains("「資源」で鉱石・資源ハイライトを有効にし"));
        assertFalse(ja.contains("\"help.chisetweaks.builder_focus_blocks.usage\": \"Rendering"));
        assertFalse(ja.contains("\"help.chisetweaks.builder_focus_entities.usage\": \"Rendering"));
        assertFalse(ja.contains("\"help.chisetweaks.lava_highlight.usage\": \"Rendering"));

        assertTrue(en.contains("Under Visibility, open Block targets"));
        assertTrue(en.contains("Under Visibility, open Entity targets"));
        assertTrue(en.contains("Under Resources, enable Ore Highlights"));
        assertFalse(en.contains("enable it under Rendering"));
        assertFalse(en.contains("Enable it under Rendering"));
        assertFalse(en.contains("in Target Lists"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static String between(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, startToken);
        assertTrue(end > start, endToken);
        return source.substring(start, end);
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
