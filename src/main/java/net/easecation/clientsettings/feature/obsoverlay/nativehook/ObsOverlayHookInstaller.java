package net.easecation.clientsettings.feature.obsoverlay.nativehook;

import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayInstallation;
import net.easecation.clientsettings.feature.obsoverlay.ObsOverlayNativeInstaller;

import java.util.function.Consumer;

public final class ObsOverlayHookInstaller implements ObsOverlayNativeInstaller {

    @Override
    public ObsOverlayInstallation install(
            Runnable compositor,
            Consumer<Throwable> failureHandler
    ) throws Exception {
        ObsOverlayHook.requireSupportedPlatform();
        return ObsOverlayHook.install(compositor, failureHandler);
    }
}
