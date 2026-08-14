package net.easecation.clientsettings.feature.obsoverlay;

public interface ObsOverlayInstallation {

    boolean unsafeCaptureOrder();

    boolean isObsCaptureLoaded();

    default ObsOverlayInstallation armTargetSwap() {
        return null;
    }

    default void disarmTargetSwap() {
    }

    void uninstall() throws Exception;
}
