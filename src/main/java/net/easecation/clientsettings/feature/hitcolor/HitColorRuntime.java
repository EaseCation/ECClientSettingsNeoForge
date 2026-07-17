package net.easecation.clientsettings.feature.hitcolor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.easecation.clientsettings.ECClientSettings;
import net.easecation.clientsettings.profile.model.HitColorSettings;
import net.easecation.clientsettings.profile.runtime.ActiveProfileSnapshot;
import net.easecation.clientsettings.profile.runtime.ProfileParticipant;
import net.easecation.clientsettings.profile.runtime.ProfileServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;

import java.io.IOException;

public final class HitColorRuntime implements ProfileParticipant {

    private static final HitColorRuntime INSTANCE = new HitColorRuntime();
    private static final HitColorController CONTROLLER = new HitColorController();
    private static boolean participantRegistered;
    private static boolean stopping;
    private static boolean failureLogged;

    private HitColorRuntime() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (stopping) {
            return;
        }
        ensureParticipantRegistered();
        if (participantRegistered) {
            reconcileSafely(ProfileServices.active().features().hitColor());
        }
    }

    public static void onClientStopping(ClientStoppingEvent event) {
        stopping = true;
        try {
            runOnRenderThread(CONTROLLER::restore);
        } catch (Exception exception) {
            ECClientSettings.LOGGER.warn("Could not restore the vanilla hurt overlay while stopping", exception);
        }
    }

    @Override
    public void apply(ActiveProfileSnapshot previous, ActiveProfileSnapshot current) throws Exception {
        runOnRenderThread(() -> reconcile(current.features().hitColor()));
    }

    @Override
    public void resetTransientState() throws Exception {
        runOnRenderThread(CONTROLLER::restore);
    }

    private static void reconcileSafely(HitColorSettings settings) {
        try {
            runOnRenderThread(() -> reconcile(settings));
            failureLogged = false;
        } catch (Exception exception) {
            if (!failureLogged) {
                ECClientSettings.LOGGER.warn("Could not apply the custom hurt overlay; vanilla pixels were restored", exception);
                failureLogged = true;
            }
        }
    }

    private static void reconcile(HitColorSettings settings) {
        Minecraft minecraft = Minecraft.getInstance();
        OverlayTexture overlay = minecraft.gameRenderer.overlayTexture();
        CONTROLLER.reconcile(new NativeOverlayTextureAccess(overlay), settings);
    }

    private static void ensureParticipantRegistered() {
        if (participantRegistered) {
            return;
        }
        try {
            ProfileServices.manager().registerParticipant(INSTANCE);
            participantRegistered = true;
        } catch (IOException exception) {
            if (!failureLogged) {
                ECClientSettings.LOGGER.error("Could not register Hit Color Profile participant", exception);
                failureLogged = true;
            }
        }
    }

    private static void runOnRenderThread(Runnable operation) throws Exception {
        if (RenderSystem.isOnRenderThread()) {
            operation.run();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        try {
            minecraft.submit(() -> {
                RenderSystem.assertOnRenderThread();
                operation.run();
            }).get();
        } catch (java.util.concurrent.ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Render-thread operation failed", cause);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        }
    }
}
