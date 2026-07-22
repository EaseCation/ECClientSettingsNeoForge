package net.easecation.clientsettings.feature.hud.widget;

import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.profile.model.HudWidgetId;

public final class FpsHudWidget extends TextHudWidget {

    private static final int MAX_DISPLAYED_FPS = 9_999;

    @Override
    public HudWidgetId id() {
        return HudWidgetId.FPS;
    }

    @Override
    protected String text(HudRenderContext context) {
        int fps = context.preview() ? 144 : Math.max(0, context.minecraft().getFps());
        return fps > MAX_DISPLAYED_FPS ? "FPS 9999+" : "FPS " + fps;
    }
}
