package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedVisualFeatureFoundationContractTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    @Test
    void allThirteenVisualTogglesUseTheCanonicalFeatureRegistry() throws Exception {
        String definitions = source("src/main/java/dev/chise/chisetweaks/core/definition/FeatureDefinition.java");
        String switches = source("src/main/java/dev/chise/chisetweaks/config/FeatureSwitches.java");
        assertTrue(definitions.contains("BRIGHT_CHEST"));
        assertTrue(definitions.contains("BRIGHT_CONCRETE"));
        assertTrue(switches.contains("public static final List<FeatureSwitch> VALUES"));
        assertTrue(switches.contains("BRIGHT_CHEST"));
        assertTrue(switches.contains("BRIGHT_CONCRETE"));
        assertFalse(switches.contains("ChiseTexturePackController"));
    }

    @Test
    void brightFeaturesDoNotDependOnBuiltInResourcePackSelectionOrCustomAssets() throws Exception {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/feature/resource")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/resourcepacks/chise_chest_visibility")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/resourcepacks/chise_white_concrete_visibility")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/config/ChestVisibilitySetting.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/config/WhiteConcreteVisibilitySetting.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitches.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/chisetweaks/textures/entity/chest/normal.png")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/chisetweaks/textures/entity/chest/normal_left.png")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/chisetweaks/textures/entity/chest/normal_right.png")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/chisetweaks/textures/block/visual/bright_white_concrete.png")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/assets/chisetweaks/models/block/visual/bright_concrete.json")));
    }

    @Test
    void brightConcreteUsesVanillaModelWithInPlaceFullbrightLighting() throws Exception {
        String model = source("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java");
        int brightStart = model.indexOf("private void emitBrightConcrete");
        int extraStart = model.indexOf("private void emitExtraModel");
        assertTrue(brightStart >= 0 && extraStart > brightStart);
        String bright = model.substring(brightStart, extraStart);
        assertTrue(bright.contains("emitter.pushTransform"));
        assertTrue(bright.contains("FullbrightOverlayLighting.apply(quad)"));
        assertTrue(bright.contains("super.emitQuads(emitter, level, pos, state, random, cullTest)"));
        assertTrue(bright.contains("emitter.popTransform()"));
        assertFalse(bright.contains("FullbrightOverlayEmission.emit"));
        assertFalse(bright.contains("overlayModel("));
        assertTrue(model.substring(extraStart).contains("FullbrightOverlayEmission.emit"));
    }

    @Test
    void brightChestAndConcreteUseExistingRenderingBoundaries() throws Exception {
        String blockEntity = source("src/main/java/dev/chise/chisetweaks/mixin/rendering/BlockEntityVisualStateMixin.java");
        String plugin = source("src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        assertTrue(blockEntity.contains("FeatureSwitches.BRIGHT_CHEST.getBooleanValue()"));
        assertTrue(blockEntity.contains("BuilderFocusVisibility.shouldHide"));
        assertTrue(blockEntity.contains("chest.lightCoords = LightCoordsUtil.FULL_BRIGHT"));
        assertFalse(blockEntity.contains("customSprite"));
        assertTrue(plugin.contains("FullbrightOverlayModel.brightConcrete(model)"));
        assertTrue(plugin.contains("white_concrete"));
        assertFalse(plugin.contains("BRIGHT_CONCRETE_MODEL"));
    }

    private static String source(String path) throws Exception {
        return Files.readString(ROOT.resolve(path));
    }
}
