package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseRuleMode;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.core.policy.BuilderEntityVisibilityPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Runtime snapshots for Chise-owned block/entity visibility rules. */
public final class BuilderFocusVisibility {
    private static volatile BlockConfigFingerprint blockFingerprint = BlockConfigFingerprint.empty();
    private static volatile EntityRules entityRules = EntityRules.none();

    private BuilderFocusVisibility() {}

    public static void applyConfig() {
        blockFingerprint = currentBlockFingerprint();
        buildEntityLists();
    }

    public static void buildLists() {
        BlockConfigFingerprint next = currentBlockFingerprint();
        if (next.equals(blockFingerprint)) return;
        blockFingerprint = next;

        if (BuilderFocusConfig.REFRESH_RENDERER.getBooleanValue()) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) client.levelRenderer.allChanged();
        }
    }

    public static void buildEntityLists() {
        ChiseRuleMode mode = BuilderFocusConfig.ENTITY_RULE_MODE.getValue();
        entityRules = new EntityRules(
                mode,
                resolveEntityTypes(BuilderFocusConfig.ENTITY_BLACKLIST.getStrings()),
                resolveEntityTypes(BuilderFocusConfig.ENTITY_WHITELIST.getStrings()));
    }

    public static boolean shouldHide(Block block) {
        if (block == null) return false;
        BlockConfigFingerprint rules = blockFingerprint;
        if (!rules.enabled() || rules.mode() == ChiseRuleMode.NONE) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        String value = id == null ? "" : id.toString();
        return switch (rules.mode()) {
            case BLACKLIST -> rules.blacklist().contains(value);
            case WHITELIST -> !rules.whitelist().contains(value);
            case NONE -> false;
        };
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
                BuilderFocusConfig.ENTITY_RULE_MODE.setValue(ChiseRuleMode.BLACKLIST);
                BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(List.of(
                        "minecraft:item", "minecraft:experience_orb", "minecraft:area_effect_cloud"));
                BuilderFocusConfig.ENTITY_WHITELIST.setStrings(List.of());
            }
            case "technical_trace" -> {
                BuilderFocusConfig.ENTITY_RULE_MODE.setValue(ChiseRuleMode.WHITELIST);
                BuilderFocusConfig.ENTITY_WHITELIST.setStrings(List.of(
                        "minecraft:armor_stand", "minecraft:item_frame", "minecraft:glow_item_frame",
                        "minecraft:minecart", "minecraft:hopper_minecart", "minecraft:chest_minecart"));
                BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(List.of());
            }
            case "photo" -> {
                BuilderFocusConfig.ENTITY_RULE_MODE.setValue(ChiseRuleMode.BLACKLIST);
                BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(List.of(
                        "minecraft:item", "minecraft:experience_orb", "minecraft:area_effect_cloud",
                        "minecraft:marker"));
                BuilderFocusConfig.ENTITY_WHITELIST.setStrings(List.of());
            }
            case "clear" -> {
                BuilderFocusConfig.ENTITY_RULE_MODE.setValue(ChiseRuleMode.NONE);
                BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(List.of());
                BuilderFocusConfig.ENTITY_WHITELIST.setStrings(List.of());
            }
            default -> { return false; }
        }
        buildEntityLists();
        FeatureConfig.saveToFile();
        return true;
    }

    private static BuilderEntityVisibilityPolicy.Mode toPolicyMode(ChiseRuleMode mode) {
        return switch (mode) {
            case BLACKLIST -> BuilderEntityVisibilityPolicy.Mode.BLACKLIST;
            case WHITELIST -> BuilderEntityVisibilityPolicy.Mode.WHITELIST;
            case NONE -> BuilderEntityVisibilityPolicy.Mode.NONE;
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
                BuilderFocusConfig.BLOCK_RULE_MODE.getValue(),
                Set.copyOf(BuilderFocusConfig.BLOCK_BLACKLIST.getStrings()),
                Set.copyOf(BuilderFocusConfig.BLOCK_WHITELIST.getStrings()));
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
            ChiseRuleMode mode,
            Set<String> blacklist,
            Set<String> whitelist) {
        private BlockConfigFingerprint {
            Objects.requireNonNull(mode, "mode");
            blacklist = Set.copyOf(blacklist);
            whitelist = Set.copyOf(whitelist);
        }

        static BlockConfigFingerprint empty() {
            return new BlockConfigFingerprint(false, ChiseRuleMode.NONE, Set.of(), Set.of());
        }
    }

    private record EntityRules(
            ChiseRuleMode mode,
            Set<EntityType<?>> blacklist,
            Set<EntityType<?>> whitelist) {
        private EntityRules {
            Objects.requireNonNull(mode, "mode");
            blacklist = Set.copyOf(blacklist);
            whitelist = Set.copyOf(whitelist);
        }

        static EntityRules none() {
            return new EntityRules(ChiseRuleMode.NONE, Set.of(), Set.of());
        }

        boolean hides(EntityType<?> type) {
            return switch (mode) {
                case BLACKLIST -> blacklist.contains(type);
                case WHITELIST -> !whitelist.contains(type);
                case NONE -> false;
            };
        }
    }
}
