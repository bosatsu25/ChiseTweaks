package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.GlassHighlightTargetPolicy;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure derivation policy for privacy-safe Inspector snapshots.
 *
 * <p>This class owns Block/Entity classification and state formatting so
 * {@link CrosshairInspector} can stay focused on hit lifecycle and cache invalidation.</p>
 */
final class CrosshairSnapshotPolicy {
    private static final int MAX_STATE_PROPERTIES = 32;
    private static final int MAX_TOKEN_LENGTH = 64;

    private CrosshairSnapshotPolicy() {}

    static CrosshairInspector.Snapshot blockSnapshot(
            BlockState state,
            long enabledFeatures,
            int visualTargetMask,
            boolean worldOverlay,
            BlockState predictedPlacement,
            BlockState actualPlacement,
            int placementResult,
            net.minecraft.core.Direction clickedFace,
            boolean upperClick) {
        if (state == null) return CrosshairInspector.Snapshot.noTarget();
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String targetId = id == null ? "" : safeIdentifier(id.toString());
        if (targetId.isEmpty()) return CrosshairInspector.Snapshot.noTarget();

        BuilderFocusVisibility.FilterDecision filter = BuilderFocusVisibility.inspect(state.getBlock());
        OreHighlightResolver.Resolved ore = featureEnabled(enabledFeatures, FeatureDefinition.MATERIAL_HIGHLIGHTS)
                ? OreHighlightResolver.resolve(state)
                : null;
        boolean sourceLava = state.getFluidState().isSource()
                && (state.getFluidState().getType() == Fluids.LAVA
                || state.getFluidState().getType() == Fluids.FLOWING_LAVA);

        return new CrosshairInspector.Snapshot(
                HitResult.Type.BLOCK,
                targetId,
                stateProperties(state),
                filter,
                responsibleFeatures(
                        targetId,
                        BlockInspectionPolicy.categories(targetId),
                        ore == null ? null : ore.target(),
                        ore != null,
                        sourceLava,
                        visualTargetMask,
                        worldOverlay,
                        enabledFeatures),
                predictedPlacement,
                actualPlacement,
                placementResult,
                clickedFace,
                upperClick);
    }

    static CrosshairInspector.Snapshot entitySnapshot(Entity entity) {
        Identifier id = entity == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String targetId = id == null ? "" : safeIdentifier(id.toString());
        if (targetId.isEmpty()) return CrosshairInspector.Snapshot.noTarget();
        return new CrosshairInspector.Snapshot(
                HitResult.Type.ENTITY,
                targetId,
                List.of(),
                BuilderFocusVisibility.inspect(entity),
                List.of(),
                null,
                null,
                PlacementInspector.NONE,
                null,
                false);
    }

    static List<FeatureDefinition> responsibleFeatures(
            String blockId,
            Set<BlockInspectionCategory> categories,
            Target oreTarget,
            boolean oreResolved,
            boolean sourceLava,
            int visualTargetMask,
            boolean worldOverlay,
            long enabledFeatures) {
        String id = safeIdentifier(blockId);
        if (id.isEmpty()) return List.of();
        Set<BlockInspectionCategory> safeCategories = categories == null ? Set.of() : categories;
        ArrayList<FeatureDefinition> result = new ArrayList<>();

        addWorksiteImpact(result, FeatureDefinition.FINE_THREAD_TRACE,
                BlockInspectionCategory.TECHNICAL_TRACE, id, safeCategories,
                visualTargetMask, worldOverlay, enabledFeatures);
        addWorksiteImpact(result, FeatureDefinition.HIDDEN_SURFACE_TRACE,
                BlockInspectionCategory.HIDDEN_SURFACE, id, safeCategories,
                visualTargetMask, worldOverlay, enabledFeatures);
        if (featureEnabled(enabledFeatures, FeatureDefinition.MATERIAL_HIGHLIGHTS)
                && oreResolved
                && (oreTarget == null || VisualTargetSelectionPolicy.isEnabled(visualTargetMask, oreTarget))) {
            result.add(FeatureDefinition.MATERIAL_HIGHLIGHTS);
        }
        if (featureEnabled(enabledFeatures, FeatureDefinition.GLASS_INSPECTION)
                && GlassHighlightTargetPolicy.classifyBlockId(id) != GlassHighlightTargetPolicy.Shape.NONE) {
            result.add(FeatureDefinition.GLASS_INSPECTION);
        }
        if (featureEnabled(enabledFeatures, FeatureDefinition.KELP_HIGHLIGHT)
                && ("minecraft:kelp".equals(id) || "minecraft:kelp_plant".equals(id))) {
            result.add(FeatureDefinition.KELP_HIGHLIGHT);
        }
        addWorksiteImpact(result, FeatureDefinition.NETHER_PALETTE,
                BlockInspectionCategory.NETHER_PALETTE, id, safeCategories,
                visualTargetMask, worldOverlay, enabledFeatures);
        if (featureEnabled(enabledFeatures, FeatureDefinition.LAVA_HIGHLIGHT) && sourceLava) {
            result.add(FeatureDefinition.LAVA_HIGHLIGHT);
        }
        return List.copyOf(result);
    }

    static long enabledFeatureMask(FeatureDefinition... features) {
        long mask = 0L;
        if (features == null) return mask;
        for (FeatureDefinition feature : features) {
            if (feature != null) mask |= 1L << feature.ordinal();
        }
        return mask;
    }

    static long currentEnabledFeatureMask() {
        long mask = 0L;
        for (var setting : FeatureSwitches.VALUES) {
            if (setting.getBooleanValue()) mask |= 1L << setting.definition().ordinal();
        }
        return mask;
    }

    static List<String> stateProperties(BlockState state) {
        LinkedHashMap<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            addProperty(state, property, properties);
        }
        return formatStateProperties(properties);
    }

    static String semanticPropertyGroup(String property) {
        return switch (property == null ? "" : property) {
            case "facing", "axis" -> "orientation";
            case "half", "type", "shape", "face" -> "shape";
            case "north", "south", "east", "west", "up", "down", "in_wall" -> "connection";
            case "open", "powered", "lit", "honey_level" -> "interaction";
            case "waterlogged" -> "fluid";
            default -> "other";
        };
    }

    static String humanize(String token) {
        String value = token.replace('_', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    static List<String> formatStateProperties(Map<String, String> rawProperties) {
        if (rawProperties == null || rawProperties.isEmpty()) return List.of();
        ArrayList<String> result = new ArrayList<>();
        rawProperties.entrySet().stream()
                .sorted(Comparator.comparing(entry -> safeToken(entry.getKey())))
                .forEach(entry -> {
                    if (result.size() >= MAX_STATE_PROPERTIES) return;
                    String name = safeToken(entry.getKey());
                    String value = safeToken(entry.getValue());
                    if (!name.isEmpty() && !value.isEmpty()) result.add(name + "=" + value);
                });
        return List.copyOf(result);
    }

    private static void addWorksiteImpact(
            ArrayList<FeatureDefinition> result,
            FeatureDefinition feature,
            BlockInspectionCategory category,
            String blockId,
            Set<BlockInspectionCategory> categories,
            int visualTargetMask,
            boolean worldOverlay,
            long enabledFeatures) {
        if (worldOverlay
                && featureEnabled(enabledFeatures, feature)
                && categories.contains(category)
                && VisualTargetSelectionPolicy.matchesEnabled(visualTargetMask, blockId, category)) {
            result.add(feature);
        }
    }

    private static boolean featureEnabled(long mask, FeatureDefinition feature) {
        return feature != null && (mask & (1L << feature.ordinal())) != 0L;
    }

    private static <T extends Comparable<T>> void addProperty(
            BlockState state,
            Property<T> property,
            Map<String, String> properties) {
        properties.put(property.getName(), property.getName(state.getValue(property)));
    }

    private static String safeIdentifier(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        Identifier parsed = value.length() <= 256 ? Identifier.tryParse(value) : null;
        return parsed == null ? "" : parsed.toString();
    }

    private static String safeToken(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.length() <= MAX_TOKEN_LENGTH && Identifier.isValidPath(value) ? value : "";
    }
}
