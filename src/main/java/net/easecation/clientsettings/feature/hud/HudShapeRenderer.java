package net.easecation.clientsettings.feature.hud;

import net.minecraft.client.gui.GuiGraphics;

public final class HudShapeRenderer {

    private HudShapeRenderer() {
    }

    public static void fillRoundedRect(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int effectiveRadius = Math.clamp(radius, 0, Math.min(width, height) / 2);
        if (effectiveRadius == 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        graphics.fill(
                x + effectiveRadius,
                y,
                x + width - effectiveRadius,
                y + height,
                color
        );
        for (int row = 0; row < effectiveRadius; row++) {
            double vertical = effectiveRadius - row - 0.5;
            int inset = (int) Math.ceil(effectiveRadius - Math.sqrt(
                    effectiveRadius * effectiveRadius - vertical * vertical
            ));
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            graphics.fill(
                    x + inset,
                    y + height - row - 1,
                    x + width - inset,
                    y + height - row,
                    color
            );
        }
        graphics.fill(
                x,
                y + effectiveRadius,
                x + width,
                y + height - effectiveRadius,
                color
        );
    }

    public static void drawRoundedBorder(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int borderWidth,
            int color
    ) {
        if (width <= 0 || height <= 0 || borderWidth <= 0) {
            return;
        }
        int effectiveBorder = Math.min(borderWidth, Math.min(width, height) / 2);
        int effectiveRadius = Math.clamp(radius, 0, Math.min(width, height) / 2);
        int innerWidth = width - effectiveBorder * 2;
        int innerHeight = height - effectiveBorder * 2;
        int innerRadius = Math.max(0, effectiveRadius - effectiveBorder);

        for (int row = 0; row < height; row++) {
            int outerInset = roundedRowInset(row, height, effectiveRadius);
            int outerStart = x + outerInset;
            int outerEnd = x + width - outerInset;
            int innerRow = row - effectiveBorder;
            if (innerWidth <= 0 || innerHeight <= 0 || innerRow < 0 || innerRow >= innerHeight) {
                graphics.fill(outerStart, y + row, outerEnd, y + row + 1, color);
                continue;
            }

            int innerInset = roundedRowInset(innerRow, innerHeight, innerRadius);
            int innerStart = x + effectiveBorder + innerInset;
            int innerEnd = x + width - effectiveBorder - innerInset;
            if (innerStart > outerStart) {
                graphics.fill(outerStart, y + row, innerStart, y + row + 1, color);
            }
            if (innerEnd < outerEnd) {
                graphics.fill(innerEnd, y + row, outerEnd, y + row + 1, color);
            }
        }
    }

    private static int roundedRowInset(int row, int height, int radius) {
        if (radius == 0 || row >= radius && row < height - radius) {
            return 0;
        }
        int edgeRow = row < radius ? row : height - row - 1;
        double vertical = radius - edgeRow - 0.5;
        return (int) Math.ceil(radius - Math.sqrt(radius * radius - vertical * vertical));
    }
}
