package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseRuleMode;
import dev.chise.chisetweaks.config.ChiseRuleModeSetting;
import dev.chise.chisetweaks.config.ChiseStringListSetting;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import dev.chise.chisetweaks.config.SettingPersistenceCoordinator;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import dev.chise.chisetweaks.core.policy.MasaIdListPolicy;
import dev.chise.chisetweaks.feature.rendering.model.OreHighlightModelReload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Domain adapter for the shared list-editor screen.
 * The screen owns widget lifecycle; adapters own validation, persistence and mode transitions.
 */
interface ChiseListEditorBackend {
    record Mutation(String feedback, boolean clearInputs, boolean resetPage, boolean moveToLastPage) {
        static Mutation none(String feedback) {
            return new Mutation(feedback, false, false, false);
        }

        static Mutation success(String feedback, boolean clearInputs, boolean resetPage, boolean moveToLastPage) {
            return new Mutation(feedback, clearInputs, resetPage, moveToLastPage);
        }
    }

    static ChiseListEditorBackend create(ChiseListEditorScreen.Target target) {
        ChiseListEditorScreen.Target resolved =
                target == null ? ChiseListEditorScreen.Target.BLOCK_FILTER : target;
        return switch (resolved) {
            case BLOCK_FILTER -> new SceneFilterBackend(true);
            case ENTITY_FILTER -> new SceneFilterBackend(false);
            case ORE_COMPATIBILITY -> new OreCompatibilityBackend();
            case LITEMATICA_PICK_REDIRECT -> new MasaListBackend(MasaListBackend.Kind.PICK_REDIRECT);
            case TWEAKERMORE_AUTO_PICK_GUARD -> new MasaListBackend(MasaListBackend.Kind.AUTO_PICK_GUARD);
            case TWEAKEROO_TOOL_SWITCH_GUARD -> new MasaListBackend(MasaListBackend.Kind.TOOL_SWITCH_GUARD);
        };
    }

    static Component screenTitle(ChiseListEditorScreen.Target target) {
        return create(target).title();
    }

    Component title();

    default String subtitle() {
        return "";
    }

    default boolean usesOreLayout() {
        return false;
    }

    default boolean hasMode() {
        return false;
    }

    default int preferredModeWidth() {
        return 240;
    }

    default Component modeMessage() {
        return Component.empty();
    }

    default Mutation cycleMode() {
        return Mutation.none("");
    }

    default boolean hasSecondInput() {
        return false;
    }

    default int standardListTop() {
        return 112;
    }

    default int standardClearWidth() {
        return 110;
    }

    Component firstInputLabel();

    String firstInputHint();

    default Component secondInputLabel() {
        return Component.empty();
    }

    default String secondInputHint() {
        return "";
    }

    Component addLabel(boolean compact);

    Component removeLabel();

    Component previousLabel(boolean compact);

    Component nextLabel(boolean compact);

    Component clearLabel(boolean compact);

    Component backLabel();

    boolean editable();

    int entryCount();

    String entryText(int index);

    boolean canSubmit(String first, String second);

    Mutation add(String first, String second, OreHighlightStyle style);

    Mutation remove(int index);

    Mutation clear();

    static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    static String text(String key) {
        return Component.translatable(key).getString();
    }

    static boolean isRegisteredBlock(Identifier id) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (id.equals(BuiltInRegistries.BLOCK.getKey(block))) return true;
        }
        return false;
    }
}

final class SceneFilterBackend implements ChiseListEditorBackend {
    private final boolean blockTarget;
    private final SettingPersistenceCoordinator persistence = SettingPersistenceCoordinator.production();

    SceneFilterBackend(boolean blockTarget) {
        this.blockTarget = blockTarget;
    }

    @Override
    public Component title() {
        return Component.translatable("screen.chisetweaks.scene_filter.title");
    }

    @Override
    public String subtitle() {
        ChiseRuleMode mode = modeSetting().getValue();
        if (mode == ChiseRuleMode.NONE) {
            return text("screen.chisetweaks.scene_filter.mode.description.disabled");
        }
        return text(mode == ChiseRuleMode.WHITELIST
                ? "screen.chisetweaks.scene_filter.mode.description.allow"
                : "screen.chisetweaks.scene_filter.mode.description.hide");
    }

    @Override
    public boolean hasMode() {
        return true;
    }

    @Override
    public int preferredModeWidth() {
        return 220;
    }

    @Override
    public Component modeMessage() {
        Component targetName = Component.translatable(blockTarget
                ? "screen.chisetweaks.scene_filter.target.blocks"
                : "screen.chisetweaks.scene_filter.target.entities");
        ChiseRuleMode mode = modeSetting().getValue();
        String modeKey = mode == ChiseRuleMode.NONE
                ? "screen.chisetweaks.scene_filter.mode.disabled"
                : mode == ChiseRuleMode.WHITELIST
                        ? "screen.chisetweaks.scene_filter.mode.allow"
                        : "screen.chisetweaks.scene_filter.mode.hide";
        return Component.translatable(
                "screen.chisetweaks.scene_filter.mode.label",
                targetName,
                Component.translatable(modeKey));
    }

    @Override
    public Mutation cycleMode() {
        ChiseRuleModeSetting setting = modeSetting();
        ChiseRuleMode previous = setting.getValue();
        ChiseRuleMode next = nextMode(previous);
        if (!setting.setValue(next)) return Mutation.none("");
        if (!persist(setting)) {
            setting.setValue(previous);
            return Mutation.none(text("screen.chisetweaks.scene_filter.feedback.save_failed"));
        }
        return Mutation.success("", false, true, false);
    }

    static ChiseRuleMode nextMode(ChiseRuleMode current) {
        return current == ChiseRuleMode.NONE
                ? ChiseRuleMode.BLACKLIST
                : current == ChiseRuleMode.BLACKLIST
                        ? ChiseRuleMode.WHITELIST
                        : ChiseRuleMode.NONE;
    }

    @Override
    public int standardClearWidth() {
        return 104;
    }

    @Override
    public Component firstInputLabel() {
        return Component.translatable("screen.chisetweaks.scene_filter.target_id");
    }

    @Override
    public String firstInputHint() {
        return blockTarget ? "minecraft:stone" : "minecraft:item";
    }

    @Override
    public Component addLabel(boolean compact) {
        return Component.translatable("screen.chisetweaks.scene_filter.add");
    }

    @Override
    public Component removeLabel() {
        return Component.translatable("screen.chisetweaks.scene_filter.remove");
    }

    @Override
    public Component previousLabel(boolean compact) {
        return Component.translatable("screen.chisetweaks.scene_filter.previous");
    }

    @Override
    public Component nextLabel(boolean compact) {
        return Component.translatable("screen.chisetweaks.scene_filter.next");
    }

    @Override
    public Component clearLabel(boolean compact) {
        return Component.translatable("screen.chisetweaks.scene_filter.clear");
    }

    @Override
    public Component backLabel() {
        return Component.translatable("screen.chisetweaks.common.back");
    }

    @Override
    public boolean editable() {
        return modeSetting().getValue() != ChiseRuleMode.NONE;
    }

    @Override
    public int entryCount() {
        ChiseStringListSetting setting = activeList();
        return setting == null ? 0 : setting.getStrings().size();
    }

    @Override
    public String entryText(int index) {
        ChiseStringListSetting setting = activeList();
        return setting == null ? "" : setting.getStrings().get(index);
    }

    @Override
    public boolean canSubmit(String first, String second) {
        return editable()
                && !normalize(first).isEmpty()
                && entryCount() < ConfigListPolicy.MAX_ENTRIES;
    }

    @Override
    public Mutation add(String first, String second, OreHighlightStyle style) {
        ChiseStringListSetting setting = activeList();
        if (setting == null) return Mutation.none("");
        Identifier id = Identifier.tryParse(normalize(first));
        if (id == null) {
            return Mutation.none(text("screen.chisetweaks.scene_filter.feedback.invalid_id"));
        }
        if (!isRegisteredTarget(id)) {
            return Mutation.none(text("screen.chisetweaks.scene_filter.feedback.unregistered"));
        }

        String normalized = id.toString();
        List<String> previous = setting.getStrings();
        if (previous.contains(normalized)) {
            return Mutation.none(text("screen.chisetweaks.scene_filter.feedback.duplicate"));
        }

        ArrayList<String> updated = new ArrayList<>(previous);
        updated.add(normalized);
        List<String> sanitized = ConfigListPolicy.sanitize(updated);
        if (!sanitized.contains(normalized)) {
            return Mutation.none(text("screen.chisetweaks.scene_filter.feedback.rejected"));
        }
        if (!setting.setStrings(sanitized)) return Mutation.none("");
        if (!persist(setting)) {
            setting.setStrings(previous);
            return Mutation.none(text("screen.chisetweaks.scene_filter.feedback.save_failed"));
        }
        return Mutation.success(
                text("screen.chisetweaks.scene_filter.feedback.added"),
                true,
                false,
                true);
    }

    @Override
    public Mutation remove(int index) {
        ChiseStringListSetting setting = activeList();
        if (setting == null) return Mutation.none("");
        List<String> previous = setting.getStrings();
        ArrayList<String> updated = new ArrayList<>(previous);
        updated.remove(index);
        if (!setting.setStrings(updated)) return Mutation.none("");
        if (!persist(setting)) {
            setting.setStrings(previous);
            return Mutation.none(text("screen.chisetweaks.scene_filter.feedback.save_failed"));
        }
        return Mutation.success(
                text("screen.chisetweaks.scene_filter.feedback.removed"),
                false,
                false,
                false);
    }

    @Override
    public Mutation clear() {
        ChiseStringListSetting setting = activeList();
        if (setting == null) return Mutation.none("");
        List<String> previous = setting.getStrings();
        if (!setting.setStrings(List.of())) return Mutation.none("");
        if (!persist(setting)) {
            setting.setStrings(previous);
            return Mutation.none(text("screen.chisetweaks.scene_filter.feedback.save_failed"));
        }
        return Mutation.success(
                text("screen.chisetweaks.scene_filter.feedback.cleared"),
                false,
                true,
                false);
    }

    private ChiseRuleModeSetting modeSetting() {
        return blockTarget ? BuilderFocusConfig.BLOCK_RULE_MODE : BuilderFocusConfig.ENTITY_RULE_MODE;
    }

    private ChiseStringListSetting activeList() {
        ChiseRuleMode mode = modeSetting().getValue();
        if (mode == ChiseRuleMode.NONE) return null;
        if (blockTarget) {
            return mode == ChiseRuleMode.WHITELIST
                    ? BuilderFocusConfig.BLOCK_WHITELIST
                    : BuilderFocusConfig.BLOCK_BLACKLIST;
        }
        return mode == ChiseRuleMode.WHITELIST
                ? BuilderFocusConfig.ENTITY_WHITELIST
                : BuilderFocusConfig.ENTITY_BLACKLIST;
    }

    private boolean isRegisteredTarget(Identifier id) {
        if (blockTarget) return ChiseListEditorBackend.isRegisteredBlock(id);
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type))) return true;
        }
        return false;
    }

    private boolean persist(dev.chise.chisetweaks.config.ChiseSetting<?> setting) {
        return persistence.save(Set.of(setting.persistence())).successful();
    }
}

final class MasaListBackend implements ChiseListEditorBackend {
    enum Kind {
        PICK_REDIRECT,
        AUTO_PICK_GUARD,
        TOOL_SWITCH_GUARD
    }

    private final Kind kind;
    private final MasaIntegrationConfig config = MasaIntegrationConfig.getInstance();

    MasaListBackend(Kind kind) {
        this.kind = kind;
    }

    @Override
    public Component title() {
        return Component.literal(switch (kind) {
            case PICK_REDIRECT -> "Litematica Pick Redirect";
            case AUTO_PICK_GUARD -> "TweakerMore Auto Pick Guard";
            case TOOL_SWITCH_GUARD -> "Tweakeroo Tool Switch Guard";
        });
    }

    @Override
    public String subtitle() {
        return isGuard() ? "外部MODの操作をChise policyで許可/拒否します。" : "";
    }

    @Override
    public boolean hasMode() {
        return isGuard();
    }

    @Override
    public Component modeMessage() {
        String modeName = switch (mode()) {
            case MasaIdListPolicy.WHITELIST -> "ALLOW / Whitelist";
            case MasaIdListPolicy.BLACKLIST -> "DENY / Blacklist";
            default -> "Disabled";
        };
        return Component.literal("Guard mode: " + modeName);
    }

    @Override
    public Mutation cycleMode() {
        if (!isGuard()) return Mutation.none("");
        int previous = mode();
        int next = nextMode(previous);
        setMode(next);
        if (!save()) {
            setMode(previous);
            return Mutation.none("保存に失敗しました");
        }
        return Mutation.success("", false, true, false);
    }

    static int nextMode(int current) {
        return current == MasaIdListPolicy.NONE
                ? MasaIdListPolicy.BLACKLIST
                : current == MasaIdListPolicy.BLACKLIST
                        ? MasaIdListPolicy.WHITELIST
                        : MasaIdListPolicy.NONE;
    }

    @Override
    public boolean hasSecondInput() {
        return kind == Kind.PICK_REDIRECT;
    }

    @Override
    public int standardListTop() {
        return hasSecondInput() ? 116 : 112;
    }

    @Override
    public Component firstInputLabel() {
        return hasSecondInput()
                ? Component.literal("Schematic block")
                : Component.literal("Target ID");
    }

    @Override
    public String firstInputHint() {
        return switch (kind) {
            case PICK_REDIRECT -> "minecraft:farmland";
            case AUTO_PICK_GUARD -> "minecraft:golden_carrot";
            case TOOL_SWITCH_GUARD -> "minecraft:glass";
        };
    }

    @Override
    public Component secondInputLabel() {
        return Component.literal("Replacement block");
    }

    @Override
    public String secondInputHint() {
        return "minecraft:dirt";
    }

    @Override
    public Component addLabel(boolean compact) {
        return Component.literal("追加");
    }

    @Override
    public Component removeLabel() {
        return Component.literal("削除");
    }

    @Override
    public Component previousLabel(boolean compact) {
        return Component.literal("前");
    }

    @Override
    public Component nextLabel(boolean compact) {
        return Component.literal("次");
    }

    @Override
    public Component clearLabel(boolean compact) {
        return Component.literal("リストを消去");
    }

    @Override
    public Component backLabel() {
        return Component.literal("戻る");
    }

    @Override
    public boolean editable() {
        return !isGuard() || mode() != MasaIdListPolicy.NONE;
    }

    @Override
    public int entryCount() {
        return entries().size();
    }

    @Override
    public String entryText(int index) {
        return entries().get(index);
    }

    @Override
    public boolean canSubmit(String first, String second) {
        return editable()
                && !normalize(first).isEmpty()
                && (!hasSecondInput() || !normalize(second).isEmpty())
                && entryCount() < MasaIntegrationConfig.MAX_LIST_ENTRIES;
    }

    @Override
    public Mutation add(String first, String second, OreHighlightStyle style) {
        String normalizedFirst = normalize(first);
        if (!validRegisteredId(normalizedFirst, kind == Kind.AUTO_PICK_GUARD)) {
            return Mutation.none("登録済みIDを入力してください");
        }

        String entry = normalizedFirst;
        if (hasSecondInput()) {
            String normalizedSecond = normalize(second);
            if (!validRegisteredId(normalizedSecond, false)) {
                return Mutation.none("代替先Block IDが正しくありません");
            }
            entry = normalizedFirst + "," + normalizedSecond;
        }

        List<String> previous = List.copyOf(entries());
        if (previous.contains(entry)) return Mutation.none("既に登録されています");
        if (previous.size() >= MasaIntegrationConfig.MAX_LIST_ENTRIES) {
            return Mutation.none("登録上限に達しています");
        }

        ArrayList<String> updated = new ArrayList<>(previous);
        updated.add(entry);
        setEntries(updated);
        if (!save()) {
            setEntries(previous);
            return Mutation.none("保存に失敗しました");
        }
        return Mutation.success("追加しました", true, false, true);
    }

    @Override
    public Mutation remove(int index) {
        List<String> previous = List.copyOf(entries());
        ArrayList<String> updated = new ArrayList<>(previous);
        updated.remove(index);
        setEntries(updated);
        if (!save()) {
            setEntries(previous);
            return Mutation.none("保存に失敗しました");
        }
        return Mutation.success("削除しました", false, false, false);
    }

    @Override
    public Mutation clear() {
        List<String> previous = List.copyOf(entries());
        setEntries(List.of());
        if (!save()) {
            setEntries(previous);
            return Mutation.none("保存に失敗しました");
        }
        return Mutation.success("リストを消去しました", false, true, false);
    }

    private boolean isGuard() {
        return kind != Kind.PICK_REDIRECT;
    }

    private int mode() {
        return kind == Kind.AUTO_PICK_GUARD
                ? MasaIdListPolicy.clampMode(config.tweakermoreAutoPickListMode)
                : kind == Kind.TOOL_SWITCH_GUARD
                        ? MasaIdListPolicy.clampMode(config.tweakerooToolSwitchListMode)
                        : MasaIdListPolicy.NONE;
    }

    private void setMode(int value) {
        int normalized = MasaIdListPolicy.clampMode(value);
        if (kind == Kind.AUTO_PICK_GUARD) {
            config.tweakermoreAutoPickListMode = normalized;
        } else if (kind == Kind.TOOL_SWITCH_GUARD) {
            config.tweakerooToolSwitchListMode = normalized;
        }
    }

    private List<String> entries() {
        if (kind == Kind.PICK_REDIRECT) return config.pickRedirectMap;
        if (kind == Kind.AUTO_PICK_GUARD) {
            return mode() == MasaIdListPolicy.WHITELIST
                    ? config.tweakermoreAutoPickWhitelist
                    : mode() == MasaIdListPolicy.BLACKLIST
                            ? config.tweakermoreAutoPickBlacklist
                            : List.of();
        }
        return mode() == MasaIdListPolicy.WHITELIST
                ? config.tweakerooToolSwitchWhitelist
                : mode() == MasaIdListPolicy.BLACKLIST
                        ? config.tweakerooToolSwitchBlacklist
                        : List.of();
    }

    private void setEntries(List<String> values) {
        ArrayList<String> copy = new ArrayList<>(values == null ? List.of() : values);
        if (kind == Kind.PICK_REDIRECT) {
            config.pickRedirectMap = copy;
        } else if (kind == Kind.AUTO_PICK_GUARD) {
            if (mode() == MasaIdListPolicy.WHITELIST) config.tweakermoreAutoPickWhitelist = copy;
            else if (mode() == MasaIdListPolicy.BLACKLIST) config.tweakermoreAutoPickBlacklist = copy;
        } else {
            if (mode() == MasaIdListPolicy.WHITELIST) config.tweakerooToolSwitchWhitelist = copy;
            else if (mode() == MasaIdListPolicy.BLACKLIST) config.tweakerooToolSwitchBlacklist = copy;
        }
    }

    private boolean save() {
        config.sanitize();
        return config.save();
    }

    private static boolean validRegisteredId(String raw, boolean itemTarget) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null) return false;
        if (!itemTarget) return ChiseListEditorBackend.isRegisteredBlock(id);
        for (Item item : BuiltInRegistries.ITEM) {
            if (id.equals(BuiltInRegistries.ITEM.getKey(item))) return true;
        }
        return false;
    }
}

final class OreCompatibilityBackend implements ChiseListEditorBackend {
    @Override
    public Component title() {
        return Component.translatable("screen.chisetweaks.ore_compat.title");
    }

    @Override
    public String subtitle() {
        return text("screen.chisetweaks.ore_compat.subtitle");
    }

    @Override
    public boolean usesOreLayout() {
        return true;
    }

    @Override
    public Component firstInputLabel() {
        return Component.translatable("screen.chisetweaks.ore_compat.block_id");
    }

    @Override
    public String firstInputHint() {
        return "examplemod:copper_ore";
    }

    @Override
    public Component addLabel(boolean compact) {
        return Component.translatable(compact
                ? "screen.chisetweaks.ore_compat.add"
                : "screen.chisetweaks.ore_compat.add_update");
    }

    @Override
    public Component removeLabel() {
        return Component.translatable("screen.chisetweaks.ore_compat.remove");
    }

    @Override
    public Component previousLabel(boolean compact) {
        return compact
                ? Component.literal("‹")
                : Component.translatable("screen.chisetweaks.ore_compat.previous");
    }

    @Override
    public Component nextLabel(boolean compact) {
        return compact
                ? Component.literal("›")
                : Component.translatable("screen.chisetweaks.ore_compat.next");
    }

    @Override
    public Component clearLabel(boolean compact) {
        return Component.translatable(compact
                ? "screen.chisetweaks.ore_compat.clear"
                : "screen.chisetweaks.ore_compat.clear_overrides");
    }

    @Override
    public Component backLabel() {
        return Component.translatable("screen.chisetweaks.common.back");
    }

    @Override
    public boolean editable() {
        return true;
    }

    @Override
    public int entryCount() {
        return entries().size();
    }

    @Override
    public String entryText(int index) {
        OreHighlightCompatibilityConfig.Entry entry = entries().get(index);
        return entry.blockId() + "  →  " + entry.style().key();
    }

    @Override
    public boolean canSubmit(String first, String second) {
        return canSubmitEntry(first, entries());
    }

    static boolean canSubmitEntry(
            String rawValue,
            List<OreHighlightCompatibilityConfig.Entry> entries) {
        if (rawValue == null || rawValue.isBlank()) return false;
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (entries != null) {
            for (OreHighlightCompatibilityConfig.Entry entry : entries) {
                if (entry != null && normalized.equals(entry.blockId())) return true;
            }
        }
        return (entries == null ? 0 : entries.size()) < OreHighlightCompatibilityConfig.MAX_ENTRIES;
    }

    @Override
    public Mutation add(String first, String second, OreHighlightStyle style) {
        Identifier id = Identifier.tryParse(normalize(first));
        if (id == null || "minecraft".equals(id.getNamespace())) {
            return Mutation.none(text("screen.chisetweaks.ore_compat.feedback.invalid"));
        }
        if (!ChiseListEditorBackend.isRegisteredBlock(id)) {
            return Mutation.none(text("screen.chisetweaks.ore_compat.feedback.unregistered"));
        }
        if (!OreHighlightCompatibilityConfig.put(id.toString(), style)) {
            return Mutation.none(text("screen.chisetweaks.ore_compat.feedback.save_failed"));
        }
        OreHighlightModelReload.request();
        return Mutation.success(
                text("screen.chisetweaks.ore_compat.feedback.saved"),
                true,
                false,
                true);
    }

    @Override
    public Mutation remove(int index) {
        List<OreHighlightCompatibilityConfig.Entry> current = entries();
        if (!OreHighlightCompatibilityConfig.remove(current.get(index).blockId())) {
            return Mutation.none(text("screen.chisetweaks.ore_compat.feedback.remove_failed"));
        }
        OreHighlightModelReload.request();
        return Mutation.success(
                text("screen.chisetweaks.ore_compat.feedback.removed"),
                false,
                false,
                false);
    }

    @Override
    public Mutation clear() {
        if (!OreHighlightCompatibilityConfig.clear()) {
            return Mutation.none(text("screen.chisetweaks.ore_compat.feedback.clear_failed"));
        }
        OreHighlightModelReload.request();
        return Mutation.success(
                text("screen.chisetweaks.ore_compat.feedback.cleared"),
                false,
                true,
                false);
    }

    private static List<OreHighlightCompatibilityConfig.Entry> entries() {
        return OreHighlightCompatibilityConfig.entries();
    }
}
