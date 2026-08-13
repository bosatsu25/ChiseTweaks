package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

/** Preloaded Chise-owned overlay model keys shared by vanilla and modded ore targets. */
final class OreHighlightOverlayCatalog {
    private static final Map<OreHighlightStyle, OverlayModels> MODELS = build();

    private OreHighlightOverlayCatalog() {}

    static Iterable<OverlayModels> values() {
        return MODELS.values();
    }

    static OverlayModels forStyle(OreHighlightStyle style) {
        OreHighlightStyle effective = style == OreHighlightStyle.GENERIC
                ? OreHighlightStyle.QUARTZ
                : style;
        return effective == null ? null : MODELS.get(effective);
    }

    private static Map<OreHighlightStyle, OverlayModels> build() {
        EnumMap<OreHighlightStyle, OverlayModels> result = new EnumMap<>(OreHighlightStyle.class);
        for (OreHighlightStyle style : OreHighlightStyle.values()) {
            if (style == OreHighlightStyle.GENERIC) continue;
            result.put(style, overlays(style.key()));
        }
        return Map.copyOf(result);
    }

    private static OverlayModels overlays(String highlightKey) {
        Identifier staticModel = Identifier.fromNamespaceAndPath(
                ChiseTweaksMetadata.MOD_ID,
                "block/visual/overlay/" + highlightKey + "_static");
        Identifier animatedModel = Identifier.fromNamespaceAndPath(
                ChiseTweaksMetadata.MOD_ID,
                "block/visual/overlay/" + highlightKey + "_animated");
        ExtraModelKey<BlockStateModel> staticKey = ExtraModelKey.create(staticModel::toString);
        ExtraModelKey<BlockStateModel> animatedKey = ExtraModelKey.create(animatedModel::toString);
        return new OverlayModels(staticModel, animatedModel, staticKey, animatedKey);
    }

    record OverlayModels(
            Identifier staticModel,
            Identifier animatedModel,
            ExtraModelKey<BlockStateModel> staticKey,
            ExtraModelKey<BlockStateModel> animatedKey) {}
}
