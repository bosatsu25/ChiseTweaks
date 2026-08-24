package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.LocalFeatureConfig;
import dev.chise.chisetweaks.config.LocalFeatureSwitches;
import dev.chise.chisetweaks.core.definition.FeatureDefinition;
import dev.chise.chisetweaks.core.vision.BlockInspectionCategory;
import dev.chise.chisetweaks.core.vision.BlockInspectionPolicy;
import dev.chise.chisetweaks.core.vision.GlassHighlightTargetPolicy;
import dev.chise.chisetweaks.core.vision.OreHighlightResolver;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy;
import dev.chise.chisetweaks.core.vision.VisualTargetSelectionPolicy.Target;
import dev.chise.chisetweaks.feature.rendering.BuilderFocusVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Minecraftが保持するcrosshair hitだけから、privacy-safeな読み取り専用snapshotを作る。 */
final class CrosshairInspector {
    private static final int MAX_STATE_PROPERTIES = 32;
    private static final int MAX_TOKEN_LENGTH = 64;
    private static final Snapshot NO_TARGET = new Snapshot(
            HitResult.Type.MISS, "", List.of(), null, List.of(), null, null,
            PlacementComparisonTracker.NONE, null, false);

    private HitResult.Type cachedKind;
    private BlockState cachedBlockState;
    private EntityType<?> cachedEntityType;
    private boolean cachedSelf;
    private long cachedFeatureMask = Long.MIN_VALUE;
    private int cachedVisualTargetMask = Integer.MIN_VALUE;
    private boolean cachedWorldOverlay;
    private boolean cachedInNether;
    private long cachedFilterRevision = Long.MIN_VALUE;
    private long cachedOreRevision = Long.MIN_VALUE;
    private long cachedComparisonRevision = Long.MIN_VALUE;
    private Snapshot snapshot = NO_TARGET;

    Snapshot snapshot() {
        return snapshot;
    }

    void invalidate() {
        cachedKind = null;
    }

    boolean refresh(Minecraft client) {
        try {
            return refreshSafely(client);
        } catch (RuntimeException | LinkageError ignored) {
            boolean changed = snapshot != NO_TARGET;
            cachedKind = HitResult.Type.MISS;
            cachedBlockState = null;
            cachedEntityType = null;
            snapshot = NO_TARGET;
            return changed;
        }
    }

    private boolean refreshSafely(Minecraft client) {
        if (client == null || client.level == null || client.hitResult == null) {
            return updateNoTarget();
        }
        HitResult hit = client.hitResult;
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            BlockState state = client.level.getBlockState(blockHit.getBlockPos());
            return updateBlock(client, blockHit, state, Level.NETHER.equals(client.level.dimension()));
        }
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            return updateEntity(client, entity);
        }
        return updateNoTarget();
    }

    private boolean updateNoTarget() {
        if (cachedKind == HitResult.Type.MISS) return false;
        cachedKind = HitResult.Type.MISS;
        cachedBlockState = null;
        cachedEntityType = null;
        snapshot = NO_TARGET;
        return true;
    }

    private boolean updateBlock(
            Minecraft client,
            BlockHitResult hit,
            BlockState state,
            boolean inNether) {
        LocalFeatureConfig local = LocalFeatureConfig.getInstance();
        long featureMask = enabledFeatureMask();
        long filterRevision = BuilderFocusVisibility.revision();
        long oreRevision = OreHighlightResolver.revision();
        PlacementComparisonTracker comparison = PlacementComparisonTracker.activeAt(
                client.level.dimension(), hit.getBlockPos());
        long comparisonRevision = comparison == null ? Long.MIN_VALUE : comparison.revision;
        ItemStack stack = client.player == null ? null : client.player.getMainHandItem();
        boolean placementAvailable = stack != null
                && stack.getItem() instanceof BlockItem item
                && supportsPlacementPreview(item.getBlock());
        Direction clickedFace = placementAvailable ? hit.getDirection() : null;
        boolean upperClick = placementAvailable
                && hit.getLocation().y - hit.getBlockPos().getY() > 0.5D;
        BlockState livePrediction = placementAvailable
                ? predictPlacementState(
                        client.level,
                        client.player,
                        InteractionHand.MAIN_HAND,
                        stack,
                        hit,
                        true)
                : null;
        BlockState predictedPlacement = comparison == null ? livePrediction : comparison.predictedState;
        BlockState actualPlacement = comparison == null ? null : comparison.actualState;
        int placementResult = comparison == null
                ? PlacementComparisonTracker.NONE
                : PlacementComparisonTracker.compare(predictedPlacement, actualPlacement);
        if (cachedKind == HitResult.Type.BLOCK
                && cachedBlockState == state
                && cachedFeatureMask == featureMask
                && cachedVisualTargetMask == local.visualTargetMask
                && cachedWorldOverlay == local.worksiteVisibilityWorldOverlay
                && cachedInNether == inNether
                && cachedFilterRevision == filterRevision
                && cachedOreRevision == oreRevision
                && cachedComparisonRevision == comparisonRevision
                && snapshot.predictedPlacement() == predictedPlacement
                && snapshot.actualPlacement() == actualPlacement
                && snapshot.placementResult() == placementResult
                && snapshot.clickedFace() == clickedFace
                && snapshot.upperClick() == upperClick) {
            return false;
        }

        cachedKind = HitResult.Type.BLOCK;
        cachedBlockState = state;
        cachedEntityType = null;
        cachedFeatureMask = featureMask;
        cachedVisualTargetMask = local.visualTargetMask;
        cachedWorldOverlay = local.worksiteVisibilityWorldOverlay;
        cachedInNether = inNether;
        cachedFilterRevision = filterRevision;
        cachedOreRevision = oreRevision;
        cachedComparisonRevision = comparisonRevision;
        snapshot = blockSnapshot(
                state,
                featureMask,
                local.visualTargetMask,
                local.worksiteVisibilityWorldOverlay,
                inNether,
                predictedPlacement,
                actualPlacement,
                placementResult,
                clickedFace,
                upperClick);
        return true;
    }

    private boolean updateEntity(Minecraft client, Entity entity) {
        EntityType<?> type = entity == null ? null : entity.getType();
        boolean self = entity != null && entity == client.player;
        long filterRevision = BuilderFocusVisibility.revision();
        if (cachedKind == HitResult.Type.ENTITY
                && cachedEntityType == type
                && cachedSelf == self
                && cachedFilterRevision == filterRevision) {
            return false;
        }

        cachedKind = HitResult.Type.ENTITY;
        cachedBlockState = null;
        cachedEntityType = type;
        cachedSelf = self;
        cachedFilterRevision = filterRevision;
        Identifier id = type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String targetId = id == null ? "" : safeIdentifier(id.toString());
        snapshot = targetId.isEmpty()
                ? NO_TARGET
                : new Snapshot(
                        HitResult.Type.ENTITY,
                        targetId,
                        List.of(),
                        BuilderFocusVisibility.inspect(entity),
                        List.of(),
                        null,
                        null,
                        PlacementComparisonTracker.NONE,
                        null,
                        false);
        return true;
    }

    private static Snapshot blockSnapshot(
            BlockState state,
            long enabledFeatures,
            int visualTargetMask,
            boolean worldOverlay,
            boolean inNether,
            BlockState predictedPlacement,
            BlockState actualPlacement,
            int placementResult,
            Direction clickedFace,
            boolean upperClick) {
        if (state == null) return NO_TARGET;
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String targetId = id == null ? "" : safeIdentifier(id.toString());
        if (targetId.isEmpty()) return NO_TARGET;
        BuilderFocusVisibility.FilterDecision filter = BuilderFocusVisibility.inspect(state.getBlock());
        OreHighlightResolver.Resolved ore = featureEnabled(enabledFeatures, FeatureDefinition.MATERIAL_HIGHLIGHTS)
                ? OreHighlightResolver.resolve(state)
                : null;
        boolean sourceLava = state.getFluidState().isSource()
                && (state.getFluidState().getType() == Fluids.LAVA
                || state.getFluidState().getType() == Fluids.FLOWING_LAVA);
        return new Snapshot(
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
                        inNether && "minecraft:ancient_debris".equals(targetId),
                        visualTargetMask,
                        worldOverlay,
                        enabledFeatures),
                predictedPlacement,
                actualPlacement,
                placementResult,
                clickedFace,
                upperClick);
    }

    static BlockState predictPlacementState(
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack stack,
            BlockHitResult hit,
            boolean enabled) {
        if (!enabled || !(stack.getItem() instanceof BlockItem item)
                || !supportsPlacementPreview(item.getBlock())) return null;
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, stack, hit);
        if (!context.canPlace()) return null;
        BlockState state = item.getBlock().getStateForPlacement(context);
        if (state == null
                || !level.isUnobstructed(
                        state,
                        context.getClickedPos(),
                        CollisionContext.placementContext(player))) return null;
        return stack.getOrDefault(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY).apply(state);
    }

    static boolean supportsPlacementPreview(Block block) {
        if (block instanceof TrapDoorBlock || block instanceof SlabBlock) return true;
        BlockState state = block.defaultBlockState();
        if (!state.hasProperty(BlockStateProperties.AXIS)) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return state.is(BlockTags.LOGS)
                || id != null
                && "minecraft".equals(id.getNamespace())
                && id.getPath().endsWith("_froglight");
    }

    static List<FeatureDefinition> responsibleFeatures(
            String blockId,
            Set<BlockInspectionCategory> categories,
            Target oreTarget,
            boolean oreResolved,
            boolean sourceLava,
            boolean ancientAnalyzerTarget,
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
        if (featureEnabled(enabledFeatures, FeatureDefinition.ANCIENT_DEBRIS_ANALYZER)
                && ancientAnalyzerTarget
                && "minecraft:ancient_debris".equals(id)) {
            result.add(FeatureDefinition.ANCIENT_DEBRIS_ANALYZER);
        }
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

    static long enabledFeatureMask(FeatureDefinition... features) {
        long mask = 0L;
        if (features == null) return mask;
        for (FeatureDefinition feature : features) {
            if (feature != null) mask |= 1L << feature.ordinal();
        }
        return mask;
    }

    private static long enabledFeatureMask() {
        long mask = 0L;
        for (var setting : FeatureSwitches.VALUES) {
            if (setting.getBooleanValue()) mask |= 1L << setting.definition().ordinal();
        }
        for (var setting : LocalFeatureSwitches.VALUES) {
            if (setting.getBooleanValue()) mask |= 1L << setting.definition().ordinal();
        }
        return mask;
    }

    private static boolean featureEnabled(long mask, FeatureDefinition feature) {
        return feature != null && (mask & (1L << feature.ordinal())) != 0L;
    }

    static List<String> stateProperties(BlockState state) {
        LinkedHashMap<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            addProperty(state, property, properties);
        }
        return formatStateProperties(properties);
    }

    static List<String> placementStateProperties(BlockState state) {
        LinkedHashMap<String, String> properties = new LinkedHashMap<>();
        if (state.getBlock() instanceof TrapDoorBlock) {
            addPropertyIfPresent(state, BlockStateProperties.HORIZONTAL_FACING, properties);
            addPropertyIfPresent(state, BlockStateProperties.HALF, properties);
            addPropertyIfPresent(state, BlockStateProperties.OPEN, properties);
            addPropertyIfPresent(state, BlockStateProperties.POWERED, properties);
            addPropertyIfPresent(state, BlockStateProperties.WATERLOGGED, properties);
        } else if (state.getBlock() instanceof SlabBlock) {
            addPropertyIfPresent(state, BlockStateProperties.SLAB_TYPE, properties);
            addPropertyIfPresent(state, BlockStateProperties.WATERLOGGED, properties);
        } else {
            addPropertyIfPresent(state, BlockStateProperties.AXIS, properties);
        }
        return formatStateProperties(properties);
    }

    private static <T extends Comparable<T>> void addPropertyIfPresent(
            BlockState state,
            Property<T> property,
            Map<String, String> properties) {
        if (state.hasProperty(property)) addProperty(state, property, properties);
    }

    private static <T extends Comparable<T>> void addProperty(
            BlockState state,
            Property<T> property,
            Map<String, String> properties) {
        properties.put(property.getName(), property.getName(state.getValue(property)));
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

    record Snapshot(
            HitResult.Type targetKind,
            String targetId,
            List<String> stateProperties,
            BuilderFocusVisibility.FilterDecision filterDecision,
            List<FeatureDefinition> responsibleFeatures,
            BlockState predictedPlacement,
            BlockState actualPlacement,
            int placementResult,
            Direction clickedFace,
            boolean upperClick) {
        Snapshot {
            Objects.requireNonNull(targetKind);
            targetId = targetId == null ? "" : targetId;
            stateProperties = List.copyOf(Objects.requireNonNull(stateProperties));
            responsibleFeatures = List.copyOf(Objects.requireNonNull(responsibleFeatures));
            if (targetKind == HitResult.Type.MISS && (!targetId.isEmpty() || filterDecision != null)) {
                throw new IllegalArgumentException();
            }
            if (targetKind != HitResult.Type.MISS && (targetId.isEmpty() || filterDecision == null)) {
                throw new IllegalArgumentException();
            }
            if (targetKind != HitResult.Type.BLOCK && !stateProperties.isEmpty()) {
                throw new IllegalArgumentException();
            }
            if (placementResult != PlacementComparisonTracker.NONE && predictedPlacement == null) {
                throw new IllegalArgumentException();
            }
            if (actualPlacement != null && placementResult == PlacementComparisonTracker.NONE) {
                throw new IllegalArgumentException();
            }
        }

        static Snapshot noTarget() {
            return NO_TARGET;
        }

    }
}
