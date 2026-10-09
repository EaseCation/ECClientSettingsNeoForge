package net.easecation.clientsettings.feature.hud.potions;

import net.easecation.clientsettings.profile.model.PotionHudSettings;

public final class PotionWarning {
    private PotionWarning() {}
    public static boolean active(PotionHudSettings settings, int durationTicks, boolean infinite, double tickRate) {
        return settings.warningEnabled() && !infinite && durationTicks >= 0 && Double.isFinite(tickRate)
                && tickRate > 0 && durationTicks / tickRate < settings.warningSeconds();
    }
    public static float opacity(PotionHudSettings settings, int durationTicks, boolean infinite,
            double tickRate, double gameTicks) {
        if (!active(settings, durationTicks, infinite, tickRate)) return 1.0F;
        // One pulse per game second. Icons never vanish entirely.
        return (float) (0.65 + 0.35 * Math.cos(gameTicks / tickRate * Math.PI * 2));
    }
    public static int fade(int argb, float opacity) {
        return ((int) Math.round((argb >>> 24) * opacity) << 24) | (argb & 0xFFFFFF);
    }
}
