package net.easecation.clientsettings.feature.hud.widget;

import net.easecation.clientsettings.feature.hud.HudRenderContext;
import net.easecation.clientsettings.feature.hud.HudSize;
import net.easecation.clientsettings.feature.hud.HudTextRenderer;
import net.easecation.clientsettings.feature.hud.HudWidget;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.List;

public final class PotionHudWidget implements HudWidget {

    private static final int MINIMUM_WIDTH = 100;
    private static final int MAXIMUM_WIDTH = 176;
    private static final int ROW_HEIGHT = 20;
    private static final int WARNING_TICKS = 10 * 20;
    private static final int TEXT_X = 21;
    private static final int RIGHT_PADDING = 3;
    private static final int PREVIEW_ROWS = 3;
    private static final HudSize PREVIEW_SIZE = new HudSize(MINIMUM_WIDTH, ROW_HEIGHT * PREVIEW_ROWS);

    @Override
    public HudWidgetId id() {
        return HudWidgetId.POTIONS;
    }

    @Override
    public HudSize previewSize() {
        return PREVIEW_SIZE;
    }

    @Override
    public HudSize measure(HudRenderContext context) {
        List<MobEffectInstance> effects = effects(context);
        int width = MINIMUM_WIDTH;
        for (MobEffectInstance effect : effects) {
            width = Math.max(width, TEXT_X + RIGHT_PADDING + Math.max(
                    context.font().width(effectName(effect)),
                    context.font().width(duration(context, effect))
            ));
        }
        return new HudSize(Math.min(MAXIMUM_WIDTH, width), Math.max(1, effects.size()) * ROW_HEIGHT);
    }

    @Override
    public boolean shouldRender(HudRenderContext context) {
        return context.preview() || !effects(context).isEmpty();
    }

    @Override
    public void render(HudRenderContext context, HudSize size) {
        List<MobEffectInstance> effects = effects(context);
        for (int index = 0; index < effects.size(); index++) {
            renderEffect(context, effects.get(index), index * ROW_HEIGHT, size.width());
        }
    }

    private static void renderEffect(
            HudRenderContext context,
            MobEffectInstance effect,
            int y,
            int width
    ) {
        if (y > 0 && context.style().borderEnabled()) {
            context.graphics().fill(1, y, width - 1, y + 1, context.style().borderColor().value());
        }
        context.graphics().blitSprite(
                RenderPipelines.GUI_TEXTURED,
                Gui.getMobEffectSprite(effect.getEffect()),
                0,
                y + 1,
                18,
                18
        );
        int textWidth = width - TEXT_X - RIGHT_PADDING;
        Component name = fitText(context.font(), effectName(effect), textWidth);
        HudTextRenderer.draw(context, name.getString(), TEXT_X, y);
        Component duration = fitText(context.font(), duration(context, effect), textWidth);
        if (!effect.isInfiniteDuration() && effect.endsWithin(WARNING_TICKS)) {
            int warningColor = (context.style().textColor().value() & 0xFF000000) | 0x00FF5555;
            HudTextRenderer.drawFixed(context, duration.getString(), TEXT_X, y + 10, warningColor);
        } else {
            HudTextRenderer.draw(context, duration.getString(), TEXT_X, y + 10);
        }
    }

    private static List<MobEffectInstance> effects(HudRenderContext context) {
        if (context.preview()) {
            return List.of(
                    new MobEffectInstance(MobEffects.SPEED, 75 * 20, 1),
                    new MobEffectInstance(MobEffects.ABSORPTION, 45 * 20, 0),
                    new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 0)
            );
        }
        if (context.player() == null) {
            return List.of();
        }
        return context.player().getActiveEffects().stream()
                .filter(ClientHooks::shouldRenderEffect)
                .sorted()
                .toList();
    }

    private static Component effectName(MobEffectInstance effect) {
        MutableComponent name = effect.getEffect().value().getDisplayName().copy();
        if (effect.getAmplifier() >= 0 && effect.getAmplifier() <= 9) {
            name.append(CommonComponents.SPACE)
                    .append(Component.translatable(effectLevelTranslationKey(effect.getAmplifier())));
        } else {
            name.append(" ").append(Integer.toString(effect.getAmplifier() + 1));
        }
        return name;
    }

    static String effectLevelTranslationKey(int amplifier) {
        if (amplifier < 0 || amplifier > 9) {
            throw new IllegalArgumentException("amplifier must be in 0..9");
        }
        return "enchantment.level." + (amplifier + 1);
    }

    private static Component fitText(Font font, Component text, int maximumWidth) {
        if (font.width(text) <= maximumWidth) {
            return text;
        }
        String suffix = "...";
        int contentWidth = Math.max(0, maximumWidth - font.width(suffix));
        return Component.literal(font.plainSubstrByWidth(text.getString(), contentWidth) + suffix);
    }

    private static Component duration(HudRenderContext context, MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) {
            return Component.literal("\u221e");
        }
        float tickRate = context.minecraft().level == null
                ? 20.0F
                : context.minecraft().level.tickRateManager().tickrate();
        return MobEffectUtil.formatDuration(effect, 1.0F, tickRate);
    }
}
