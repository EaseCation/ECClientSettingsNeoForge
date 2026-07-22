package net.easecation.clientsettings.feature.timechanger;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.profile.model.TimeChangerSettings;
import net.easecation.clientsettings.profile.runtime.ActiveProfileSnapshot;
import net.easecation.clientsettings.profile.runtime.ProfileParticipant;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;

public final class TimeChangerRuntime implements ProfileParticipant {

    private static final TimeChangerRuntime INSTANCE = new TimeChangerRuntime();
    private static final TimeChangerController CONTROLLER = new TimeChangerController();
    private static ClientLevel trackedLevel;
    private static boolean participantRegistered;

    private TimeChangerRuntime() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != trackedLevel) {
            trackedLevel = level;
            if (level == null) {
                CONTROLLER.clear();
            } else {
                CONTROLLER.enterWorld(level);
            }
            ensureParticipantRegistered();
            if (level != null) {
                applyCurrentSettings();
            }
        } else {
            ensureParticipantRegistered();
        }
        CONTROLLER.tick();
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        trackedLevel = null;
        CONTROLLER.clear();
    }

    public static ClientTimeValues onServerTime(
            ClientLevel level,
            long gameTime,
            long dayTime,
            boolean tickDayTime
    ) {
        trackedLevel = level;
        TimeChangerSettings settings = ProfileServices.active().features().timeChanger();
        return CONTROLLER.onServerTime(level, gameTime, dayTime, tickDayTime, settings);
    }

    @Override
    public void apply(ActiveProfileSnapshot previous, ActiveProfileSnapshot current) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        CONTROLLER.applySettings(
                level,
                level.getGameTime(),
                level.getDayTime(),
                current.features().timeChanger()
        ).ifPresent(TimeChangerRuntime::applyToLevel);
    }

    @Override
    public void resetTransientState() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        CONTROLLER.restoreBeforeProfileSwitch(level).ifPresent(TimeChangerRuntime::applyToLevel);
    }

    private static void applyCurrentSettings() {
        ClientLevel level = trackedLevel;
        if (level == null) {
            return;
        }
        CONTROLLER.applySettings(
                level,
                level.getGameTime(),
                level.getDayTime(),
                ProfileServices.active().features().timeChanger()
        ).ifPresent(TimeChangerRuntime::applyToLevel);
    }

    private static void applyToLevel(ClientTimeValues values) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            trackedLevel = level;
            level.setTimeFromServer(values.gameTime(), values.dayTime(), values.tickDayTime());
        }
    }

    private static void ensureParticipantRegistered() {
        if (participantRegistered) {
            return;
        }
        try {
            ProfileServices.manager().registerParticipant(INSTANCE);
            participantRegistered = true;
        } catch (IOException exception) {
            ECClientSettings.LOGGER.error("Could not register Time Changer Profile participant", exception);
        }
    }
}
