package net.easecation.clientsettings.client;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.feature.hud.HudBounds;
import net.easecation.clientsettings.feature.hud.HudLayout;
import net.easecation.clientsettings.feature.hud.HudPosition;
import net.easecation.clientsettings.feature.hud.HudRenderer;
import net.easecation.clientsettings.feature.hud.HudSize;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.HudWidgetSettings;
import net.easecation.clientsettings.profile.runtime.ProfileManager;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudEditorScreen extends Screen {

    private static final int OVERLAY_COLOR = 0x58000000;
    private static final int NORMAL_OUTLINE_COLOR = 0x80FFFFFF;
    private static final int HOVERED_OUTLINE_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_OUTLINE_COLOR = 0xFFFFC247;
    private static final int GUIDE_COLOR = 0xFF41B9FF;
    private static final int PALETTE_BACKGROUND = 0xC0101216;
    private static final int PALETTE_OUTLINE = 0x805B7188;
    private static final int PALETTE_PADDING = 6;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 3;

    private final Screen parent;
    private final ProfileSettingsDraft draft;
    private final HudSettings initialHud;
    private final ProfileManager profiles;
    private final boolean standalone;
    private HudWidgetId selected;
    private boolean dragging;
    private boolean snapping = true;
    private double dragOffsetX;
    private double dragOffsetY;
    private Integer verticalGuide;
    private Integer horizontalGuide;
    private Component error;
    private Button styleButton;
    private Button toggleSelectedButton;
    private Button resetSelectedButton;
    private Button snappingButton;
    private int paletteX;
    private int paletteY;
    private int paletteWidth;
    private int paletteHeight;

    public static HudEditorScreen createStandalone(Screen parent) {
        ProfileManager profiles = ProfileServices.manager();
        return new HudEditorScreen(parent, ProfileSettingsDraft.active(profiles), profiles);
    }

    public HudEditorScreen(Screen parent, ProfileSettingsDraft draft) {
        this(parent, draft, null);
    }

    private HudEditorScreen(Screen parent, ProfileSettingsDraft draft, ProfileManager profiles) {
        super(Component.translatable("screen.ecclientsettings.hud_editor.title"));
        this.parent = parent;
        this.draft = draft;
        this.initialHud = draft.hudSettings();
        this.profiles = profiles;
        this.standalone = profiles != null;
        this.selected = initialSelection(initialHud);
    }

    @Override
    protected void init() {
        int buttonCount = standalone ? 8 : 6;
        paletteWidth = Math.min(174, Math.max(120, this.width - 24));
        paletteHeight = PALETTE_PADDING * 2
                + buttonCount * BUTTON_HEIGHT
                + (buttonCount - 1) * BUTTON_GAP;
        paletteX = (this.width - paletteWidth) / 2;
        int desiredY = Math.max(52, (this.height - paletteHeight) / 2);
        int maximumY = Math.max(4, this.height - paletteHeight - 4);
        paletteY = Math.max(4, Math.min(desiredY, maximumY));

        int y = paletteY + PALETTE_PADDING;
        if (standalone) {
            addPaletteButton(
                    Component.translatable("button.ecclientsettings.hud.all_settings"),
                    y,
                    button -> openAllSettings()
            );
            y += BUTTON_HEIGHT + BUTTON_GAP;
            toggleSelectedButton = addPaletteButton(
                    toggleSelectedMessage(),
                    y,
                    button -> toggleSelectedWidget()
            );
            y += BUTTON_HEIGHT + BUTTON_GAP;
        }
        styleButton = addPaletteButton(
                Component.translatable("button.ecclientsettings.hud.edit_style"),
                y,
                button -> openSelectedStyle()
        );
        y += BUTTON_HEIGHT + BUTTON_GAP;
        snappingButton = addPaletteButton(snappingMessage(), y, button -> toggleSnapping());
        y += BUTTON_HEIGHT + BUTTON_GAP;
        resetSelectedButton = addPaletteButton(
                Component.translatable("button.ecclientsettings.hud.reset_selected"),
                y,
                button -> resetSelectedLayout()
        );
        y += BUTTON_HEIGHT + BUTTON_GAP;
        addPaletteButton(
                Component.translatable("button.ecclientsettings.hud.reset_layout"),
                y,
                button -> resetLayout()
        );
        y += BUTTON_HEIGHT + BUTTON_GAP;
        addPaletteButton(CommonComponents.GUI_CANCEL, y, button -> cancelAndClose());
        y += BUTTON_HEIGHT + BUTTON_GAP;
        addPaletteButton(
                Component.translatable(standalone
                        ? "button.ecclientsettings.hud.save_exit"
                        : "button.ecclientsettings.hud.apply"),
                y,
                button -> applyAndClose()
        );
        updateSelectionActions();
    }

    private Button addPaletteButton(Component message, int y, Button.OnPress action) {
        Button button = Button.builder(message, action)
                .bounds(
                        paletteX + PALETTE_PADDING,
                        y,
                        paletteWidth - PALETTE_PADDING * 2,
                        BUTTON_HEIGHT
                )
                .build();
        return this.addRenderableWidget(button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, OVERLAY_COLOR);

        for (HudWidgetId id : HudWidgetId.values()) {
            HudWidgetSettings settings = draft.hudSettings().widget(id);
            HudBounds bounds = HudRenderer.renderPreview(graphics, id, draft.hudSettings());
            boolean hovered = bounds.contains(mouseX, mouseY);
            int outlineColor = id == selected
                    ? SELECTED_OUTLINE_COLOR
                    : hovered ? HOVERED_OUTLINE_COLOR : NORMAL_OUTLINE_COLOR;
            graphics.renderOutline(
                    bounds.x() - 1,
                    bounds.y() - 1,
                    bounds.width() + 2,
                    bounds.height() + 2,
                    outlineColor
            );
            drawWidgetLabel(graphics, id, settings, bounds);
        }

        if (dragging) {
            if (verticalGuide != null) {
                int guideX = Math.clamp(verticalGuide, 0, Math.max(0, this.width - 1));
                graphics.fill(guideX, 0, guideX + 1, this.height, GUIDE_COLOR);
            }
            if (horizontalGuide != null) {
                int guideY = Math.clamp(horizontalGuide, 0, Math.max(0, this.height - 1));
                graphics.fill(0, guideY, this.width, guideY + 1, GUIDE_COLOR);
            }
        }

        graphics.fill(
                paletteX,
                paletteY,
                paletteX + paletteWidth,
                paletteY + paletteHeight,
                PALETTE_BACKGROUND
        );
        graphics.renderOutline(paletteX, paletteY, paletteWidth, paletteHeight, PALETTE_OUTLINE);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 7, 0xFFFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.ecclientsettings.hud_editor.instructions"),
                this.width / 2,
                19,
                0xFFE5E5E5
        );
        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.ecclientsettings.hud_editor.instructions_secondary"),
                this.width / 2,
                30,
                0xFFBFC8D2
        );
        if (selected != null) {
            HudWidgetSettings settings = draft.hudSettings().widget(selected);
            HudBounds bounds = bounds(selected);
            double effectiveScale = HudLayout.fittedScale(
                    settings.scale(),
                    this.width,
                    this.height,
                    HudRenderer.previewSize(selected, draft.hudSettings())
            );
            boolean viewportLimited = effectiveScale + 1.0E-6 < settings.scale();
            Component selectedSummary = viewportLimited
                    ? Component.translatable(
                            "screen.ecclientsettings.hud_editor.selected_limited",
                            Math.round(effectiveScale * 100.0),
                            Math.round(settings.scale() * 100.0)
                    )
                    : Component.translatable(
                            "screen.ecclientsettings.hud_editor.selected",
                            Component.translatable(widgetTranslationKey(selected)),
                            bounds.x(),
                            bounds.y(),
                            Math.round(effectiveScale * 100.0)
                    );
            graphics.drawCenteredString(
                    this.font,
                    selectedSummary,
                    this.width / 2,
                    41,
                    SELECTED_OUTLINE_COLOR
            );
        }
        if (error != null) {
            graphics.drawCenteredString(this.font, error, this.width / 2, this.height - 13, 0xFFFF5555);
        }
    }

    private void drawWidgetLabel(
            GuiGraphics graphics,
            HudWidgetId id,
            HudWidgetSettings settings,
            HudBounds bounds
    ) {
        Component name = Component.translatable(widgetTranslationKey(id));
        Component state = Component.translatable(settings.enabled()
                ? "screen.ecclientsettings.hud_editor.enabled"
                : "screen.ecclientsettings.hud_editor.disabled");
        Component label = Component.translatable(
                "screen.ecclientsettings.hud_editor.widget_label",
                name,
                state,
                String.format(Locale.ROOT, "%.1fx", settings.scale())
        );
        int labelY = bounds.y() >= 54 ? bounds.y() - 11 : bounds.bottom() + 2;
        int labelX = Math.clamp(bounds.x(), 2, Math.max(2, this.width - this.font.width(label) - 2));
        graphics.drawString(this.font, label, labelX, labelY, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (isInsidePalette(mouseX, mouseY)) {
            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (!handled) {
                this.setFocused(null);
            }
            return true;
        }

        HudWidgetId target = widgetAt(mouseX, mouseY);
        if (target != null) {
            select(target);
            HudBounds bounds = bounds(target);
            dragging = true;
            dragOffsetX = mouseX - bounds.x();
            dragOffsetY = mouseY - bounds.y();
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        this.setFocused(null);
        clearDragState();
        return false;
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY
    ) {
        if (!dragging || selected == null || button != 0) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        HudWidgetSettings settings = draft.hudSettings().widget(selected);
        HudBounds currentBounds = bounds(selected);
        int proposedX = (int) Math.round(mouseX - dragOffsetX);
        int proposedY = (int) Math.round(mouseY - dragOffsetY);
        HudSnapEngine.SnapResult snapped;
        if (snapping && !Screen.hasAltDown()) {
            snapped = HudSnapEngine.snap(
                    proposedX,
                    proposedY,
                    currentBounds.width(),
                    currentBounds.height(),
                    this.width,
                    this.height,
                    otherWidgetBounds(selected)
            );
        } else {
            snapped = new HudSnapEngine.SnapResult(proposedX, proposedY, null, null);
        }
        HudSize previewSize = HudRenderer.previewSize(selected, draft.hudSettings());
        HudPosition normalized = HudLayout.normalizePosition(
                snapped.x(),
                snapped.y(),
                this.width,
                this.height,
                previewSize,
                settings.scale()
        );
        draft.setHudWidget(selected, new HudWidgetSettings(
                settings.enabled(),
                normalized.normalizedX(),
                normalized.normalizedY(),
                settings.scale(),
                settings.style()
        ));
        verticalGuide = snapped.verticalGuide();
        horizontalGuide = snapped.horizontalGuide();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            clearDragState();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (isInsidePalette(mouseX, mouseY)) {
            super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        if (scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        HudWidgetId target = widgetAt(mouseX, mouseY);
        if (target == null) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        select(target);
        clearGuides();
        scaleWidget(target, scrollY, mouseX, mouseY);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selected != null && this.getFocused() == null) {
            int distance = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> {
                    moveSelected(-distance, 0);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    moveSelected(distance, 0);
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    moveSelected(0, -distance);
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    moveSelected(0, distance);
                    return true;
                }
                case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                    scaleWidget(selected, 1.0, null, null);
                    return true;
                }
                case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                    scaleWidget(selected, -1.0, null, null);
                    return true;
                }
                default -> {
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        cancelAndClose();
    }

    @Override
    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }

    private void moveSelected(int deltaX, int deltaY) {
        HudWidgetSettings settings = draft.hudSettings().widget(selected);
        HudBounds current = bounds(selected);
        HudSize previewSize = HudRenderer.previewSize(selected, draft.hudSettings());
        HudPosition normalized = HudLayout.normalizePosition(
                current.x() + deltaX,
                current.y() + deltaY,
                this.width,
                this.height,
                previewSize,
                settings.scale()
        );
        draft.setHudWidget(selected, new HudWidgetSettings(
                settings.enabled(),
                normalized.normalizedX(),
                normalized.normalizedY(),
                settings.scale(),
                settings.style()
        ));
    }

    private void scaleWidget(HudWidgetId target, double direction, Double anchorX, Double anchorY) {
        HudWidgetSettings settings = draft.hudSettings().widget(target);
        double scale = HudEditorInteraction.scaleAfterScroll(settings.scale(), direction);
        if (scale == settings.scale()) {
            return;
        }

        HudBounds oldBounds = bounds(target);
        HudSize previewSize = HudRenderer.previewSize(target, draft.hudSettings());
        HudWidgetSettings scaledAtOrigin = new HudWidgetSettings(
                settings.enabled(), 0.0, 0.0, scale, settings.style()
        );
        HudBounds scaledBounds = HudLayout.bounds(
                scaledAtOrigin,
                this.width,
                this.height,
                previewSize
        );
        int newX;
        int newY;
        if (anchorX != null && anchorY != null && oldBounds.width() > 0 && oldBounds.height() > 0) {
            double relativeX = (anchorX - oldBounds.x()) / oldBounds.width();
            double relativeY = (anchorY - oldBounds.y()) / oldBounds.height();
            newX = (int) Math.round(anchorX - relativeX * scaledBounds.width());
            newY = (int) Math.round(anchorY - relativeY * scaledBounds.height());
        } else {
            HudEditorInteraction.Point centered = HudEditorInteraction.centeredOrigin(
                    oldBounds.x(),
                    oldBounds.y(),
                    oldBounds.width(),
                    oldBounds.height(),
                    scaledBounds.width(),
                    scaledBounds.height()
            );
            newX = centered.x();
            newY = centered.y();
        }
        HudPosition normalized = HudLayout.normalizePosition(
                newX,
                newY,
                this.width,
                this.height,
                previewSize,
                scale
        );
        draft.setHudWidget(target, new HudWidgetSettings(
                settings.enabled(),
                normalized.normalizedX(),
                normalized.normalizedY(),
                scale,
                settings.style()
        ));
    }

    private HudWidgetId widgetAt(double mouseX, double mouseY) {
        if (isInsidePalette(mouseX, mouseY)) {
            return null;
        }
        HudWidgetId[] ids = HudWidgetId.values();
        for (int index = ids.length - 1; index >= 0; index--) {
            if (bounds(ids[index]).contains(mouseX, mouseY)) {
                return ids[index];
            }
        }
        return null;
    }

    private boolean isInsidePalette(double mouseX, double mouseY) {
        return mouseX >= paletteX
                && mouseX < paletteX + paletteWidth
                && mouseY >= paletteY
                && mouseY < paletteY + paletteHeight;
    }

    private HudBounds bounds(HudWidgetId id) {
        return HudRenderer.previewBounds(
                id,
                draft.hudSettings(),
                this.width,
                this.height
        );
    }

    private List<HudSnapEngine.Rect> otherWidgetBounds(HudWidgetId excluded) {
        List<HudSnapEngine.Rect> bounds = new ArrayList<>();
        for (HudWidgetId id : HudWidgetId.values()) {
            if (id == excluded) {
                continue;
            }
            HudBounds widget = bounds(id);
            bounds.add(new HudSnapEngine.Rect(
                    widget.x(),
                    widget.y(),
                    widget.width(),
                    widget.height()
            ));
        }
        return bounds;
    }

    private void select(HudWidgetId id) {
        selected = id;
        this.setFocused(null);
        updateSelectionActions();
    }

    private void updateSelectionActions() {
        boolean active = selected != null;
        if (styleButton != null) {
            styleButton.active = active;
        }
        if (toggleSelectedButton != null) {
            toggleSelectedButton.active = active;
            toggleSelectedButton.setMessage(toggleSelectedMessage());
        }
        if (resetSelectedButton != null) {
            resetSelectedButton.active = active;
        }
    }

    private void toggleSnapping() {
        snapping = !snapping;
        snappingButton.setMessage(snappingMessage());
        clearGuides();
    }

    private Component snappingMessage() {
        return Component.translatable(snapping
                ? "button.ecclientsettings.hud.snapping_on"
                : "button.ecclientsettings.hud.snapping_off");
    }

    private Component toggleSelectedMessage() {
        if (selected == null) {
            return Component.translatable("button.ecclientsettings.hud.toggle_selected");
        }
        return Component.translatable(draft.hudSettings().widget(selected).enabled()
                ? "button.ecclientsettings.hud.disable_selected"
                : "button.ecclientsettings.hud.enable_selected");
    }

    private void toggleSelectedWidget() {
        if (selected != null) {
            HudWidgetSettings settings = draft.hudSettings().widget(selected);
            draft.setHudEnabled(selected, !settings.enabled());
            toggleSelectedButton.setMessage(toggleSelectedMessage());
        }
    }

    private void openSelectedStyle() {
        if (selected != null) {
            this.minecraft.setScreen(HudStyleScreen.create(this, draft, selected));
        }
    }

    private void openAllSettings() {
        if (standalone) {
            clearDragState();
            this.minecraft.setScreen(ClientSettingsScreen.create(parent, draft));
        }
    }

    private void resetSelectedLayout() {
        if (selected != null) {
            draft.resetHudLayout(selected);
            clearDragState();
        }
    }

    private void resetLayout() {
        draft.resetHudLayout();
        clearDragState();
    }

    private void applyAndClose() {
        clearDragState();
        if (!standalone) {
            this.minecraft.setScreen(parent);
            return;
        }
        try {
            draft.save(profiles);
            this.minecraft.setScreen(parent);
        } catch (IOException | RuntimeException exception) {
            ECClientSettings.LOGGER.error("Could not save HUD editor changes", exception);
            error = Component.translatable("message.ecclientsettings.hud_editor.save_failed");
        }
    }

    private void cancelAndClose() {
        clearDragState();
        if (!standalone) {
            draft.restoreHudSettings(initialHud);
        }
        this.minecraft.setScreen(parent);
    }

    private void clearDragState() {
        dragging = false;
        clearGuides();
    }

    private void clearGuides() {
        verticalGuide = null;
        horizontalGuide = null;
    }

    private static String widgetTranslationKey(HudWidgetId id) {
        return "option.ecclientsettings.hud." + id.serializedName() + ".enabled";
    }

    private static HudWidgetId initialSelection(HudSettings settings) {
        for (HudWidgetId id : HudWidgetId.values()) {
            if (settings.widget(id).enabled()) {
                return id;
            }
        }
        return HudWidgetId.FPS;
    }
}
