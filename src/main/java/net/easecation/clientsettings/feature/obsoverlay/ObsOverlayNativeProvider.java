package net.easecation.clientsettings.feature.obsoverlay;

import java.util.function.Consumer;

final class ObsOverlayNativeProvider {

    private ObsOverlayNativeProvider() {
    }

    static ObsOverlayInstallation install(
            Runnable compositor,
            Consumer<Throwable> failureHandler
    ) throws Exception {
        return Holder.INSTALLER.install(compositor, failureHandler);
    }

    private static final class Holder {
        private static final ObsOverlayNativeInstaller INSTALLER =
                new net.easecation.clientsettings.feature.obsoverlay.nativehook.ObsOverlayHookInstaller();
    }
}
