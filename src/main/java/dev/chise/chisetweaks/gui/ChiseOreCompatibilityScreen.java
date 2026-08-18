package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import dev.chise.chisetweaks.config.OreHighlightCompatibilityConfig;
import dev.chise.chisetweaks.feature.rendering.model.OreHighlightModelReload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Safe editor for explicit modded ore block IDs and Chise-owned visual styles. */
public final class ChiseOreCompatibilityScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int REMOVE_WIDTH = 68;

    private final Screen parent;
    private final boolean japanese;
    private final ArrayList<Button> removeButtons = new ArrayList<>();
    private EditBox idBox;
    private Button styleButton;
    private Button addButton;
    private Button clearButton;
    private Button previousButton;
    private Button nextButton;
    private OreHighlightStyle selectedStyle = OreHighlightStyle.GENERIC;
    private int panelX;
    private int panelWidth;
    private int listTop;
    private int pageSize;
    private int page;
    private String feedback = "";

    public ChiseOreCompatibilityScreen(Screen parent, boolean japanese) {
        super(Component.literal("Ore Compatibility"));
        this.parent = parent;
        this.japanese = japanese;
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(760, Math.max(336, width - 24));
        panelX = Math.max(12, (width - panelWidth) / 2);
        listTop = 112;
        pageSize = Math.max(2, Math.min(10, (Math.max(260, height) - listTop - 54) / ROW_HEIGHT));

        int styleWidth = Math.min(150, Math.max(110, panelWidth / 4));
        int addWidth = 76;
        int inputWidth = Math.max(110, panelWidth - styleWidth - addWidth - 32);
        idBox = addRenderableWidget(new EditBox(
                font, panelX + 8, 70, inputWidth, 20,
                Component.literal(japanese ? "MODブロックID" : "Mod block ID")));
        idBox.setHint(Component.literal("examplemod:copper_ore"));
        idBox.setResponder(ignored -> refreshControls());

        styleButton = addRenderableWidget(Button.builder(styleMessage(), ignored -> {
            selectedStyle = selectedStyle.next();
            styleButton.setMessage(styleMessage());
        }).bounds(panelX + 12 + inputWidth, 70, styleWidth, 20).build());

        addButton = addRenderableWidget(Button.builder(
                Component.literal(japanese ? "追加/更新" : "Add/Update"), ignored -> addEntry())
                .bounds(panelX + 16 + inputWidth + styleWidth, 70, addWidth, 20)
                .build());

        removeButtons.clear();
        for (int slot = 0; slot < pageSize; slot++) {
            final int visibleSlot = slot;
            removeButtons.add(addRenderableWidget(Button.builder(
                    Component.literal(japanese ? "削除" : "Remove"), ignored -> removeEntry(visibleSlot))
                    .bounds(panelX + panelWidth - REMOVE_WIDTH - 10,
                            listTop + slot * ROW_HEIGHT, REMOVE_WIDTH, 20)
                    .build()));
        }

        int footerY = height - 28;
        previousButton = addRenderableWidget(Button.builder(
                Component.literal(japanese ? "前へ" : "Previous"), ignored -> movePage(-1))
                .bounds(panelX + 8, footerY, 58, 20).build());
        nextButton = addRenderableWidget(Button.builder(
                Component.literal(japanese ? "次へ" : "Next"), ignored -> movePage(1))
                .bounds(panelX + 70, footerY, 58, 20).build());
        clearButton = addRenderableWidget(Button.builder(
                Component.literal(japanese ? "個別設定を空にする" : "Clear overrides"), ignored -> clearEntries())
                .bounds(panelX + 132, footerY, 122, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal(japanese ? "戻る" : "Back"), ignored -> onClose())
                .bounds(panelX + panelWidth - 88, footerY, 80, 20).build());
        refreshControls();
    }

    private void addEntry() {
        if (idBox == null) return;
        String raw = idBox.getValue() == null ? "" : idBox.getValue().trim().toLowerCase(Locale.ROOT);
        Identifier id = Identifier.tryParse(raw);
        if (id == null || "minecraft".equals(id.getNamespace())) {
            feedback = japanese ? "非バニラMODのBlock IDを入力してください。" : "Enter a non-vanilla mod block ID.";
            refreshControls();
            return;
        }
        if (!isRegisteredBlock(id)) {
            feedback = japanese ? "現在のクライアントに存在しないBlock IDです。" : "That block is not registered in this client.";
            refreshControls();
            return;
        }
        if (!OreHighlightCompatibilityConfig.put(id.toString(), selectedStyle)) {
            feedback = japanese
                    ? "登録できません。入力・件数上限・設定ファイルの保存先を確認してください。"
                    : "Could not save the override. Check the ID, entry limit, and config storage.";
            refreshControls();
            return;
        }
        OreHighlightModelReload.request();
        idBox.setValue("");
        page = Math.max(0, (entries().size() - 1) / pageSize);
        feedback = japanese ? "追加/更新しました。描画モデルを再構築します。" : "Added/updated. Rebuilding visual models.";
        refreshControls();
    }

    private boolean isRegisteredBlock(Identifier id) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (id.equals(BuiltInRegistries.BLOCK.getKey(block))) return true;
        }
        return false;
    }

    private void removeEntry(int visibleSlot) {
        int index = page * pageSize + visibleSlot;
        List<OreHighlightCompatibilityConfig.Entry> entries = entries();
        if (index < 0 || index >= entries.size()) return;
        if (OreHighlightCompatibilityConfig.remove(entries.get(index).blockId())) {
            OreHighlightModelReload.request();
            feedback = japanese ? "削除しました。描画モデルを再構築します。" : "Removed. Rebuilding visual models.";
        } else {
            feedback = japanese ? "削除内容を保存できませんでした。" : "Could not persist the removal.";
        }
        clampPage();
        refreshControls();
    }

    private void clearEntries() {
        if (entries().isEmpty()) return;
        if (!OreHighlightCompatibilityConfig.clear()) {
            feedback = japanese ? "設定を空にした内容を保存できませんでした。" : "Could not persist the cleared overrides.";
            refreshControls();
            return;
        }
        OreHighlightModelReload.request();
        page = 0;
        feedback = japanese ? "個別設定を空にしました。描画モデルを再構築します。" : "Overrides cleared. Rebuilding visual models.";
        refreshControls();
    }

    private void movePage(int delta) {
        page += delta;
        clampPage();
        refreshControls();
    }

    private void clampPage() {
        int size = entries().size();
        int maxPage = size == 0 ? 0 : (size - 1) / pageSize;
        page = Mth.clamp(page, 0, maxPage);
    }

    private void refreshControls() {
        clampPage();
        List<OreHighlightCompatibilityConfig.Entry> entries = entries();
        if (styleButton != null) styleButton.setMessage(styleMessage());
        if (addButton != null) {
            String value = idBox == null || idBox.getValue() == null ? "" : idBox.getValue().trim();
            addButton.active = canSubmitEntry(value, entries);
        }
        if (clearButton != null) clearButton.active = !entries.isEmpty();
        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page < maxPage;
        int first = page * pageSize;
        for (int slot = 0; slot < removeButtons.size(); slot++) {
            Button button = removeButtons.get(slot);
            button.visible = first + slot < entries.size();
            button.active = button.visible;
        }
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
        int size = entries == null ? 0 : entries.size();
        return size < OreHighlightCompatibilityConfig.MAX_ENTRIES;
    }

    private Component styleMessage() {
        return Component.literal((japanese ? "模様: " : "Style: ") + selectedStyle.key());
    }

    private static List<OreHighlightCompatibilityConfig.Entry> entries() {
        return OreHighlightCompatibilityConfig.entries();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        extractor.fill(panelX, 34, panelX + panelWidth, height - 34, 0xC8121212);
        List<OreHighlightCompatibilityConfig.Entry> entries = entries();
        int first = page * pageSize;
        for (int slot = 0; slot < pageSize; slot++) {
            int index = first + slot;
            if (index >= entries.size()) break;
            int y = listTop + slot * ROW_HEIGHT;
            extractor.fill(panelX + 8, y - 2, panelX + panelWidth - 8, y + 20, 0x66303030);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font,
                Component.literal(japanese ? "MOD鉱石の対象" : "Modded Ore Targets"),
                width / 2, 10, 0xFFFFFFFF);
        extractor.centeredText(font,
                Component.literal(japanese
                        ? "c:ores自動判定を補完する個別指定。元のMODテクスチャは変更しません。"
                        : "Explicit overrides complement c:ores detection; source mod textures remain unchanged."),
                width / 2, 24, 0xFFB8B8B8);

        List<OreHighlightCompatibilityConfig.Entry> entries = entries();
        int first = page * pageSize;
        int textRight = panelX + panelWidth - REMOVE_WIDTH - 18;
        for (int slot = 0; slot < pageSize; slot++) {
            int index = first + slot;
            if (index >= entries.size()) break;
            OreHighlightCompatibilityConfig.Entry entry = entries.get(index);
            String text = entry.blockId() + "  →  " + entry.style().key();
            extractor.text(font, ellipsize(text, Math.max(80, textRight - panelX - 18)),
                    panelX + 14, listTop + slot * ROW_HEIGHT + 5, 0xFFFFFFFF);
        }
        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        extractor.centeredText(font,
                Component.literal((page + 1) + " / " + (maxPage + 1) + "  (" + entries.size() + ")"),
                width / 2, height - 42, 0xFFAAAAAA);
        if (!feedback.isEmpty()) {
            extractor.centeredText(font, Component.literal(feedback), width / 2, 96, 0xFFFFD166);
        }
    }

    private String ellipsize(String value, int maxWidth) {
        if (value == null || value.isEmpty() || font.width(value) <= maxWidth) return value == null ? "" : value;
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + "…") > maxWidth) end--;
        return value.substring(0, end) + "…";
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
