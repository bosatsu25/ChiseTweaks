package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

/** Business boundary used by the shared list-editor widget shell. */
interface ChiseListEditorBackend {
    record Mutation(String feedback, boolean clearInputs, boolean resetPage, boolean moveToLastPage) {
        static Mutation none(String feedback) {
            return new Mutation(feedback, false, false, false);
        }

        static Mutation success(
                String feedback,
                boolean clearInputs,
                boolean resetPage,
                boolean moveToLastPage) {
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

    Component title();

    default Component screenTitle() {
        return title();
    }

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
        return Component.literal("");
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
        return Component.literal("");
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

    default String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    default String text(String key) {
        return Component.translatable(key).getString();
    }

    static boolean isRegisteredBlock(Identifier id) {
        return id != null && BuiltInRegistries.BLOCK.getValue(id) != null;
    }

    static boolean isRegisteredEntity(Identifier id) {
        EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getValue(id);
        return type != null;
    }

    static boolean isRegisteredItem(Identifier id) {
        Item item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
        return item != null;
    }
}
