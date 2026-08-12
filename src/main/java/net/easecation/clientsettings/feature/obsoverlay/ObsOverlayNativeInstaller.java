package net.easecation.clientsettings.feature.obsoverlay;

import java.util.function.Consumer;

@FunctionalInterface
public interface ObsOverlayNativeInstaller {

    ObsOverlayInstallation install(
            long glfwWindow,
            Runnable compositor,
            Consumer<Throwable> failureHandler
    ) throws Exception;
}
