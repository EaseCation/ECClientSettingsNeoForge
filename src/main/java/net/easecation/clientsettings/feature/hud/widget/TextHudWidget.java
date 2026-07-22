package net.easecation.clientsettings.feature.hud.widget;

import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.feature.hud.HudSize;
import net.easecation.clientsettings.feature.hud.HudTextRenderer;
import net.easecation.clientsettings.feature.hud.HudWidget;

abstract class TextHudWidget implements HudWidget {

    private static final HudSize SIZE = new HudSize(60, 10);

    @Override
    public final HudSize previewSize() {
        return SIZE;
    }

    @Override
    public final HudSize measure(HudRenderContext context) {
        return SIZE;
    }

    @Override
    public final void render(HudRenderContext context, HudSize size) {
        String text = text(context);
        int x = Math.max(1, (size.width() - context.font().width(text)) / 2);
        int y = (size.height() - context.font().lineHeight) / 2;
        HudTextRenderer.draw(context, text, x, y);
    }

    protected abstract String text(HudRenderContext context);

}
