package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.MasaIdListPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Masa ecosystem list/map adapter. */
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
        return Component.translatable(switch (kind) {
            case PICK_REDIRECT -> "screen.chisetweaks.masa_editor.title.pick_redirect";
            case AUTO_PICK_GUARD -> "screen.chisetweaks.masa_editor.title.auto_pick_guard";
            case TOOL_SWITCH_GUARD -> "screen.chisetweaks.masa_editor.title.tool_switch_guard";
        });
    }

    @Override
    public Component screenTitle() {
        return Component.translatable("screen.chisetweaks.masa_editor.screen_title");
    }

    @Override
    public String subtitle() {
        return isGuard() ? text("screen.chisetweaks.masa_editor.subtitle.guard") : "";
    }

    @Override
    public boolean hasMode() {
        return isGuard();
    }

    @Override
    public Component modeMessage() {
        String modeKey = switch (mode()) {
            case MasaIdListPolicy.WHITELIST -> "screen.chisetweaks.masa_editor.mode.allow";
            case MasaIdListPolicy.BLACKLIST -> "screen.chisetweaks.masa_editor.mode.deny";
            default -> "screen.chisetweaks.masa_editor.mode.disabled";
        };
        return Component.translatable(
                "screen.chisetweaks.masa_editor.mode.label",
                Component.translatable(modeKey));
    }

    @Override
    public Mutation cycleMode() {
        if (!isGuard()) return Mutation.none("");

        int previous = mode();
        int next = nextMode(previous);
        setMode(next);
        if (!save()) {
            setMode(previous);
            return Mutation.none(text("screen.chisetweaks.masa_editor.feedback.save_failed"));
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
        return Component.translatable(hasSecondInput()
                ? "screen.chisetweaks.masa_editor.input.schematic_block"
                : "screen.chisetweaks.masa_editor.input.target_id");
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
        return Component.translatable("screen.chisetweaks.masa_editor.input.replacement_block");
    }

    @Override
    public String secondInputHint() {
        return "minecraft:dirt";
    }

    @Override
    public Component addLabel(boolean compact) {
        return Component.translatable("screen.chisetweaks.masa_editor.add");
    }

    @Override
    public Component removeLabel() {
        return Component.translatable("screen.chisetweaks.masa_editor.remove");
    }

    @Override
    public Component previousLabel(boolean compact) {
        return Component.translatable("screen.chisetweaks.masa_editor.previous");
    }

    @Override
    public Component nextLabel(boolean compact) {
        return Component.translatable("screen.chisetweaks.masa_editor.next");
    }

    @Override
    public Component clearLabel(boolean compact) {
        return Component.translatable("screen.chisetweaks.masa_editor.clear");
    }

    @Override
    public Component backLabel() {
        return Component.translatable("screen.chisetweaks.common.back");
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
            return Mutation.none(text("screen.chisetweaks.masa_editor.feedback.unregistered"));
        }

        String entry = normalizedFirst;
        if (hasSecondInput()) {
            String normalizedSecond = normalize(second);
            if (!validRegisteredId(normalizedSecond, false)) {
                return Mutation.none(text("screen.chisetweaks.masa_editor.feedback.invalid_replacement"));
            }
            entry = normalizedFirst + "," + normalizedSecond;
        }

        List<String> previous = List.copyOf(entries());
        if (previous.contains(entry)) {
            return Mutation.none(text("screen.chisetweaks.masa_editor.feedback.duplicate"));
        }
        if (previous.size() >= MasaIntegrationConfig.MAX_LIST_ENTRIES) {
            return Mutation.none(text("screen.chisetweaks.masa_editor.feedback.limit"));
        }

        ArrayList<String> updated = new ArrayList<>(previous);
        updated.add(entry);
        setEntries(updated);
        if (!save()) {
            setEntries(previous);
            return Mutation.none("保存に失敗しました");
        }
        return Mutation.success(text("screen.chisetweaks.masa_editor.feedback.added"), true, false, true);
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
        return Mutation.success(text("screen.chisetweaks.masa_editor.feedback.removed"), false, false, false);
    }

    @Override
    public Mutation clear() {
        List<String> previous = List.copyOf(entries());
        setEntries(List.of());
        if (!save()) {
            setEntries(previous);
            return Mutation.none("保存に失敗しました");
        }
        return Mutation.success(text("screen.chisetweaks.masa_editor.feedback.cleared"), false, true, false);
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
            return;
        }

        if (kind == Kind.AUTO_PICK_GUARD) {
            if (mode() == MasaIdListPolicy.WHITELIST) {
                config.tweakermoreAutoPickWhitelist = copy;
            } else if (mode() == MasaIdListPolicy.BLACKLIST) {
                config.tweakermoreAutoPickBlacklist = copy;
            }
            return;
        }

        if (mode() == MasaIdListPolicy.WHITELIST) {
            config.tweakerooToolSwitchWhitelist = copy;
        } else if (mode() == MasaIdListPolicy.BLACKLIST) {
            config.tweakerooToolSwitchBlacklist = copy;
        }
    }

    private boolean save() {
        config.sanitize();
        return config.save();
    }

    private static boolean validRegisteredId(String raw, boolean itemTarget) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null) return false;
        return itemTarget
                ? ChiseListEditorBackend.isRegisteredItem(id)
                : ChiseListEditorBackend.isRegisteredBlock(id);
    }
}
