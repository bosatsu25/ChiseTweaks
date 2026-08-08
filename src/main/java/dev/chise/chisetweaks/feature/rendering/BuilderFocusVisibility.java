package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitch;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.core.policy.BuilderEntityVisibilityPolicy;
import fi.dy.masa.malilib.util.restrictions.BlockRestriction;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Runtime snapshot for Chise Builder Focus visibility rules. */
public final class BuilderFocusVisibility {
    public static final BlockRestriction BLOCKS_LIST = new BlockRestriction();
    private static volatile BlockConfigFingerprint blockFingerprint = BlockConfigFingerprint.empty();
    private static volatile EntityRules entityRules = EntityRules.none();

    private BuilderFocusVisibility() {}

    public static void applyConfig() {
        blockFingerprint = currentBlockFingerprint();
    }

    public static void buildLists() {
        BlockConfigFingerprint next = currentBlockFingerprint();
        if (next.equals(blockFingerprint)) return;

        BLOCKS_LIST.setListType(next.mode());
        BLOCKS_LIST.setListContents(next.blacklist(), next.whitelist());
        blockFingerprint = next;

        if (BuilderFocusConfig.REFRESH_RENDERER.getBooleanValue()) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) client.levelRenderer.allChanged();
        }
    }

    public static void buildEntityLists() {
        UsageRestriction.ListType mode = (UsageRestriction.ListType)
                BuilderFocusConfig.ENTITY_RULE_MODE.getOptionListValue();
        entityRules = new EntityRules(
                mode,
                resolveEntityTypes(BuilderFocusConfig.ENTITY_BLACKLIST.getStrings()),
                resolveEntityTypes(BuilderFocusConfig.ENTITY_WHITELIST.getStrings()));
    }

    public static boolean shouldHide(EntityType<?> type) {
        return type != null && entityRules.hides(type);
    }

    public static boolean shouldHide(Entity entity) {
        if (entity == null) return false;
        Minecraft client = Minecraft.getInstance();
        boolean localPlayer = client.player != null && entity == client.player;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        BuilderEntityVisibilityPolicy.Decision decision = BuilderEntityVisibilityPolicy.evaluate(
                new BuilderEntityVisibilityPolicy.Input(
                        FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue(),
                        localPlayer,
                        id == null ? "" : id.toString(),
                        toPolicyMode(entityRules.mode()),
                        ids(entityRules.blacklist()),
                        ids(entityRules.whitelist())));
        return decision == BuilderEntityVisibilityPolicy.Decision.HIDE;
    }

    public static boolean applyPreset(String presetId) {
        String preset = presetId == null ? "" : presetId.trim().toLowerCase(java.util.Locale.ROOT);
        switch (preset) {
            case "build_review" -> {
                BuilderFocusConfig.ENTITY_RULE_MODE.setValueFromString("blacklist");
                BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(List.of(
                        "minecraft:item", "minecraft:experience_orb", "minecraft:area_effect_cloud"));
                BuilderFocusConfig.ENTITY_WHITELIST.setStrings(List.of());
            }
            case "technical_trace" -> {
                BuilderFocusConfig.ENTITY_RULE_MODE.setValueFromString("whitelist");
                BuilderFocusConfig.ENTITY_WHITELIST.setStrings(List.of(
                        "minecraft:armor_stand", "minecraft:item_frame", "minecraft:glow_item_frame",
                        "minecraft:minecart", "minecraft:hopper_minecart", "minecraft:chest_minecart"));
                BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(List.of());
            }
            case "photo" -> {
                BuilderFocusConfig.ENTITY_RULE_MODE.setValueFromString("blacklist");
                BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(List.of(
                        "minecraft:item", "minecraft:experience_orb", "minecraft:area_effect_cloud",
                        "minecraft:marker"));
                BuilderFocusConfig.ENTITY_WHITELIST.setStrings(List.of());
            }
            case "clear" -> {
                BuilderFocusConfig.ENTITY_RULE_MODE.setValueFromString("none");
                BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(List.of());
                BuilderFocusConfig.ENTITY_WHITELIST.setStrings(List.of());
            }
            default -> { return false; }
        }
        buildEntityLists();
        FeatureConfig.saveToFile();
        return true;
    }

    private static BuilderEntityVisibilityPolicy.Mode toPolicyMode(UsageRestriction.ListType mode) {
        return switch (mode) {
            case BLACKLIST -> BuilderEntityVisibilityPolicy.Mode.BLACKLIST;
            case WHITELIST -> BuilderEntityVisibilityPolicy.Mode.WHITELIST;
            default -> BuilderEntityVisibilityPolicy.Mode.NONE;
        };
    }

    private static Set<String> ids(Set<EntityType<?>> types) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (EntityType<?> type : types) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id != null) result.add(id.toString());
        }
        return Set.copyOf(result);
    }

    private static BlockConfigFingerprint currentBlockFingerprint() {
        return new BlockConfigFingerprint(
                FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue(),
                (UsageRestriction.ListType) BuilderFocusConfig.BLOCK_RULE_MODE.getOptionListValue(),
                List.copyOf(BuilderFocusConfig.BLOCK_BLACKLIST.getStrings()),
                List.copyOf(BuilderFocusConfig.BLOCK_WHITELIST.getStrings()));
    }

    private static Set<EntityType<?>> resolveEntityTypes(List<String> entries) {
        LinkedHashSet<EntityType<?>> resolved = new LinkedHashSet<>();
        for (String raw : entries) {
            Identifier id = Identifier.tryParse(raw);
            if (id == null) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type != null) resolved.add(type);
        }
        return Set.copyOf(resolved);
    }

    private record BlockConfigFingerprint(
            boolean enabled,
            UsageRestriction.ListType mode,
            List<String> blacklist,
            List<String> whitelist) {
        private BlockConfigFingerprint {
            Objects.requireNonNull(mode, "mode");
            blacklist = List.copyOf(blacklist);
            whitelist = List.copyOf(whitelist);
        }
        static BlockConfigFingerprint empty() {
            return new BlockConfigFingerprint(false, UsageRestriction.ListType.NONE, List.of(), List.of());
        }
    }

    private record EntityRules(
            UsageRestriction.ListType mode,
            Set<EntityType<?>> blacklist,
            Set<EntityType<?>> whitelist) {
        private EntityRules {
            Objects.requireNonNull(mode, "mode");
            blacklist = Set.copyOf(blacklist);
            whitelist = Set.copyOf(whitelist);
        }
        static EntityRules none() {
            return new EntityRules(UsageRestriction.ListType.NONE, Set.of(), Set.of());
        }
        boolean hides(EntityType<?> type) {
            return switch (mode) {
                case BLACKLIST -> blacklist.contains(type);
                case WHITELIST -> !whitelist.contains(type);
                default -> false;
            };
        }
    }
}
