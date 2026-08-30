package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.config.MasaIntegrationConfig;
import dev.chise.chisetweaks.core.policy.MasaIdListPolicy;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Editor for user-owned Masa integration maps and allow/deny lists. */
public final class ChiseMasaIntegrationEditorScreen extends Screen {
    private static final int OUTER_MARGIN = 12;
    private static final int ROW_HEIGHT = 24;
    private static final int REMOVE_WIDTH = 68;

    public enum Target {
        LITEMATICA_PICK_REDIRECT,
        TWEAKERMORE_AUTO_PICK_GUARD,
        TWEAKEROO_TOOL_SWITCH_GUARD
    }

    private final Screen parent;
    private final Target target;
    private final MasaIntegrationConfig config = MasaIntegrationConfig.getInstance();
    private final ArrayList<Button> removeButtons = new ArrayList<>();

    private EditBox firstBox;
    private EditBox secondBox;
    private Button modeButton;
    private Button addButton;
    private Button clearButton;
    private Button previousButton;
    private Button nextButton;
    private int panelX;
    private int panelWidth;
    private int listTop;
    private int pageSize;
    private int page;
    private String feedback = "";

    public ChiseMasaIntegrationEditorScreen(Screen parent, Target target) {
        super(Component.literal("Masa Integration"));
        this.parent = parent;
        this.target = target == null ? Target.LITEMATICA_PICK_REDIRECT : target;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(760, Math.max(336, width - OUTER_MARGIN * 2));
        panelX = Math.max(OUTER_MARGIN, (width - panelWidth) / 2);
        listTop = target == Target.LITEMATICA_PICK_REDIRECT ? 116 : 112;
        pageSize = Math.max(2, Math.min(10, (Math.max(260, height) - listTop - 54) / ROW_HEIGHT));

        if (target != Target.LITEMATICA_PICK_REDIRECT) {
            modeButton = addRenderableWidget(Button.builder(
                    modeMessage(), ignored -> cycleMode())
                    .bounds(panelX + 8, 42, Math.min(240, panelWidth - 16), 20)
                    .build());
        }

        createInputs();
        createListButtons();
        createFooter();
        refreshControls();
    }

    private void createInputs() {
        int addWidth = 74;
        if (target == Target.LITEMATICA_PICK_REDIRECT) {
            int gap = 6;
            int usable = Math.max(160, panelWidth - addWidth - gap * 3 - 16);
            int fieldWidth = usable / 2;
            firstBox = addRenderableWidget(new EditBox(
                    font, panelX + 8, 70, fieldWidth, 20, Component.literal("Schematic block")));
            secondBox = addRenderableWidget(new EditBox(
                    font, panelX + 8 + fieldWidth + gap, 70, fieldWidth, 20,
                    Component.literal("Replacement block")));
            firstBox.setHint(Component.literal("minecraft:farmland"));
            secondBox.setHint(Component.literal("minecraft:dirt"));
            int addX = panelX + panelWidth - addWidth - 8;
            firstBox.setWidth(Math.max(70, addX - gap - firstBox.getX() - fieldWidth - gap));
            secondBox.setX(firstBox.getX() + firstBox.getWidth() + gap);
            secondBox.setWidth(Math.max(70, addX - gap - secondBox.getX()));
            addButton = addRenderableWidget(Button.builder(
                    Component.literal("追加"), ignored -> addEntry())
                    .bounds(addX, 70, addWidth, 20).build());
        } else {
            int inputWidth = Math.max(120, panelWidth - addWidth - 26);
            firstBox = addRenderableWidget(new EditBox(
                    font, panelX + 8, 70, inputWidth, 20, Component.literal("Target ID")));
            firstBox.setHint(Component.literal(target == Target.TWEAKERMORE_AUTO_PICK_GUARD
                    ? "minecraft:golden_carrot"
                    : "minecraft:glass"));
            addButton = addRenderableWidget(Button.builder(
                    Component.literal("追加"), ignored -> addEntry())
                    .bounds(panelX + 16 + inputWidth, 70, addWidth, 20).build());
        }
        firstBox.setResponder(ignored -> refreshControls());
        if (secondBox != null) secondBox.setResponder(ignored -> refreshControls());
    }

    private void createListButtons() {
        removeButtons.clear();
        for (int slot = 0; slot < pageSize; slot++) {
            final int visibleSlot = slot;
            Button remove = addRenderableWidget(Button.builder(
                    Component.literal("削除"), ignored -> removeEntry(visibleSlot))
                    .bounds(
                            panelX + panelWidth - REMOVE_WIDTH - 10,
                            listTop + slot * ROW_HEIGHT,
                            REMOVE_WIDTH,
                            20)
                    .build());
            removeButtons.add(remove);
        }
    }

    private void createFooter() {
        int footerY = height - 28;
        previousButton = addRenderableWidget(Button.builder(
                Component.literal("前"), ignored -> movePage(-1))
                .bounds(panelX + 8, footerY, 58, 20).build());
        nextButton = addRenderableWidget(Button.builder(
                Component.literal("次"), ignored -> movePage(1))
                .bounds(panelX + 70, footerY, 58, 20).build());
        clearButton = addRenderableWidget(Button.builder(
                Component.literal("リストを消去"), ignored -> clearEntries())
                .bounds(panelX + 132, footerY, 110, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("戻る"), ignored -> onClose())
                .bounds(panelX + panelWidth - 88, footerY, 80, 20).build());
    }

    private void cycleMode() {
        int previous = mode();
        int next = previous == MasaIdListPolicy.NONE
                ? MasaIdListPolicy.BLACKLIST
                : previous == MasaIdListPolicy.BLACKLIST
                        ? MasaIdListPolicy.WHITELIST
                        : MasaIdListPolicy.NONE;
        setMode(next);
        if (!save()) {
            setMode(previous);
            feedback = "保存に失敗しました";
        } else {
            page = 0;
            feedback = "";
        }
        refreshControls();
    }

    private void addEntry() {
        if (firstBox == null) return;
        String first = normalize(firstBox.getValue());
        if (!validRegisteredId(first, firstTargetKind())) {
            feedback = "登録済みIDを入力してください";
            refreshControls();
            return;
        }

        String entry = first;
        if (target == Target.LITEMATICA_PICK_REDIRECT) {
            String second = secondBox == null ? "" : normalize(secondBox.getValue());
            if (!validRegisteredId(second, TargetKind.BLOCK)) {
                feedback = "代替先Block IDが正しくありません";
                refreshControls();
                return;
            }
            entry = first + "," + second;
        }

        List<String> previous = List.copyOf(currentEntries());
        if (previous.contains(entry)) {
            feedback = "既に登録されています";
            refreshControls();
            return;
        }
        if (previous.size() >= MasaIntegrationConfig.MAX_LIST_ENTRIES) {
            feedback = "登録上限に達しています";
            refreshControls();
            return;
        }

        ArrayList<String> updated = new ArrayList<>(previous);
        updated.add(entry);
        setCurrentEntries(updated);
        if (!save()) {
            setCurrentEntries(previous);
            feedback = "保存に失敗しました";
            refreshControls();
            return;
        }
        firstBox.setValue("");
        if (secondBox != null) secondBox.setValue("");
        page = Math.max(0, (updated.size() - 1) / pageSize);
        feedback = "追加しました";
        refreshControls();
    }

    private void removeEntry(int visibleSlot) {
        int index = page * pageSize + visibleSlot;
        List<String> previous = List.copyOf(currentEntries());
        if (index < 0 || index >= previous.size()) return;
        ArrayList<String> updated = new ArrayList<>(previous);
        updated.remove(index);
        setCurrentEntries(updated);
        if (!save()) {
            setCurrentEntries(previous);
            feedback = "保存に失敗しました";
        } else {
            feedback = "削除しました";
        }
        clampPage();
        refreshControls();
    }

    private void clearEntries() {
        List<String> previous = List.copyOf(currentEntries());
        if (previous.isEmpty()) return;
        setCurrentEntries(List.of());
        if (!save()) {
            setCurrentEntries(previous);
            feedback = "保存に失敗しました";
        } else {
            page = 0;
            feedback = "リストを消去しました";
        }
        refreshControls();
    }

    private boolean save() {
        config.sanitize();
        return config.save();
    }

    private int mode() {
        return target == Target.TWEAKERMORE_AUTO_PICK_GUARD
                ? MasaIdListPolicy.clampMode(config.tweakermoreAutoPickListMode)
                : target == Target.TWEAKEROO_TOOL_SWITCH_GUARD
                        ? MasaIdListPolicy.clampMode(config.tweakerooToolSwitchListMode)
                        : MasaIdListPolicy.NONE;
    }

    private void setMode(int value) {
        int normalized = MasaIdListPolicy.clampMode(value);
        if (target == Target.TWEAKERMORE_AUTO_PICK_GUARD) {
            config.tweakermoreAutoPickListMode = normalized;
        } else if (target == Target.TWEAKEROO_TOOL_SWITCH_GUARD) {
            config.tweakerooToolSwitchListMode = normalized;
        }
    }

    private List<String> currentEntries() {
        if (target == Target.LITEMATICA_PICK_REDIRECT) return config.pickRedirectMap;
        if (target == Target.TWEAKERMORE_AUTO_PICK_GUARD) {
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

    private void setCurrentEntries(List<String> values) {
        ArrayList<String> copy = new ArrayList<>(values == null ? List.of() : values);
        if (target == Target.LITEMATICA_PICK_REDIRECT) {
            config.pickRedirectMap = copy;
        } else if (target == Target.TWEAKERMORE_AUTO_PICK_GUARD) {
            if (mode() == MasaIdListPolicy.WHITELIST) config.tweakermoreAutoPickWhitelist = copy;
            else if (mode() == MasaIdListPolicy.BLACKLIST) config.tweakermoreAutoPickBlacklist = copy;
        } else {
            if (mode() == MasaIdListPolicy.WHITELIST) config.tweakerooToolSwitchWhitelist = copy;
            else if (mode() == MasaIdListPolicy.BLACKLIST) config.tweakerooToolSwitchBlacklist = copy;
        }
    }

    private TargetKind firstTargetKind() {
        return target == Target.TWEAKERMORE_AUTO_PICK_GUARD ? TargetKind.ITEM : TargetKind.BLOCK;
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean validRegisteredId(String raw, TargetKind kind) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null) return false;
        if (kind == TargetKind.BLOCK) {
            for (Block block : BuiltInRegistries.BLOCK) {
                if (id.equals(BuiltInRegistries.BLOCK.getKey(block))) return true;
            }
            return false;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (id.equals(BuiltInRegistries.ITEM.getKey(item))) return true;
        }
        return false;
    }

    private void movePage(int delta) {
        page += delta;
        clampPage();
        refreshControls();
    }

    private void clampPage() {
        int size = currentEntries().size();
        int maxPage = size == 0 ? 0 : (size - 1) / pageSize;
        page = Mth.clamp(page, 0, maxPage);
    }

    private void refreshControls() {
        clampPage();
        if (modeButton != null) modeButton.setMessage(modeMessage());

        boolean editable = target == Target.LITEMATICA_PICK_REDIRECT || mode() != MasaIdListPolicy.NONE;
        if (firstBox != null) firstBox.active = editable;
        if (secondBox != null) secondBox.active = editable;
        boolean firstReady = firstBox != null && !normalize(firstBox.getValue()).isEmpty();
        boolean secondReady = secondBox == null || !normalize(secondBox.getValue()).isEmpty();
        if (addButton != null) {
            addButton.active = editable
                    && firstReady
                    && secondReady
                    && currentEntries().size() < MasaIntegrationConfig.MAX_LIST_ENTRIES;
        }
        if (clearButton != null) clearButton.active = editable && !currentEntries().isEmpty();

        int maxPage = currentEntries().isEmpty() ? 0 : (currentEntries().size() - 1) / pageSize;
        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page < maxPage;

        int first = page * pageSize;
        for (int slot = 0; slot < removeButtons.size(); slot++) {
            Button button = removeButtons.get(slot);
            button.visible = editable && first + slot < currentEntries().size();
            button.active = button.visible;
        }
    }

    private Component modeMessage() {
        String modeName = switch (mode()) {
            case MasaIdListPolicy.WHITELIST -> "ALLOW / Whitelist";
            case MasaIdListPolicy.BLACKLIST -> "DENY / Blacklist";
            default -> "Disabled";
        };
        return Component.literal("Guard mode: " + modeName);
    }

    private String titleText() {
        return switch (target) {
            case LITEMATICA_PICK_REDIRECT -> "Litematica Pick Redirect";
            case TWEAKERMORE_AUTO_PICK_GUARD -> "TweakerMore Auto Pick Guard";
            case TWEAKEROO_TOOL_SWITCH_GUARD -> "Tweakeroo Tool Switch Guard";
        };
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        int panelBottom = Math.max(listTop + ROW_HEIGHT * pageSize + 4, height - 36);
        extractor.fill(panelX, 34, panelX + panelWidth, panelBottom, 0xD0181818);
        extractor.fill(panelX, 34, panelX + panelWidth, 35, 0xFF6A6A6A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font, Component.literal(titleText()), width / 2, 10, 0xFFFFFFFF);
        if (target != Target.LITEMATICA_PICK_REDIRECT) {
            extractor.centeredText(
                    font,
                    Component.literal("外部MODの操作をChise policyで許可/拒否します。"),
                    width / 2, 24, 0xFFB8B8B8);
        }

        List<String> entries = currentEntries();
        int first = page * pageSize;
        int textRight = panelX + panelWidth - REMOVE_WIDTH - 18;
        for (int slot = 0; slot < pageSize; slot++) {
            int index = first + slot;
            if (index >= entries.size()) break;
            String entry = entries.get(index);
            int y = listTop + slot * ROW_HEIGHT;
            extractor.fill(panelX + 8, y - 2, panelX + panelWidth - 8, y + 20, 0x66303030);
            extractor.text(font, ellipsize(entry, Math.max(80, textRight - panelX - 18)),
                    panelX + 14, y + 5, 0xFFFFFFFF);
        }

        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        extractor.centeredText(
                font,
                Component.literal((page + 1) + " / " + (maxPage + 1) + "  (" + entries.size() + ")"),
                width / 2, height - 42, 0xFFAAAAAA);
        if (!feedback.isEmpty()) {
            extractor.centeredText(font, Component.literal(feedback), width / 2, 96, 0xFFFFD166);
        }
    }

    private String ellipsize(String value, int maxWidth) {
        if (value == null || value.isEmpty() || font.width(value) <= maxWidth) return value == null ? "" : value;
        String ellipsis = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + ellipsis) > maxWidth) end--;
        return value.substring(0, end) + ellipsis;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum TargetKind {
        BLOCK,
        ITEM
    }
}
