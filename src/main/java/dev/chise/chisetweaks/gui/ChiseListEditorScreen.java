package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.api.ore.OreHighlightStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;

/**
 * Shared widget shell for bounded user-owned ID lists.
 * Validation, persistence and mode transitions live in ChiseListEditorBackend.
 */
public final class ChiseListEditorScreen extends Screen {
    private static final int OUTER_MARGIN = 12;
    private static final int ROW_HEIGHT = 24;
    private static final int STANDARD_REMOVE_WIDTH = 68;

    public enum Target {
        BLOCK_FILTER,
        ENTITY_FILTER,
        ORE_COMPATIBILITY,
        LITEMATICA_PICK_REDIRECT,
        TWEAKERMORE_AUTO_PICK_GUARD,
        TWEAKEROO_TOOL_SWITCH_GUARD
    }

    private final Screen parent;
    private final ChiseListEditorBackend backend;
    private final ArrayList<Button> removeButtons = new ArrayList<>();

    private EditBox firstBox;
    private EditBox secondBox;
    private Button modeButton;
    private Button styleButton;
    private Button addButton;
    private Button clearButton;
    private Button previousButton;
    private Button nextButton;
    private OreHighlightStyle selectedStyle = OreHighlightStyle.GENERIC;
    private ChiseOreCompatibilityLayout.Geometry oreLayout;
    private int panelX;
    private int panelWidth;
    private int listTop;
    private int pageSize;
    private int removeWidth = STANDARD_REMOVE_WIDTH;
    private boolean compactLayout;
    private int page;
    private String feedback = "";

    public ChiseListEditorScreen(Screen parent, Target target) {
        this(parent, ChiseListEditorBackend.create(target));
    }

    private ChiseListEditorScreen(Screen parent, ChiseListEditorBackend backend) {
        super(backend.title());
        this.parent = parent;
        this.backend = backend;
    }

    @Override
    protected void init() {
        super.init();
        if (backend.usesOreLayout()) initOreLayout();
        else initStandardLayout();
        createRemoveButtons();
        createFooter();
        refreshControls();
    }

    private void initStandardLayout() {
        panelWidth = Math.min(760, Math.max(336, width - OUTER_MARGIN * 2));
        panelX = Math.max(OUTER_MARGIN, (width - panelWidth) / 2);
        listTop = backend.standardListTop();
        pageSize = Math.max(2, Math.min(10, (Math.max(260, height) - listTop - 54) / ROW_HEIGHT));
        removeWidth = STANDARD_REMOVE_WIDTH;
        compactLayout = false;

        if (backend.hasMode()) {
            modeButton = addRenderableWidget(Button.builder(
                    backend.modeMessage(), ignored -> applyMutation(backend.cycleMode()))
                    .bounds(
                            panelX + 8,
                            42,
                            Math.min(backend.preferredModeWidth(), panelWidth - 16),
                            20)
                    .build());
        }
        createStandardInputs();
    }

    private void createStandardInputs() {
        int addWidth = 74;
        if (backend.hasSecondInput()) {
            int gap = 6;
            int usable = Math.max(160, panelWidth - addWidth - gap * 3 - 16);
            int fieldWidth = usable / 2;
            firstBox = addRenderableWidget(new EditBox(
                    font, panelX + 8, 70, fieldWidth, 20, backend.firstInputLabel()));
            secondBox = addRenderableWidget(new EditBox(
                    font,
                    panelX + 8 + fieldWidth + gap,
                    70,
                    fieldWidth,
                    20,
                    backend.secondInputLabel()));
            firstBox.setHint(Component.literal(backend.firstInputHint()));
            secondBox.setHint(Component.literal(backend.secondInputHint()));

            int addX = panelX + panelWidth - addWidth - 8;
            firstBox.setWidth(Math.max(70, addX - gap - firstBox.getX() - fieldWidth - gap));
            secondBox.setX(firstBox.getX() + firstBox.getWidth() + gap);
            secondBox.setWidth(Math.max(70, addX - gap - secondBox.getX()));
            addButton = addRenderableWidget(Button.builder(
                    backend.addLabel(false), ignored -> addEntry())
                    .bounds(addX, 70, addWidth, 20)
                    .build());
        } else {
            int inputWidth = Math.max(120, panelWidth - addWidth - 26);
            firstBox = addRenderableWidget(new EditBox(
                    font, panelX + 8, 70, inputWidth, 20, backend.firstInputLabel()));
            firstBox.setHint(Component.literal(backend.firstInputHint()));
            addButton = addRenderableWidget(Button.builder(
                    backend.addLabel(false), ignored -> addEntry())
                    .bounds(panelX + 16 + inputWidth, 70, addWidth, 20)
                    .build());
        }

        firstBox.setResponder(ignored -> refreshControls());
        if (secondBox != null) secondBox.setResponder(ignored -> refreshControls());
    }

    private void initOreLayout() {
        oreLayout = ChiseOreCompatibilityLayout.calculate(width, height);
        panelX = oreLayout.panel().x();
        panelWidth = oreLayout.panel().width();
        listTop = oreLayout.listTop();
        pageSize = oreLayout.pageSize();
        removeWidth = oreLayout.removeWidth();
        compactLayout = oreLayout.compact();

        var idRect = oreLayout.idInput();
        firstBox = addRenderableWidget(new EditBox(
                font,
                idRect.x(),
                idRect.y(),
                idRect.width(),
                idRect.height(),
                backend.firstInputLabel()));
        firstBox.setHint(Component.literal(backend.firstInputHint()));
        firstBox.setResponder(ignored -> refreshControls());

        var styleRect = oreLayout.style();
        styleButton = addRenderableWidget(Button.builder(styleMessage(), ignored -> {
            selectedStyle = selectedStyle.next();
            styleButton.setMessage(styleMessage());
        }).bounds(styleRect.x(), styleRect.y(), styleRect.width(), styleRect.height()).build());

        var addRect = oreLayout.add();
        addButton = addRenderableWidget(Button.builder(
                backend.addLabel(compactLayout), ignored -> addEntry())
                .bounds(addRect.x(), addRect.y(), addRect.width(), addRect.height())
                .build());
    }

    private void createRemoveButtons() {
        removeButtons.clear();
        for (int slot = 0; slot < pageSize; slot++) {
            final int visibleSlot = slot;
            removeButtons.add(addRenderableWidget(Button.builder(
                    backend.removeLabel(), ignored -> removeEntry(visibleSlot))
                    .bounds(
                            panelX + panelWidth - removeWidth - 10,
                            listTop + slot * ROW_HEIGHT,
                            removeWidth,
                            20)
                    .build()));
        }
    }

    private void createFooter() {
        if (backend.usesOreLayout()) {
            previousButton = footerButton(oreLayout.previous(), backend.previousLabel(compactLayout), -1);
            nextButton = footerButton(oreLayout.next(), backend.nextLabel(compactLayout), 1);

            var clear = oreLayout.clear();
            clearButton = addRenderableWidget(Button.builder(
                    backend.clearLabel(compactLayout), ignored -> clearEntries())
                    .bounds(clear.x(), clear.y(), clear.width(), clear.height())
                    .build());

            var back = oreLayout.back();
            addRenderableWidget(Button.builder(backend.backLabel(), ignored -> onClose())
                    .bounds(back.x(), back.y(), back.width(), back.height())
                    .build());
            return;
        }

        int footerY = height - 28;
        previousButton = addRenderableWidget(Button.builder(
                backend.previousLabel(false), ignored -> movePage(-1))
                .bounds(panelX + 8, footerY, 58, 20)
                .build());
        nextButton = addRenderableWidget(Button.builder(
                backend.nextLabel(false), ignored -> movePage(1))
                .bounds(panelX + 70, footerY, 58, 20)
                .build());
        clearButton = addRenderableWidget(Button.builder(
                backend.clearLabel(false), ignored -> clearEntries())
                .bounds(panelX + 132, footerY, backend.standardClearWidth(), 20)
                .build());
        addRenderableWidget(Button.builder(backend.backLabel(), ignored -> onClose())
                .bounds(panelX + panelWidth - 88, footerY, 80, 20)
                .build());
    }

    private Button footerButton(
            ChiseOreCompatibilityLayout.Rect rect,
            Component message,
            int delta) {
        return addRenderableWidget(Button.builder(message, ignored -> movePage(delta))
                .bounds(rect.x(), rect.y(), rect.width(), rect.height())
                .build());
    }

    private void addEntry() {
        applyMutation(backend.add(firstValue(), secondValue(), selectedStyle));
    }

    private void removeEntry(int visibleSlot) {
        int index = page * pageSize + visibleSlot;
        if (index < 0 || index >= backend.entryCount()) return;
        applyMutation(backend.remove(index));
    }

    private void clearEntries() {
        if (backend.entryCount() == 0) return;
        applyMutation(backend.clear());
    }

    private void applyMutation(ChiseListEditorBackend.Mutation mutation) {
        if (mutation == null) return;
        feedback = mutation.feedback();

        if (mutation.clearInputs()) {
            if (firstBox != null) firstBox.setValue("");
            if (secondBox != null) secondBox.setValue("");
        }
        if (mutation.resetPage()) page = 0;
        if (mutation.moveToLastPage()) {
            page = Math.max(0, (backend.entryCount() - 1) / Math.max(1, pageSize));
        }

        clampPage();
        refreshControls();
    }

    private void movePage(int delta) {
        page += delta;
        clampPage();
        refreshControls();
    }

    private void clampPage() {
        int size = backend.entryCount();
        int maxPage = size == 0 ? 0 : (size - 1) / pageSize;
        page = Mth.clamp(page, 0, maxPage);
    }

    private void refreshControls() {
        clampPage();
        if (modeButton != null) modeButton.setMessage(backend.modeMessage());
        if (styleButton != null) styleButton.setMessage(styleMessage());

        boolean editable = backend.editable();
        if (firstBox != null) firstBox.active = editable;
        if (secondBox != null) secondBox.active = editable;
        if (addButton != null) addButton.active = backend.canSubmit(firstValue(), secondValue());
        if (clearButton != null) clearButton.active = editable && backend.entryCount() != 0;

        int maxPage = backend.entryCount() == 0 ? 0 : (backend.entryCount() - 1) / pageSize;
        if (previousButton != null) previousButton.active = page > 0;
        if (nextButton != null) nextButton.active = page < maxPage;

        int first = page * pageSize;
        for (int slot = 0; slot < removeButtons.size(); slot++) {
            Button button = removeButtons.get(slot);
            button.visible = editable && first + slot < backend.entryCount();
            button.active = button.visible;
        }
    }

    private String firstValue() {
        return firstBox == null ? "" : firstBox.getValue();
    }

    private String secondValue() {
        return secondBox == null ? "" : secondBox.getValue();
    }

    private Component styleMessage() {
        return Component.translatable("screen.chisetweaks.ore_compat.style", selectedStyle.key());
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor extractor,
            int mouseX,
            int mouseY,
            float delta) {
        super.extractBackground(extractor, mouseX, mouseY, delta);

        if (backend.usesOreLayout()) {
            extractor.fill(panelX, 34, panelX + panelWidth, Math.max(35, height - 34), 0xC8121212);
            int first = page * pageSize;
            for (int slot = 0; slot < pageSize && first + slot < backend.entryCount(); slot++) {
                int y = listTop + slot * ROW_HEIGHT;
                extractor.fill(panelX + 8, y - 2, panelX + panelWidth - 8, y + 20, 0x66303030);
            }
            return;
        }

        int panelBottom = Math.max(listTop + ROW_HEIGHT * pageSize + 4, height - 36);
        extractor.fill(panelX, 34, panelX + panelWidth, panelBottom, 0xD0181818);
        extractor.fill(panelX, 34, panelX + panelWidth, 35, 0xFF6A6A6A);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor extractor,
            int mouseX,
            int mouseY,
            float delta) {
        super.extractRenderState(extractor, mouseX, mouseY, delta);
        extractor.centeredText(font, backend.title(), width / 2, 10, 0xFFFFFFFF);

        String subtitle = backend.subtitle();
        if (!subtitle.isEmpty()) {
            extractor.centeredText(
                    font,
                    Component.literal(subtitle),
                    width / 2,
                    24,
                    0xFFB8B8B8);
        }

        int first = page * pageSize;
        int textRight = panelX + panelWidth - removeWidth - 18;
        for (int slot = 0; slot < pageSize; slot++) {
            int index = first + slot;
            if (index >= backend.entryCount()) break;
            int y = listTop + slot * ROW_HEIGHT;
            if (!backend.usesOreLayout()) {
                extractor.fill(panelX + 8, y - 2, panelX + panelWidth - 8, y + 20, 0x66303030);
            }
            extractor.text(
                    font,
                    ellipsize(
                            backend.entryText(index),
                            Math.max(backend.usesOreLayout() ? 40 : 80, textRight - panelX - 18)),
                    panelX + 14,
                    y + 5,
                    0xFFFFFFFF);
        }

        int count = backend.entryCount();
        int maxPage = count == 0 ? 0 : (count - 1) / pageSize;
        extractor.centeredText(
                font,
                Component.literal((page + 1) + " / " + (maxPage + 1) + "  (" + count + ")"),
                width / 2,
                Math.max(0, height - 42),
                0xFFAAAAAA);

        if (!feedback.isEmpty()) {
            extractor.centeredText(
                    font,
                    Component.literal(feedback),
                    width / 2,
                    backend.usesOreLayout() && compactLayout ? 104 : 96,
                    0xFFFFD166);
        }
    }

    private String ellipsize(String value, int maxWidth) {
        if (value == null || value.isEmpty() || font.width(value) <= maxWidth) {
            return value == null ? "" : value;
        }
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
