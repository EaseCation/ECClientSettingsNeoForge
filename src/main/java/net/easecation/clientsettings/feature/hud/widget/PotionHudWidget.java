package net.easecation.clientsettings.feature.hud.widget;

import java.util.List;
import net.easecation.clientsettings.feature.hud.*;
import net.easecation.clientsettings.feature.hud.potions.PotionEffectText;
import net.easecation.clientsettings.feature.hud.potions.PotionWarning;
import net.easecation.clientsettings.profile.model.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.client.ClientHooks;

public final class PotionHudWidget implements HudWidget {
    private static final int ROW_HEIGHT = 20;
    private static final int TEXT_X = 21;
    @Override public HudWidgetId id() { return HudWidgetId.POTIONS; }
    @Override public HudSize previewSize() { return new HudSize(100, ROW_HEIGHT * 3); }
    @Override public HudSize previewSize(HudSettings settings) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.font == null) return previewSize();
        return measureContent(mc.font, previewEffects(), settings.potions(), 20);
    }
    @Override public HudSize measure(HudRenderContext context) {
        return measureContent(context.font(), effects(context), context.hudSettings().potions(), tickRate(context));
    }
    private static HudSize measureContent(net.minecraft.client.gui.Font font, List<MobEffectInstance> effects,
            PotionHudSettings settings, float rate) {
        int width = 20;
        for (var effect : effects) {
            int nameWidth = settings.showName() ? font.width(effect.getEffect().value().getDisplayName()) : 0;
            int levelWidth = settings.showLevel() ? font.width(PotionEffectText.level(effect)) : 0;
            int topWidth = nameWidth + levelWidth + (nameWidth > 0 && levelWidth > 0 ? font.width(" ") : 0);
            int timeWidth = settings.showTime() ? font.width(PotionEffectText.duration(effect, rate)) : 0;
            if (topWidth > 0 || timeWidth > 0) width = Math.max(width, TEXT_X + Math.max(topWidth, timeWidth) + 3);
        }
        return new HudSize(Math.min(256, width), Math.max(1, effects.size()) * ROW_HEIGHT);
    }
    @Override public boolean shouldRender(HudRenderContext context) { return context.preview() || !effects(context).isEmpty(); }
    @Override public void render(HudRenderContext context, HudSize size) {
        var settings = context.hudSettings().potions();
        var effects = effects(context);
        for (int i = 0; i < effects.size(); i++) {
            var effect = effects.get(i); int y = i * ROW_HEIGHT;
            double ticks = context.minecraft().gui.getGuiTicks() + context.partialTick();
            float alpha = PotionWarning.opacity(settings, effect.getDuration(), effect.isInfiniteDuration(), tickRate(context), ticks);
            if (y > 0 && context.style().borderEnabled()) {
                context.graphics().fill(1, y, size.width() - 1, y + 1, context.style().borderColor().value());
            }
            context.graphics().blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()),
                    0, y + 1, 18, 18, alpha);
            int available = size.width() - TEXT_X - 3;
            String level = settings.showLevel() ? PotionEffectText.level(effect) : "";
            String name = settings.showName() ? effect.getEffect().value().getDisplayName().getString() : "";
            boolean hasTop = !name.isEmpty() || !level.isEmpty();
            int topY = y + (settings.showTime() ? 0 : 5);
            int space = !name.isEmpty() && !level.isEmpty() ? context.font().width(" ") : 0;
            int levelWidth = context.font().width(level);
            name = fit(context, name, Math.max(0, available - levelWidth - space));
            if (!name.isEmpty()) {
                if (settings.effectNameColor()) {
                    int color = (context.style().textColor().value() & 0xFF000000) | effect.getEffect().value().getColor();
                    HudTextRenderer.drawFixed(context, name, TEXT_X, topY, PotionWarning.fade(color, alpha));
                } else {
                    HudWidgetStyle style = context.style();
                    var faded = new HudWidgetStyle(style.backgroundEnabled(), style.backgroundColor(), style.borderEnabled(),
                            style.borderColor(), style.borderWidth(), style.padding(), style.textShadow(), style.textColorMode(),
                            new ArgbColor(PotionWarning.fade(style.textColor().value(), alpha)), style.animationSpeed(), style.rainbowSpread());
                    HudTextRenderer.draw(context.withStyle(faded), name, TEXT_X, topY);
                }
            }
            if (!level.isEmpty()) HudTextRenderer.drawFixed(context, fit(context, level, available),
                    TEXT_X + context.font().width(name) + (name.isEmpty() ? 0 : space), topY,
                    PotionWarning.fade(settings.levelColor().value(), alpha));
            if (settings.showTime()) HudTextRenderer.drawFixed(context,
                    fit(context, PotionEffectText.duration(effect, tickRate(context)), available), TEXT_X, y + (hasTop ? 10 : 5),
                    PotionWarning.fade(settings.timeColor().value(), alpha));
        }
    }
    private static String fit(HudRenderContext context, String text, int width) {
        if (width <= 0) return "";
        if (context.font().width(text) <= width) return text;
        String suffix = "...";
        if (context.font().width(suffix) > width) return context.font().plainSubstrByWidth(text, width);
        return context.font().plainSubstrByWidth(text, width - context.font().width(suffix)) + suffix;
    }
    private static List<MobEffectInstance> effects(HudRenderContext context) {
        if (context.preview()) return previewEffects();
        if (context.player() == null) return List.of();
        return context.player().getActiveEffects().stream().filter(ClientHooks::shouldRenderEffect).sorted().toList();
    }
    private static List<MobEffectInstance> previewEffects() {
        return List.of(new MobEffectInstance(MobEffects.SPEED, 75 * 20, 1),
                new MobEffectInstance(MobEffects.ABSORPTION, -1, 0),
                new MobEffectInstance(MobEffects.REGENERATION, 8 * 20, 0));
    }
    private static float tickRate(HudRenderContext context) {
        return context.minecraft().level == null ? 20 : context.minecraft().level.tickRateManager().tickrate();
    }
    static String effectLevelTranslationKey(int amplifier) {
        if (amplifier < 0 || amplifier > 9) throw new IllegalArgumentException("amplifier must be in 0..9");
        return "enchantment.level." + (amplifier + 1);
    }
}
