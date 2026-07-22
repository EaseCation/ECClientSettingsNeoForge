package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.easecation.clientsettings.feature.hud.HudRenderer;
import net.easecation.clientsettings.feature.hud.HudSize;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.easecation.clientsettings.profile.model.KeystrokesSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("deprecation")
final class KeystrokesStylePreviewEntry extends TextListEntry {

    private static final int HEIGHT = 118;
    private static final int CANVAS_TOP = 14;
    private static final int CANVAS_HEIGHT = 100;
    private static final int CHECKER_SIZE = 8;

    private final HudSettings baseSettings;
    private final Supplier<HudWidgetStyle> styleSupplier;
    private final Supplier<KeystrokesSettings> keystrokesSupplier;

    KeystrokesStylePreviewEntry(
            HudSettings baseSettings,
            Supplier<HudWidgetStyle> styleSupplier,
            Supplier<KeystrokesSettings> keystrokesSupplier
    ) {
        super(Component.empty(), Component.empty());
        this.baseSettings = Objects.requireNonNull(baseSettings, "base HUD settings");
        this.styleSupplier = Objects.requireNonNull(styleSupplier, "style supplier");
        this.keystrokesSupplier = Objects.requireNonNull(keystrokesSupplier, "keystrokes supplier");
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int index,
            int y,
            int x,
            int entryWidth,
            int entryHeight,
            int mouseX,
            int mouseY,
            boolean hovered,
            float partialTick
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        graphics.drawString(
                font,
                Component.translatable("option.ecclientsettings.hud_style.preview"),
                x + 4,
                y + 2,
                getPreferredTextColor()
        );

        int canvasLeft = x + 4;
        int canvasRight = x + entryWidth - 4;
        int canvasBottom = y + CANVAS_TOP + CANVAS_HEIGHT;
        for (int tileY = y + CANVAS_TOP; tileY < canvasBottom; tileY += CHECKER_SIZE) {
            for (int tileX = canvasLeft; tileX < canvasRight; tileX += CHECKER_SIZE) {
                boolean alternate = ((tileX - canvasLeft) / CHECKER_SIZE
                        + (tileY - y - CANVAS_TOP) / CHECKER_SIZE) % 2 == 0;
                graphics.fill(
                        tileX,
                        tileY,
                        Math.min(tileX + CHECKER_SIZE, canvasRight),
                        Math.min(tileY + CHECKER_SIZE, canvasBottom),
                        alternate ? 0xFF242A31 : 0xFF343C45
                );
            }
        }

        HudSettings previewSettings;
        try {
            previewSettings = baseSettings
                    .withStyle(HudWidgetId.KEYSTROKES, styleSupplier.get())
                    .withKeystrokes(keystrokesSupplier.get());
        } catch (RuntimeException exception) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("option.ecclientsettings.hud_style.preview.invalid"),
                    x + entryWidth / 2,
                    y + CANVAS_TOP + 44,
                    0xFFFF7777
            );
            return;
        }

        HudSize size = HudRenderer.previewSize(HudWidgetId.KEYSTROKES, previewSettings);
        int availableWidth = Math.max(1, canvasRight - canvasLeft - 8);
        int availableHeight = CANVAS_HEIGHT - 8;
        double scale = Math.min(1.0, Math.min(
                availableWidth / (double) size.width(),
                availableHeight / (double) size.height()
        ));
        int renderedWidth = (int) Math.ceil(size.width() * scale);
        int renderedHeight = (int) Math.ceil(size.height() * scale);
        int previewX = canvasLeft + (canvasRight - canvasLeft - renderedWidth) / 2;
        int previewY = y + CANVAS_TOP + (CANVAS_HEIGHT - renderedHeight) / 2;
        HudRenderer.renderPreviewAt(
                graphics,
                HudWidgetId.KEYSTROKES,
                previewSettings,
                previewX,
                previewY,
                scale
        );
    }

    @Override
    public int getItemHeight() {
        return HEIGHT;
    }
}
