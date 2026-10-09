package net.easecation.clientsettings.feature.hud.speed;

import net.easecation.clientsettings.profile.model.SpeedHudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class SpeedHudRuntime {
    private static final SpeedSampler SAMPLER = new SpeedSampler();
    private static SpeedHudSettings previous;
    private SpeedHudRuntime() {}
    public static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var hud = ProfileServices.active().features().hud();
        if (mc.player == null || mc.level == null || !hud.widget(HudWidgetId.SPEED).enabled()) {
            SAMPLER.clear(); previous = null; return;
        }
        if (mc.isPaused()) return;
        if (!hud.speed().equals(previous)) { SAMPLER.clear(); previous = hud.speed(); }
        var target = mc.player.getVehicle() == null ? mc.player : mc.player.getVehicle();
        SAMPLER.sample(mc.level, target, mc.player.tickCount, target.getX(), target.getY(), target.getZ(),
                mc.level.tickRateManager().tickrate());
    }
    public static double speed(boolean horizontalOnly) { return SAMPLER.speed(horizontalOnly); }
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SAMPLER.clear(); previous = null;
    }
}
