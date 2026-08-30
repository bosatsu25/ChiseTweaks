package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import dev.chise.chisetweaks.feature.rendering.model.OreHighlightModelReload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/** Ore Highlight compatibility override adapter. */
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
        return (entries == null ? 0 : entries.size())
                < OreHighlightCompatibilityConfig.MAX_ENTRIES;
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
