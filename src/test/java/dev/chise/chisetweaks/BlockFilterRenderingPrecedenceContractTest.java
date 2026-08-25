package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BlockFilterRenderingPrecedenceContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void blockEntitiesAreStoppedAtTheSharedVisualStateBoundary() throws IOException {
        String mixin = source(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/BlockEntityVisualStateMixin.java");
        String mixinConfig = source("src/main/resources/chisetweaks.features.mixins.json");
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/mixin/FeatureAvailabilityMixinConfigPlugin.java");

        assertTrue(mixin.contains("@Mixin(BlockEntityRenderDispatcher.class)"));
        assertTrue(mixin.contains("method = \"tryExtractRenderState\""));
        assertTrue(mixin.contains("at = @At(\"HEAD\")"));
        assertTrue(mixin.contains("BuilderFocusVisibility.shouldHide(blockEntity.getBlockState().getBlock())"));
        assertTrue(mixin.contains("callbackInfo.setReturnValue(null)"));
        assertTrue(mixin.contains("FeatureSwitches.BRIGHT_CHEST.getBooleanValue()"));
        assertTrue(mixin.contains("state instanceof ChestRenderState chest"));
        assertTrue(mixin.contains("chest.lightCoords = LightCoordsUtil.FULL_BRIGHT"));
        assertTrue(mixinConfig.contains("rendering.BlockEntityVisualStateMixin"));
        assertTrue(plugin.contains("BlockEntityVisualStateMixin"));
        assertTrue(plugin.contains("FeatureDefinition.BUILDER_FOCUS_BLOCKS"));
        assertTrue(plugin.contains("FeatureDefinition.BRIGHT_CHEST"));

        assertFalse(mixin.contains("customSprite"));
        assertFalse(mixin.contains("SpriteId"));
        assertFalse(mixin.contains("CHEST_MAPPER"));
        assertFalse(mixin.contains("entity/chest/"));
        assertFalse(mixin.contains("SignBlockEntity"));
        assertFalse(mixin.contains("BannerBlockEntity"));
        assertFalse(mixin.contains("ShulkerBoxBlockEntity"));
        assertFalse(mixin.contains("BeaconBlockEntity"));
        assertFalse(mixin.contains("saveWith"));
        assertFalse(mixin.contains("getUpdateTag"));
        assertFalse(mixin.contains("getComponents"));
    }

    @Test
    void brightChestOwnsOnlyItsDedicatedChestTextures() {
        assertTrue(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/textures/entity/chest/normal.png")));
        assertTrue(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/textures/entity/chest/normal_left.png")));
        assertTrue(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/textures/entity/chest/normal_right.png")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/textures/block/visual/bright_white_concrete.png")));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/resources/assets/chisetweaks/models/block/visual/bright_concrete.json")));
    }

    @Test
    void hidePrecedenceCoversEveryRetainedIndependentOverlayPath() throws IOException {
        String worksiteScanner = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java");
        String worksiteRenderer = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java");
        String lava = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");

        assertTrue(worksiteScanner.contains("BuilderFocusVisibility.shouldHide(state.getBlock())"));
        assertTrue(worksiteRenderer.contains("BuilderFocusVisibility.shouldHide(block)"));
        assertTrue(lava.contains("BuilderFocusVisibility.shouldHide(Blocks.LAVA)"));
        assertFalse(Files.exists(ROOT.resolve(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java")));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
