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

public final class ChiseOreCompatibilityScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
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
    private int removeWidth;
    private boolean compactLayout;
    private int page;
    private String feedback = "";

    public ChiseOreCompatibilityScreen(Screen parent) {
        super(Component.translatable("screen.chisetweaks.ore_compat.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        ChiseOreCompatibilityLayout.Geometry layout =
                ChiseOreCompatibilityLayout.calculate(width, height);
        panelX = layout.panel().x();
        panelWidth = layout.panel().width();
        listTop = layout.listTop();
        pageSize = layout.pageSize();
        removeWidth = layout.removeWidth();
        compactLayout = layout.compact();

        var idRect = layout.idInput();
        idBox = addRenderableWidget(new EditBox(
                font, idRect.x(), idRect.y(), idRect.width(), idRect.height(),
                Component.translatable("screen.chisetweaks.ore_compat.block_id")));
        idBox.setHint(Component.literal("examplemod:copper_ore"));
        idBox.setResponder(ignored -> refreshControls());

        var styleRect = layout.style();
        styleButton = addRenderableWidget(Button.builder(styleMessage(), ignored -> {
            selectedStyle = selectedStyle.next();
            styleButton.setMessage(styleMessage());
        }).bounds(styleRect.x(), styleRect.y(), styleRect.width(), styleRect.height()).build());

        var addRect = layout.add();
        addButton = addRenderableWidget(Button.builder(
                Component.translatable(compactLayout
                        ? "screen.chisetweaks.ore_compat.add"
                        : "screen.chisetweaks.ore_compat.add_update"),
                ignored -> addEntry())
                .bounds(addRect.x(), addRect.y(), addRect.width(), addRect.height())
                .build());

        removeButtons.clear();
        for (int slot = 0; slot < pageSize; slot++) {
            final int visibleSlot = slot;
            removeButtons.add(addRenderableWidget(Button.builder(
                    Component.translatable("screen.chisetweaks.ore_compat.remove"), ignored -> removeEntry(visibleSlot))
                    .bounds(panelX + panelWidth - removeWidth - 10,
                            listTop + slot * ROW_HEIGHT, removeWidth, 20)
                    .build()));
        }

        var previous = layout.previous();
        previousButton = addRenderableWidget(Button.builder(
                compactLayout
                        ? Component.literal("‹")
                        : Component.translatable("screen.chisetweaks.ore_compat.previous"),
                ignored -> movePage(-1))
                .bounds(previous.x(), previous.y(), previous.width(), previous.height()).build());
        var next = layout.next();
        nextButton = addRenderableWidget(Button.builder(
                compactLayout
                        ? Component.literal("›")
                        : Component.translatable("screen.chisetweaks.ore_compat.next"),
                ignored -> movePage(1))
                .bounds(next.x(), next.y(), next.width(), next.height()).build());
        var clear = layout.clear();
        clearButton = addRenderableWidget(Button.builder(
                Component.translatable(compactLayout
                        ? "screen.chisetweaks.ore_compat.clear"
                        : "screen.chisetweaks.ore_compat.clear_overrides"),
                ignored -> clearEntries())
                .bounds(clear.x(), clear.y(), clear.width(), clear.height()).build());
        var back = layout.back();
        addRenderableWidget(Button.builder(
                Component.translatable("screen.chisetweaks.common.back"), ignored -> onClose())
                .bounds(back.x(), back.y(), back.width(), back.height()).build());
        refreshControls();
    }

    private void addEntry() {
        if (idBox == null) return;
        String raw = idBox.getValue() == null ? "" : idBox.getValue().trim().toLowerCase(Locale.ROOT);
        Identifier id = Identifier.tryParse(raw);
        if (id == null || "minecraft".equals(id.getNamespace())) {
            feedback = text("screen.chisetweaks.ore_compat.feedback.invalid");
            refreshControls();
            return;
        }
        if (!isRegisteredBlock(id)) {
            feedback = text("screen.chisetweaks.ore_compat.feedback.unregistered");
            refreshControls();
            return;
        }
        if (!OreHighlightCompatibilityConfig.put(id.toString(), selectedStyle)) {
            feedback = text("screen.chisetweaks.ore_compat.feedback.save_failed");
            refreshControls();
            return;
        }
        OreHighlightModelReload.request();
        idBox.setValue("");
        page = Math.max(0, (entries().size() - 1) / pageSize);
        feedback = text("screen.chisetweaks.ore_compat.feedback.saved");
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
            feedback = text("screen.chisetweaks.ore_compat.feedback.removed");
        } else {
            feedback = text("screen.chisetweaks.ore_compat.feedback.remove_failed");
        }
        clampPage();
        refreshControls();
    }

    private void clearEntries() {
        if (entries().isEmpty()) return;
        if (!OreHighlightCompatibilityConfig.clear()) {
            feedback = text("screen.chisetweaks.ore_compat.feedback.clear_failed");
            refreshControls();
            return;
        }
        OreHighlightModelReload.request();
        page = 0;
        feedback = text("screen.chisetweaks.ore_compat.feedback.cleared");
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
        return Component.translatable("screen.chisetweaks.ore_compat.style", selectedStyle.key());
    }

    private static List<OreHighlightCompatibilityConfig.Entry> entries() {
        return OreHighlightCompatibilityConfig.entries();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);
        extractor.fill(panelX, 34, panelX + panelWidth, Math.max(35, height - 34), 0xC8121212);
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
                Component.translatable("screen.chisetweaks.ore_compat.title"),
                width / 2, 10, 0xFFFFFFFF);
        extractor.centeredText(font,
                Component.translatable("screen.chisetweaks.ore_compat.subtitle"),
                width / 2, 24, 0xFFB8B8B8);

        List<OreHighlightCompatibilityConfig.Entry> entries = entries();
        int first = page * pageSize;
        int textRight = panelX + panelWidth - removeWidth - 18;
        for (int slot = 0; slot < pageSize; slot++) {
            int index = first + slot;
            if (index >= entries.size()) break;
            OreHighlightCompatibilityConfig.Entry entry = entries.get(index);
            String text = entry.blockId() + "  →  " + entry.style().key();
            extractor.text(font, ellipsize(text, Math.max(40, textRight - panelX - 18)),
                    panelX + 14, listTop + slot * ROW_HEIGHT + 5, 0xFFFFFFFF);
        }
        int maxPage = entries.isEmpty() ? 0 : (entries.size() - 1) / pageSize;
        extractor.centeredText(font,
                Component.literal((page + 1) + " / " + (maxPage + 1) + "  (" + entries.size() + ")"),
                width / 2, Math.max(0, height - 42), 0xFFAAAAAA);
        if (!feedback.isEmpty()) {
            extractor.centeredText(font, Component.literal(feedback), width / 2,
                    compactLayout ? 104 : 96, 0xFFFFD166);
        }
    }

    private String ellipsize(String value, int maxWidth) {
        if (value == null || value.isEmpty() || font.width(value) <= maxWidth) return value == null ? "" : value;
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + "…") > maxWidth) end--;
        return value.substring(0, end) + "…";
    }

    private static String text(String key) {
        return Component.translatable(key).getString();
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
