package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.BuilderFocusConfig;
import dev.chise.chisetweaks.config.ChiseRuleMode;
import dev.chise.chisetweaks.config.ChiseRuleModeSetting;
import dev.chise.chisetweaks.config.ChiseStringListSetting;
import dev.chise.chisetweaks.config.SettingPersistence;
import dev.chise.chisetweaks.config.SettingPersistenceCoordinator;
import dev.chise.chisetweaks.core.policy.ConfigListPolicy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Builder Focus block/entity rule-list adapter. */
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
        if (!persist(setting.persistence())) {
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
        if (!persist(setting.persistence())) {
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

        if (!persist(setting.persistence())) {
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
        if (!persist(setting.persistence())) {
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
        return blockTarget
                ? BuilderFocusConfig.BLOCK_RULE_MODE
                : BuilderFocusConfig.ENTITY_RULE_MODE;
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

    private boolean persist(SettingPersistence domain) {
        return persistence.save(Set.of(domain)).successful();
    }
}
