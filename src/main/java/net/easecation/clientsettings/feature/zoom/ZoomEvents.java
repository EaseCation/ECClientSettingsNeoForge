package net.easecation.clientsettings.feature.zoom;

import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.client.ClientSettingsKeyMappings;
import net.easecation.clientsettings.profile.model.ZoomSettings;
import net.easecation.clientsettings.profile.runtime.ActiveProfileSnapshot;
import net.easecation.clientsettings.profile.runtime.ProfileParticipant;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.io.IOException;

public final class ZoomEvents implements ProfileParticipant {

    private static final ZoomEvents INSTANCE = new ZoomEvents();
    private static final ZoomController CONTROLLER = new ZoomController();
    private static boolean participantRegistered;
    private static ClientLevel trackedLevel;

    private ZoomEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ensureParticipantRegistered();
        ZoomSettings settings = settings();
        boolean clicked = ClientSettingsKeyMappings.ZOOM.consumeClick();
        Minecraft minecraft = Minecraft.getInstance();
        boolean levelChanged = minecraft.level != trackedLevel;
        trackedLevel = minecraft.level;
        CONTROLLER.updateInput(
                settings,
                ClientSettingsKeyMappings.ZOOM.isDown(),
                clicked,
                validContext() && !levelChanged
        );
        CONTROLLER.tick();
    }

    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (validContext() && CONTROLLER.shouldAffectView()) {
            event.setFOV((float) (event.getFOV() * CONTROLLER.interpolatedMultiplier(event.getPartialTick())));
        }
    }

    public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        if (!validContext()) {
            return;
        }
        ZoomSettings settings = settings();
        event.setMouseSensitivity(CONTROLLER.effectiveSensitivity(event.getMouseSensitivity(), settings));
        event.setCinematicCameraEnabled(CONTROLLER.effectiveCinematicCamera(
                event.getCinematicCameraEnabled(),
                settings
        ));
    }

    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (CONTROLLER.handleScroll(settings(), event.getScrollDeltaY(), validContext())) {
            event.setCanceled(true);
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        trackedLevel = null;
        CONTROLLER.snapReset(settings());
    }

    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        CONTROLLER.snapReset(settings());
    }

    @Override
    public void apply(ActiveProfileSnapshot previous, ActiveProfileSnapshot current) {
        CONTROLLER.snapReset(current.features().zoom());
    }

    @Override
    public void resetTransientState() {
        CONTROLLER.snapReset(settings());
    }

    private static ZoomSettings settings() {
        return ProfileServices.active().features().zoom();
    }

    private static boolean validContext() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return minecraft.screen == null
                && minecraft.level != null
                && minecraft.level == trackedLevel
                && player != null
                && player.isAlive()
                && minecraft.isWindowActive();
    }

    private static void ensureParticipantRegistered() {
        if (participantRegistered) {
            return;
        }
        try {
            ProfileServices.manager().registerParticipant(INSTANCE);
            participantRegistered = true;
        } catch (IOException exception) {
            ECClientSettings.LOGGER.error("Could not register Zoom Profile participant", exception);
        }
    }
}
