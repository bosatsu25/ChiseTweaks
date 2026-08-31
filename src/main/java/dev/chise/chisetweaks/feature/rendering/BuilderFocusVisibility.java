package dev.chise.chisetweaks.feature.rendering;

import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseRuleMode;
import dev.chise.chisetweaks.config.FeatureConfig;
import dev.chise.chisetweaks.config.FeatureSwitches;
import dev.chise.chisetweaks.config.SettingChangeDispatcher;
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
    private static volatile BlockConfigFingerprint blockFingerprint = BlockConfigFingerprint.empty();
    private static volatile EntityConfigFingerprint entityFingerprint = EntityConfigFingerprint.empty();
    private static volatile BlockRules blockRules = BlockRules.none();
    private static volatile EntityRules entityRules = EntityRules.none();

    private BuilderFocusVisibility() {}

    public static void applyConfig() {
        BlockConfigFingerprint nextBlock = currentBlockFingerprint();
        EntityConfigFingerprint nextEntity = currentEntityFingerprint();
        blockRules = compileBlockRules(nextBlock);
        entityRules = compileEntityRules(nextEntity);
        blockFingerprint = nextBlock;
        entityFingerprint = nextEntity;
    }

    public static void buildLists() {
        BlockConfigFingerprint next = currentBlockFingerprint();
        if (next.equals(blockFingerprint)) return;

        // compile完了前にfingerprintを進めると、失敗後の同一設定再試行が抑止されるためcommitは最後に行う。
        BlockRules nextRules = compileBlockRules(next);
        blockRules = nextRules;
        blockFingerprint = next;

        if (BuilderFocusConfig.REFRESH_RENDERER.getBooleanValue()) {
            ChunkRenderInvalidation.request();
        }
    }

    public static void buildEntityLists() {
        EntityConfigFingerprint next = currentEntityFingerprint();
        if (next.equals(entityFingerprint)) return;
        entityRules = compileEntityRules(next);
        entityFingerprint = next;
    }

    public static boolean shouldHide(Block block) {
        if (block == null || !FeatureSwitches.BUILDER_FOCUS_BLOCKS.getBooleanValue()) return false;
        if (BuiltInRegistries.BLOCK.getKey(block) == null) return false;
        return blockRules.hides(block);
    }

    static boolean shouldHideByRule(
            boolean enabled,
            ChiseRuleMode mode,
            boolean blacklistMatch,
            boolean whitelistMatch) {
        if (!enabled || mode == null || mode == ChiseRuleMode.NONE) return false;
        return mode == ChiseRuleMode.BLACKLIST ? blacklistMatch : !whitelistMatch;
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

    public static boolean applyPreset(String presetId) {
        String preset = presetId == null ? "" : presetId.trim().toLowerCase(Locale.ROOT);
        ChiseRuleMode previousMode = BuilderFocusConfig.ENTITY_RULE_MODE.getValue();
        List<String> previousBlacklist = BuilderFocusConfig.ENTITY_BLACKLIST.getStrings();
        List<String> previousWhitelist = BuilderFocusConfig.ENTITY_WHITELIST.getStrings();

        ChiseRuleMode nextMode;
        List<String> nextBlacklist;
        List<String> nextWhitelist;
        switch (preset) {
            case "build_review" -> {
                nextMode = ChiseRuleMode.BLACKLIST;
                nextBlacklist = List.of(
                        "minecraft:item", "minecraft:experience_orb", "minecraft:area_effect_cloud");
                nextWhitelist = List.of();
            }
            case "technical_trace" -> {
                nextMode = ChiseRuleMode.WHITELIST;
                nextBlacklist = List.of();
                nextWhitelist = List.of(
                        "minecraft:armor_stand", "minecraft:item_frame", "minecraft:glow_item_frame",
                        "minecraft:minecart", "minecraft:hopper_minecart", "minecraft:chest_minecart");
            }
            case "photo" -> {
                nextMode = ChiseRuleMode.BLACKLIST;
                nextBlacklist = List.of(
                        "minecraft:item", "minecraft:experience_orb", "minecraft:area_effect_cloud",
                        "minecraft:marker");
                nextWhitelist = List.of();
            }
            case "clear" -> {
                nextMode = ChiseRuleMode.NONE;
                nextBlacklist = List.of();
                nextWhitelist = List.of();
            }
            default -> {
                return false;
            }
        }

        EntityConfigFingerprint previousFingerprint = currentEntityFingerprint();
        setEntityRulesSilently(nextMode, nextBlacklist, nextWhitelist);
        FeatureConfig.sanitizeStringLists();
        boolean changed = !currentEntityFingerprint().equals(previousFingerprint);
        buildEntityLists();
        if (FeatureConfig.saveToFile()) {
            if (changed) SettingChangeDispatcher.markChanged();
            return true;
        }

        setEntityRulesSilently(previousMode, previousBlacklist, previousWhitelist);
        buildEntityLists();
        return false;
    }

    private static void setEntityRulesSilently(
            ChiseRuleMode mode,
            List<String> blacklist,
            List<String> whitelist) {
        BuilderFocusConfig.ENTITY_RULE_MODE.setValueSilently(mode);
        BuilderFocusConfig.ENTITY_BLACKLIST.setStringsSilently(blacklist);
        BuilderFocusConfig.ENTITY_WHITELIST.setStringsSilently(whitelist);
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

    private static Set<Block> resolveBlocks(Iterable<String> entries) {
        if (entries == null) return Set.of();
        LinkedHashSet<Block> resolved = new LinkedHashSet<>();
        for (String raw : entries) {
            Identifier id = Identifier.tryParse(raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT));
            if (id == null) continue;
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null && id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
                resolved.add(block);
            }
        }
        return Set.copyOf(resolved);
    }

    private static Set<EntityType<?>> resolveEntityTypes(Iterable<String> entries) {
        if (entries == null) return Set.of();
        LinkedHashSet<EntityType<?>> resolved = new LinkedHashSet<>();
        for (String raw : entries) {
            Identifier id = Identifier.tryParse(raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT));
            if (id == null) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type))) {
                resolved.add(type);
            }
        }
        return Set.copyOf(resolved);
    }

    private static EntityConfigFingerprint currentEntityFingerprint() {
        return new EntityConfigFingerprint(
                FeatureSwitches.BUILDER_FOCUS_ENTITIES.getBooleanValue(),
                BuilderFocusConfig.ENTITY_RULE_MODE.getValue(),
                Set.copyOf(BuilderFocusConfig.ENTITY_BLACKLIST.getStrings()),
                Set.copyOf(BuilderFocusConfig.ENTITY_WHITELIST.getStrings()));
    }

    private static EntityRules compileEntityRules(EntityConfigFingerprint fingerprint) {
        return new EntityRules(
                fingerprint.mode(),
                resolveEntityTypes(fingerprint.blacklist()),
                resolveEntityTypes(fingerprint.whitelist()));
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

    private record EntityConfigFingerprint(
            boolean enabled,
            ChiseRuleMode mode,
            Set<String> blacklist,
            Set<String> whitelist) {
        private EntityConfigFingerprint {
            Objects.requireNonNull(mode, "mode");
            blacklist = Set.copyOf(blacklist);
            whitelist = Set.copyOf(whitelist);
        }

        static EntityConfigFingerprint empty() {
            return new EntityConfigFingerprint(false, ChiseRuleMode.NONE, Set.of(), Set.of());
        }
    }

    private static final class BlockRules {
        private final boolean enabled;
        private final ChiseRuleMode mode;
        private final Set<Block> blacklist;
        private final Set<Block> whitelist;

        private BlockRules(
                boolean enabled,
                ChiseRuleMode mode,
                Set<Block> blacklist,
                Set<Block> whitelist) {
            this.enabled = enabled;
            this.mode = Objects.requireNonNull(mode, "mode");
            this.blacklist = Set.copyOf(blacklist);
            this.whitelist = Set.copyOf(whitelist);
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

    }

    private static final class EntityRules {
        private final ChiseRuleMode mode;
        private final Set<EntityType<?>> blacklist;
        private final Set<EntityType<?>> whitelist;

        private EntityRules(
                ChiseRuleMode mode,
                Set<EntityType<?>> blacklist,
                Set<EntityType<?>> whitelist) {
            this.mode = Objects.requireNonNull(mode, "mode");
            this.blacklist = Set.copyOf(blacklist);
            this.whitelist = Set.copyOf(whitelist);
        }

        static EntityRules none() {
            return new EntityRules(ChiseRuleMode.NONE, Set.of(), Set.of());
        }

        boolean hides(EntityType<?> type) {
            if (mode == ChiseRuleMode.BLACKLIST) return blacklist.contains(type);
            return mode == ChiseRuleMode.WHITELIST && !whitelist.contains(type);
        }

    }

}
