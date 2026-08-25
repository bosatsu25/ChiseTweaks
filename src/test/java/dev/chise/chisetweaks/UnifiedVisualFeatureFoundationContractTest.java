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
    void brightFeaturesDoNotDependOnBuiltInResourcePackSelection() throws Exception {
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/feature/resource")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/resourcepacks/chise_chest_visibility")));
        assertFalse(Files.exists(ROOT.resolve("src/main/resources/resourcepacks/chise_white_concrete_visibility")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/config/ChestVisibilitySetting.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/config/WhiteConcreteVisibilitySetting.java")));
        assertFalse(Files.exists(ROOT.resolve("src/main/java/dev/chise/chisetweaks/config/LocalFeatureSwitches.java")));
    }

    @Test
    void brightConcretePreservesNormalLightingWhileOtherModelHighlightsStayFullbright() throws Exception {
        String model = source("src/main/java/dev/chise/chisetweaks/feature/rendering/model/FullbrightOverlayModel.java");
        int replacementStart = model.indexOf("private void emitReplacementOrBase");
        int extraStart = model.indexOf("private void emitExtraModel");
        String replacement = model.substring(replacementStart, extraStart);
        assertTrue(replacement.contains("replacement.emitQuads"));
        assertFalse(replacement.contains("FullbrightOverlayEmission.emit"));
        assertTrue(model.substring(extraStart).contains("FullbrightOverlayEmission.emit"));
    }

    @Test
    void brightChestAndConcreteUseExistingRenderingBoundaries() throws Exception {
        String blockEntity = source("src/main/java/dev/chise/chisetweaks/mixin/rendering/BlockEntityVisualStateMixin.java");
        String plugin = source("src/main/java/dev/chise/chisetweaks/feature/rendering/model/ChiseVisualModelPlugin.java");
        assertTrue(blockEntity.contains("FeatureSwitches.BRIGHT_CHEST"));
        assertTrue(blockEntity.contains("BuilderFocusVisibility.shouldHide"));
        assertTrue(plugin.contains("FullbrightOverlayModel.brightConcrete"));
        assertTrue(plugin.contains("white_concrete"));
    }

    private static String source(String path) throws Exception {
        return Files.readString(ROOT.resolve(path));
    }
}
