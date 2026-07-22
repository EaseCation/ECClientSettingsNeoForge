package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.profile.model.HudTextColorMode;
import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class HudTextRenderer {

    private static final int VISUAL_BASELINE_OFFSET = 1;

    private HudTextRenderer() {
    }

    public static void draw(HudRenderContext context, String text, int x, int y) {
        HudWidgetStyle style = context.style();
        if (style.textColorMode() == HudTextColorMode.FIXED) {
            drawFixed(context, text, x, y, style.textColor().value());
            return;
        }

        MutableComponent coloredText = Component.empty();
        int glyphIndex = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String glyph = new String(Character.toChars(codePoint));
            int color = animatedColor(style, context.animationMillis(), glyphIndex);
            coloredText.append(Component.literal(glyph).withStyle(Style.EMPTY.withColor(color)));
            glyphIndex++;
            offset += Character.charCount(codePoint);
        }
        FormattedCharSequence visualOrder = coloredText.getVisualOrderText();
        context.graphics().drawString(
                context.font(),
                visualOrder,
                x,
                y + VISUAL_BASELINE_OFFSET,
                style.textColor().value(),
                style.textShadow()
        );
    }

    public static void drawFixed(HudRenderContext context, String text, int x, int y, int color) {
        context.graphics().drawString(
                context.font(),
                text,
                x,
                y + VISUAL_BASELINE_OFFSET,
                color,
                context.style().textShadow()
        );
    }

    static int animatedColor(HudWidgetStyle style, long animationMillis, int glyphIndex) {
        double elapsedSeconds = animationMillis / 1000.0;
        double hue = switch (style.textColorMode()) {
            case FIXED -> 0.0;
            case RAINBOW_WAVE -> elapsedSeconds * style.animationSpeed()
                    + glyphIndex * style.rainbowSpread();
            case RAINBOW_SWITCH -> Math.floor(elapsedSeconds * style.animationSpeed()) / 12.0;
        };
        return hsvColor(hue - Math.floor(hue), style.textColor().value() >>> 24);
    }

    private static int hsvColor(double hue, int alpha) {
        double scaled = hue * 6.0;
        int sector = (int) Math.floor(scaled) % 6;
        double fraction = scaled - Math.floor(scaled);
        double red;
        double green;
        double blue;
        switch (sector) {
            case 0 -> {
                red = 1.0;
                green = fraction;
                blue = 0.0;
            }
            case 1 -> {
                red = 1.0 - fraction;
                green = 1.0;
                blue = 0.0;
            }
            case 2 -> {
                red = 0.0;
                green = 1.0;
                blue = fraction;
            }
            case 3 -> {
                red = 0.0;
                green = 1.0 - fraction;
                blue = 1.0;
            }
            case 4 -> {
                red = fraction;
                green = 0.0;
                blue = 1.0;
            }
            default -> {
                red = 1.0;
                green = 0.0;
                blue = 1.0 - fraction;
            }
        }
        return (alpha << 24)
                | ((int) Math.round(red * 255.0) << 16)
                | ((int) Math.round(green * 255.0) << 8)
                | (int) Math.round(blue * 255.0);
    }
}
