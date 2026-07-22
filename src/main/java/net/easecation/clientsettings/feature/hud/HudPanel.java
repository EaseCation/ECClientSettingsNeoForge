package net.easecation.clientsettings.feature.hud;

import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.minecraft.client.gui.GuiGraphics;

public final class HudPanel {

    private HudPanel() {
    }

    public static int contentInset(HudWidgetStyle style) {
        return style.padding() + (style.borderEnabled() ? style.borderWidth() : 0);
    }

    public static HudSize outerSize(HudSize contentSize, HudWidgetStyle style) {
        int inset = contentInset(style) * 2;
        return new HudSize(contentSize.width() + inset, contentSize.height() + inset);
    }

    public static void draw(GuiGraphics graphics, HudSize size, HudWidgetStyle style) {
        if (style.backgroundEnabled()) {
            graphics.fill(0, 0, size.width(), size.height(), style.backgroundColor().value());
        }
        if (!style.borderEnabled()) {
            return;
        }
        int limit = Math.min(style.borderWidth(), Math.min(size.width(), size.height()) / 2);
        for (int inset = 0; inset < limit; inset++) {
            graphics.renderOutline(
                    inset,
                    inset,
                    size.width() - inset * 2,
                    size.height() - inset * 2,
                    style.borderColor().value()
            );
        }
    }
}
