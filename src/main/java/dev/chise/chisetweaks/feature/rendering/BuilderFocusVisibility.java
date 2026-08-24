package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseRuleMode;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * ブロックとエンティティの可視性判定はランタイム用スナップショットとして保持し、描画中の設定再解釈を避ける。
 */
public final class BuilderFocusVisibility {
    public static final String REASON_FILTER_OFF = "filter_off";
    public static final String REASON_NO_RULE = "no_rule";
    public static final String REASON_HIDE_LIST_MATCH = "hide_list_match";
    public static final String REASON_HIDE_LIST_NO_MATCH = "hide_list_no_match";
    public static final String REASON_ALLOW_LIST_MATCH = "allow_list_match";
    public static final String REASON_ALLOW_LIST_NO_MATCH = "allow_list_no_match";
    public static final String REASON_SELF_PROTECTED = "self_protected";
    public static final String REASON_UNREGISTERED = "unregistered";
    private static volatile BlockConfigFingerprint blockFingerprint = BlockConfigFingerprint.empty();
    private static volatile BlockRules blockRules = BlockRules.none();
    private static volatile EntityRules entityRules = EntityRules.none();
    private static volatile long revision;

    private BuilderFocusVisibility() {}

    public static void applyConfig() {
        BlockConfigFingerprint next = currentBlockFingerprint();
        BlockRules nextRules = compileBlockRules(next);
        blockRules = nextRules;
        blockFingerprint = next;
        revision++;
        buildEntityLists();
    }

    public static void buildLists() {
        BlockConfigFingerprint next = currentBlockFingerprint();
        if (next.equals(blockFingerprint)) return;

        // compile完了前にfingerprintを進めると、失敗後の同一設定再試行が抑止されるためcommitは最後に行う。
        BlockRules nextRules = compileBlockRules(next);
        blockRules = nextRules;
        blockFingerprint = next;
        revision++;

        if (BuilderFocusConfig.REFRESH_RENDERER.getBooleanValue()) {
            ChunkRenderInvalidation.request();
        }
    }

    public static void buildEntityLists() {
        ChiseRuleMode mode = BuilderFocusConfig.ENTITY_RULE_MODE.getValue();
        EntityRules nextRules = new EntityRules(
                mode,
                resolveEntityTypes(BuilderFocusConfig.ENTITY_BLACKLIST.getStrings()),
                resolveEntityTypes(BuilderFocusConfig.ENTITY_WHITELIST.getStrings()));
        entityRules = nextRules;
        revision++;
    }

    public static boolean shouldHide(Block block) {
        if (block == null || !FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue()) return false;
        if (BuiltInRegistries.BLOCK.getKey(block) == null) return false;
        return blockRules.hides(block);
    }

    public static FilterDecision inspect(Block block) {
        if (!FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue()) {
            return FilterDecision.visible(REASON_FILTER_OFF);
        }
        Identifier id = block == null ? null : BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) return FilterDecision.visible(REASON_UNREGISTERED);
        return blockRules.inspect(block, id.toString());
    }

    static boolean shouldHideByRule(
            boolean enabled,
            ChiseRuleMode mode,
            boolean blacklistMatch,
            boolean whitelistMatch) {
        if (!enabled || mode == null || mode == ChiseRuleMode.NONE) return false;
        return switch (mode) {
            case BLACKLIST -> blacklistMatch;
            case WHITELIST -> !whitelistMatch;
            case NONE -> false;
        };
    }

    static FilterDecision inspectByRule(
            boolean enabled,
            ChiseRuleMode mode,
            boolean blacklistMatch,
            boolean whitelistMatch,
            String targetId) {
        if (!enabled) return FilterDecision.visible(REASON_FILTER_OFF);
        if (mode == null || mode == ChiseRuleMode.NONE) {
            return FilterDecision.visible(REASON_NO_RULE);
        }
        String matchedRule = targetId == null ? "" : targetId;
        return switch (mode) {
            case BLACKLIST -> blacklistMatch
                    ? FilterDecision.hidden(REASON_HIDE_LIST_MATCH, matchedRule)
                    : FilterDecision.visible(REASON_HIDE_LIST_NO_MATCH);
            case WHITELIST -> whitelistMatch
                    ? FilterDecision.visible(REASON_ALLOW_LIST_MATCH, matchedRule)
                    : FilterDecision.hidden(REASON_ALLOW_LIST_NO_MATCH, "");
            case NONE -> FilterDecision.visible(REASON_NO_RULE);
        };
    }

    public static boolean shouldHide(EntityType<?> type) {
        if (type == null || !FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue()) return false;
        if (BuiltInRegistries.ENTITY_TYPE.getKey(type) == null) return false;
        return entityRules.hides(type);
    }

    public static boolean shouldHide(Entity entity) {
        if (entity == null || !FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue()) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && entity == client.player) return false;
        EntityType<?> type = entity.getType();
        if (BuiltInRegistries.ENTITY_TYPE.getKey(type) == null) return false;
        return entityRules.hides(type);
    }

    public static FilterDecision inspect(Entity entity) {
        if (!FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue()) {
            return FilterDecision.visible(REASON_FILTER_OFF);
        }
        if (entity == null) return FilterDecision.visible(REASON_UNREGISTERED);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && entity == client.player) {
            return FilterDecision.visible(REASON_SELF_PROTECTED);
        }
        EntityType<?> type = entity.getType();
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return FilterDecision.visible(REASON_UNREGISTERED);
        return entityRules.inspect(type, id.toString());
    }

    public static long revision() {
        return revision;
    }

    public static boolean applyPreset(String presetId) {
        String preset = presetId == null ? "" : presetId.trim().toLowerCase(Locale.ROOT);
        ChiseRuleMode previousMode = BuilderFocusConfig.ENTITY_RULE_MODE.getValue();
        List<String> previousBlacklist = BuilderFocusConfig.ENTITY_BLACKLIST.getStrings();
        List<String> previousWhitelist = BuilderFocusConfig.ENTITY_WHITELIST.getStrings();

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
        if (FeatureConfig.saveToFile()) return true;

        BuilderFocusConfig.ENTITY_RULE_MODE.setValue(previousMode);
        BuilderFocusConfig.ENTITY_BLACKLIST.setStrings(previousBlacklist);
        BuilderFocusConfig.ENTITY_WHITELIST.setStrings(previousWhitelist);
        buildEntityLists();
        return false;
    }

    private static BlockConfigFingerprint currentBlockFingerprint() {
        return new BlockConfigFingerprint(
                FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue(),
                BuilderFocusConfig.BLOCK_RULE_MODE.getValue(),
                Set.copyOf(BuilderFocusConfig.BLOCK_BLACKLIST.getStrings()),
                Set.copyOf(BuilderFocusConfig.BLOCK_WHITELIST.getStrings()));
    }

    private static BlockRules compileBlockRules(BlockConfigFingerprint fingerprint) {
        return new BlockRules(
                fingerprint.enabled(),
                fingerprint.mode(),
                resolveBlocks(fingerprint.blacklist()),
                resolveBlocks(fingerprint.whitelist()));
    }

    private static Set<Block> resolveBlocks(Set<String> entries) {
        Set<String> normalized = normalizeIds(entries);
        if (normalized.isEmpty()) return Set.of();
        LinkedHashSet<Block> resolved = new LinkedHashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null && normalized.contains(id.toString())) resolved.add(block);
        }
        return Set.copyOf(resolved);
    }

    private static Set<EntityType<?>> resolveEntityTypes(List<String> entries) {
        LinkedHashSet<EntityType<?>> resolved = new LinkedHashSet<>();
        for (String raw : entries) {
            Identifier id = Identifier.tryParse(raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT));
            if (id == null) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type != null) resolved.add(type);
        }
        return Set.copyOf(resolved);
    }

    private static Set<String> normalizeIds(Iterable<String> entries) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (entries == null) return Set.of();
        for (String raw : entries) {
            Identifier id = Identifier.tryParse(raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT));
            if (id != null) normalized.add(id.toString());
        }
        return Set.copyOf(normalized);
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

    private record BlockRules(
            boolean enabled,
            ChiseRuleMode mode,
            Set<Block> blacklist,
            Set<Block> whitelist) {
        private BlockRules {
            Objects.requireNonNull(mode, "mode");
            blacklist = Set.copyOf(blacklist);
            whitelist = Set.copyOf(whitelist);
        }

        static BlockRules none() {
            return new BlockRules(false, ChiseRuleMode.NONE, Set.of(), Set.of());
        }

        boolean hides(Block block) {
            return shouldHideByRule(
                    enabled,
                    mode,
                    blacklist.contains(block),
                    whitelist.contains(block));
        }

        FilterDecision inspect(Block block, String targetId) {
            return inspectByRule(
                    enabled,
                    mode,
                    blacklist.contains(block),
                    whitelist.contains(block),
                    targetId);
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

        FilterDecision inspect(EntityType<?> type, String targetId) {
            return inspectByRule(
                    true,
                    mode,
                    blacklist.contains(type),
                    whitelist.contains(type),
                    targetId);
        }
    }

    public record FilterDecision(boolean hidden, String reason, String matchedRule) {
        public FilterDecision {
            reason = Objects.requireNonNull(reason);
            if (reason.isBlank()) throw new IllegalArgumentException();
            matchedRule = matchedRule == null ? "" : matchedRule;
        }

        private static FilterDecision visible(String reason) {
            return new FilterDecision(false, reason, "");
        }

        private static FilterDecision visible(String reason, String matchedRule) {
            return new FilterDecision(false, reason, matchedRule);
        }

        private static FilterDecision hidden(String reason, String matchedRule) {
            return new FilterDecision(true, reason, matchedRule);
        }
    }
}
