package net.easecation.clientsettings.feature.hud.widget;

import java.util.Locale;
import net.easecation.clientsettings.feature.hud.*;
import net.easecation.clientsettings.feature.hud.speed.SpeedHudRuntime;
import net.easecation.clientsettings.profile.model.*;
import net.minecraft.client.Minecraft;

public final class SpeedHudWidget implements HudWidget {
    @Override public HudWidgetId id() { return HudWidgetId.SPEED; }
    @Override public HudSize previewSize() { return new HudSize(72, 10); }
    @Override public HudSize previewSize(HudSettings settings) {
        return size(Minecraft.getInstance().font, settings.speed(), 4.35);
    }
    @Override public HudSize measure(HudRenderContext context) {
        return size(context.font(), context.hudSettings().speed(), value(context));
    }
    private static HudSize size(net.minecraft.client.gui.Font font, SpeedHudSettings settings, double speed) {
        // Reserve ordinary numeric width to avoid constant editor/layout jitter.
        int width = Math.max(font.width(format(settings, speed)), font.width(format(settings, 999.99)));
        return new HudSize(Math.max(60, width + 2), 10);
    }
    private static double value(HudRenderContext context) {
        return context.preview() ? 4.35 : SpeedHudRuntime.speed(context.hudSettings().speed().horizontalOnly());
    }
    @Override public void render(HudRenderContext context, HudSize size) {
        String text = format(context.hudSettings().speed(), value(context));
        HudTextRenderer.draw(context, text, Math.max(1, (size.width() - context.font().width(text)) / 2), 0);
    }
    public static String format(SpeedHudSettings settings, double speed) {
        if (!Double.isFinite(speed) || speed < 0.005) speed = 0;
        return settings.textTemplate().replace("{speed}", String.format(Locale.ROOT, "%.2f", speed));
    }
}
