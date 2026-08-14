package net.easecation.clientsettings.feature.obsoverlay;

public interface ObsOverlayInstallation {

    boolean unsafeCaptureOrder();

    boolean isObsCaptureLoaded();

    default void updateWindow(long glfwWindow) throws Exception {
    }

    void uninstall() throws Exception;
}
