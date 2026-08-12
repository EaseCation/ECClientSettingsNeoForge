package net.easecation.clientsettings.feature.obsoverlay;

public final class ObsOverlayInstallException extends Exception {

    private final ObsOverlayInstallation retainedInstallation;

    public ObsOverlayInstallException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public ObsOverlayInstallException(
            String message,
            Throwable cause,
            ObsOverlayInstallation retainedInstallation
    ) {
        super(message, cause);
        this.retainedInstallation = retainedInstallation;
    }

    public ObsOverlayInstallation retainedInstallation() {
        return retainedInstallation;
    }
}
