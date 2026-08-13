package dev.chise.chisetweaks.feature.rendering.model;

import dev.chise.chisetweaks.ChiseTweaksMetadata;
import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Preloaded Chise-owned overlay models shared by vanilla and modded ore targets. */
final class OreHighlightOverlayCatalog {
    private static final Map<OreHighlightStyle, OverlayModels> MODELS = build();
    private static final Map<OreHighlightStyle, BlockStateModel> STATIC_BAKED = new ConcurrentHashMap<>();
    private static final Map<OreHighlightStyle, BlockStateModel> ANIMATED_BAKED = new ConcurrentHashMap<>();
    private static final Set<OreHighlightStyle> STATIC_RESOLVED = ConcurrentHashMap.newKeySet();
    private static final Set<OreHighlightStyle> ANIMATED_RESOLVED = ConcurrentHashMap.newKeySet();
    private static final Object LOOKUP_LOCK = new Object();

    private OreHighlightOverlayCatalog() {}

    static Iterable<OverlayModels> values() {
        return MODELS.values();
    }

    static @Nullable OverlayModels forStyle(OreHighlightStyle style) {
        OreHighlightStyle effective = effective(style);
        return effective == null ? null : MODELS.get(effective);
    }

    static @Nullable BlockStateModel baked(OreHighlightStyle style, boolean animated) {
        OreHighlightStyle effective = effective(style);
        if (effective == null) return null;
        Map<OreHighlightStyle, BlockStateModel> baked = animated ? ANIMATED_BAKED : STATIC_BAKED;
        Set<OreHighlightStyle> resolved = animated ? ANIMATED_RESOLVED : STATIC_RESOLVED;
        BlockStateModel cached = baked.get(effective);
        if (cached != null) return cached;
        if (resolved.contains(effective)) return null;

        synchronized (LOOKUP_LOCK) {
            cached = baked.get(effective);
            if (cached != null) return cached;
            if (resolved.contains(effective)) return null;
            OverlayModels models = MODELS.get(effective);
            if (models == null) {
                resolved.add(effective);
                return null;
            }
            BlockStateModel loaded = Minecraft.getInstance().getModelManager().getModel(
                    animated ? models.animatedKey() : models.staticKey());
            if (loaded != null) baked.put(effective, loaded);
            resolved.add(effective);
            return loaded;
        }
    }

    static void markUnavailable(OreHighlightStyle style, boolean animated) {
        OreHighlightStyle effective = effective(style);
        if (effective == null) return;
        (animated ? ANIMATED_RESOLVED : STATIC_RESOLVED).add(effective);
    }

    static void clearBakedCache() {
        STATIC_BAKED.clear();
        ANIMATED_BAKED.clear();
        STATIC_RESOLVED.clear();
        ANIMATED_RESOLVED.clear();
    }

    private static @Nullable OreHighlightStyle effective(OreHighlightStyle style) {
        return style == OreHighlightStyle.GENERIC ? OreHighlightStyle.QUARTZ : style;
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
