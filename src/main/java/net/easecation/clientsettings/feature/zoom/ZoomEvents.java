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
    private static long lastAnimationNanos;

    private ZoomEvents() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        ensureParticipantRegistered();
        ZoomSettings settings = settings();
        boolean clicked = ClientSettingsKeyMappings.ZOOM.consumeClick();
        Minecraft minecraft = Minecraft.getInstance();
        boolean levelChanged = minecraft.level != trackedLevel;
        trackedLevel = minecraft.level;
        boolean validContext = validContext() && !levelChanged;
        CONTROLLER.updateInput(settings, ClientSettingsKeyMappings.ZOOM.isDown(), clicked, validContext);
        if (!validContext) {
            resetAnimationClock();
        }
    }

    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!validContext()) {
            resetAnimationClock();
            return;
        }
        ZoomSettings settings = settings();
        refreshInputAndAnimation(settings);
        if (CONTROLLER.shouldAffectView()) {
            event.setFOV((float) (event.getFOV() * CONTROLLER.currentMultiplier()));
        }
    }

    public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        if (!validContext()) {
            resetAnimationClock();
            return;
        }
        ZoomSettings settings = settings();
        refreshInputAndAnimation(settings);
        event.setMouseSensitivity(CONTROLLER.effectiveSensitivity(event.getMouseSensitivity(), settings));
        event.setCinematicCameraEnabled(CONTROLLER.effectiveCinematicCamera(
                event.getCinematicCameraEnabled(),
                settings
        ));
    }

    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        ZoomSettings settings = settings();
        boolean validContext = validContext();
        syncInput(settings, validContext);
        if (CONTROLLER.handleScroll(settings, event.getScrollDeltaY(), validContext).consumed()) {
            event.setCanceled(true);
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        trackedLevel = null;
        CONTROLLER.snapReset(settings());
        resetAnimationClock();
    }

    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        CONTROLLER.snapReset(settings());
        resetAnimationClock();
    }

    @Override
    public void apply(ActiveProfileSnapshot previous, ActiveProfileSnapshot current) {
        CONTROLLER.snapReset(current.features().zoom());
        resetAnimationClock();
    }

    @Override
    public void resetTransientState() {
        CONTROLLER.snapReset(settings());
        resetAnimationClock();
    }

    private static ZoomSettings settings() {
        return ProfileServices.active().features().zoom();
    }

    private static void refreshInputAndAnimation(ZoomSettings settings) {
        syncInput(settings, true);
        CONTROLLER.advance(animationDeltaSeconds(), settings);
    }

    private static void syncInput(ZoomSettings settings, boolean validContext) {
        CONTROLLER.updateInput(
                settings,
                ClientSettingsKeyMappings.ZOOM.isDown(),
                ClientSettingsKeyMappings.ZOOM.consumeClick(),
                validContext
        );
    }

    private static double animationDeltaSeconds() {
        long now = System.nanoTime();
        if (lastAnimationNanos == 0L) {
            lastAnimationNanos = now;
            return 0.0;
        }
        double deltaSeconds = (now - lastAnimationNanos) / 1_000_000_000.0;
        lastAnimationNanos = now;
        return deltaSeconds;
    }

    private static void resetAnimationClock() {
        lastAnimationNanos = 0L;
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
