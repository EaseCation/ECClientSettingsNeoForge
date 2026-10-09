package net.easecation.clientsettings.feature.hud.potions;

import net.easecation.clientsettings.profile.model.PotionHudSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;

public final class VanillaPotionOverlay {
    private VanillaPotionOverlay() {}
    public static float opacity(MobEffectInstance effect, PotionHudSettings settings) {
        var mc = Minecraft.getInstance();
        float rate = mc.level == null ? 20 : mc.level.tickRateManager().tickrate();
        return PotionWarning.opacity(settings, effect.getDuration(), effect.isInfiniteDuration(), rate,
                mc.gui.getGuiTicks() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
    public static void draw(GuiGraphics graphics, MobEffectInstance effect, int x, int y,
            float alpha, PotionHudSettings settings) {
        if (!settings.vanillaLevel() && !settings.vanillaTime()) return;
        var mc = Minecraft.getInstance();
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            // Keep labels within vanilla's 24x24 tile, without changing native slot positions.
            graphics.pose().scale(0.5F, 0.5F);
            if (settings.vanillaLevel()) {
                String level = PotionEffectText.level(effect);
                graphics.drawString(mc.font, level, 46 - mc.font.width(level), 1,
                        PotionWarning.fade(settings.vanillaLevelColor().value(), alpha), true);
            }
            if (settings.vanillaTime()) {
                float rate = mc.level == null ? 20 : mc.level.tickRateManager().tickrate();
                String time = PotionEffectText.compactDuration(effect, rate);
                graphics.drawString(mc.font, time, (48 - mc.font.width(time)) / 2, 36,
                        PotionWarning.fade(settings.vanillaTimeColor().value(), alpha), true);
            }
        } finally { graphics.pose().popMatrix(); }
    }
}
