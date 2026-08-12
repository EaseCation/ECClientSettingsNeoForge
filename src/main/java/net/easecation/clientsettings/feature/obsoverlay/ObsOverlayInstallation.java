package net.easecation.clientsettings.feature.obsoverlay;

public interface ObsOverlayInstallation {

    boolean unsafeCaptureOrder();

    boolean isObsCaptureLoaded();

    void uninstall() throws Exception;
}
