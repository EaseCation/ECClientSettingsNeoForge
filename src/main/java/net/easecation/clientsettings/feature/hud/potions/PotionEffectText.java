package net.easecation.clientsettings.feature.hud.potions;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

public final class PotionEffectText {
    private PotionEffectText() {}
    public static String level(MobEffectInstance effect) {
        int amplifier = effect.getAmplifier();
        return amplifier >= 0 && amplifier <= 9
                ? Component.translatable("enchantment.level." + (amplifier + 1)).getString()
                : Integer.toString(amplifier + 1);
    }
    public static String duration(MobEffectInstance effect, float tickRate) {
        return effect.isInfiniteDuration() ? "∞" : MobEffectUtil.formatDuration(effect, 1.0F, tickRate).getString();
    }
    public static String compactDuration(MobEffectInstance effect, float tickRate) {
        if (effect.isInfiniteDuration()) return "∞";
        long seconds = Math.max(0, (long) Math.ceil(effect.getDuration() / (double) tickRate));
        if (seconds >= 3600) return (seconds / 3600) + "h";
        if (seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }
}
