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
    void blockEntitiesAreStoppedAtTheCommonStateExtractionBoundary() throws IOException {
        String mixin = source(
                "src/main/java/dev/chise/chisetweaks/mixin/rendering/BuilderFocusBlockEntityMixin.java");
        String mixinConfig = source("src/main/resources/chisetweaks.features.mixins.json");
        String plugin = source(
                "src/main/java/dev/chise/chisetweaks/mixin/FeatureAvailabilityMixinConfigPlugin.java");

        assertTrue(mixin.contains("@Mixin(BlockEntityRenderDispatcher.class)"));
        assertTrue(mixin.contains("method = \"tryExtractRenderState\""));
        assertTrue(mixin.contains("at = @At(\"HEAD\")"));
        assertTrue(mixin.contains("BuilderFocusVisibility.shouldHide(blockEntity.getBlockState().getBlock())"));
        assertTrue(mixin.contains("result.setReturnValue(null)"));
        assertTrue(mixinConfig.contains("rendering.BuilderFocusBlockEntityMixin"));
        assertTrue(plugin.contains("BuilderFocusBlockEntityMixin"));
        assertTrue(plugin.contains("FeatureDefinition.BUILDER_FOCUS_BLOCKS"));

        assertFalse(mixin.contains("ChestBlockEntity"));
        assertFalse(mixin.contains("SignBlockEntity"));
        assertFalse(mixin.contains("BannerBlockEntity"));
        assertFalse(mixin.contains("ShulkerBoxBlockEntity"));
        assertFalse(mixin.contains("BeaconBlockEntity"));
        assertFalse(mixin.contains("saveWith"));
        assertFalse(mixin.contains("getUpdateTag"));
        assertFalse(mixin.contains("getComponents"));
    }

    @Test
    void hidePrecedenceAlsoCoversIndependentChiseOverlayPaths() throws IOException {
        String worksiteScanner = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteScanner.java");
        String worksiteRenderer = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/worksite/WorksiteOverlayRenderer.java");
        String ancient = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/AncientDebrisAnalyzerFeature.java");
        String lava = source(
                "src/main/java/dev/chise/chisetweaks/feature/rendering/LavaHighlightFeature.java");

        assertTrue(worksiteScanner.contains("BuilderFocusVisibility.shouldHide(state.getBlock())"));
        assertTrue(worksiteRenderer.contains("BuilderFocusVisibility.shouldHide(block)"));
        assertTrue(ancient.contains("BuilderFocusVisibility.shouldHide(Blocks.ANCIENT_DEBRIS)"));
        assertTrue(lava.contains("BuilderFocusVisibility.shouldHide(Blocks.LAVA)"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
