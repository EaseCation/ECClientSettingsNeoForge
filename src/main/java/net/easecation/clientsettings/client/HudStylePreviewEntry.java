package net.easecation.clientsettings.client;

import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.easecation.clientsettings.feature.hud.HudPanel;
import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.feature.hud.HudRenderMode;
import net.easecation.clientsettings.feature.hud.HudSize;
import net.easecation.clientsettings.feature.hud.HudTextRenderer;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Supplier;

@SuppressWarnings("deprecation")
final class HudStylePreviewEntry extends TextListEntry {

    private static final int HEIGHT = 68;
    private static final int CANVAS_TOP = 14;
    private static final int CANVAS_HEIGHT = 50;
    private static final int CHECKER_SIZE = 8;

    private final Component heading = Component.translatable("option.ecclientsettings.hud_style.preview");
    private final Component sample;
    private final Supplier<HudWidgetStyle> styleSupplier;

    HudStylePreviewEntry(HudWidgetId id, Supplier<HudWidgetStyle> styleSupplier) {
        super(Component.empty(), Component.empty());
        this.sample = Component.translatable(
                "option.ecclientsettings.hud_style.preview.sample",
                Component.translatable("option.ecclientsettings.hud." + id.serializedName() + ".enabled")
        );
        this.styleSupplier = Objects.requireNonNull(styleSupplier, "style supplier");
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
        graphics.drawString(font, heading, x + 4, y + 2, getPreferredTextColor());

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

        HudWidgetStyle style;
        try {
            style = styleSupplier.get();
        } catch (RuntimeException exception) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("option.ecclientsettings.hud_style.preview.invalid"),
                    x + entryWidth / 2,
                    y + CANVAS_TOP + 20,
                    0xFFFF7777
            );
            return;
        }
        HudSize contentSize = new HudSize(Math.max(60, font.width(sample) + 4), 12);
        HudSize panelSize = HudPanel.outerSize(contentSize, style);
        int panelX = x + (entryWidth - panelSize.width()) / 2;
        int panelY = y + CANVAS_TOP + (CANVAS_HEIGHT - panelSize.height()) / 2;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(panelX, panelY);
            HudPanel.draw(graphics, panelSize, style);
            int inset = HudPanel.contentInset(style);
            graphics.pose().translate(inset, inset);
            HudRenderContext context = new HudRenderContext(
                    minecraft,
                    graphics,
                    HudRenderMode.PREVIEW,
                    partialTick
            ).withStyle(style);
            HudTextRenderer.draw(context, sample.getString(), 2, 1);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Override
    public int getItemHeight() {
        return HEIGHT;
    }
}
