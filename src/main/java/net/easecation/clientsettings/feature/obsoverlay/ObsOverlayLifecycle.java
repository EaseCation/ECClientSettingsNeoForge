package net.easecation.clientsettings.feature.obsoverlay;

import java.util.Objects;

final class ObsOverlayLifecycle {

    @FunctionalInterface
    interface Installer {
        ObsOverlayInstallation install() throws Exception;
    }

    private final Installer installer;
    private volatile ObsOverlayLifecycleState state = ObsOverlayLifecycleState.WAITING_FOR_CONFIG;
    private volatile ObsOverlayInstallation installation;
    private volatile Throwable failure;
    private boolean configLoaded;
    private boolean enabled;
    private boolean clientStarted;
    private boolean stopped;

    ObsOverlayLifecycle(Installer installer) {
        this.installer = Objects.requireNonNull(installer, "installer");
    }

    synchronized void onConfigLoaded(boolean enabled) {
        if (stopped) {
            return;
        }
        configLoaded = true;
        this.enabled = enabled;
        if (!enabled) {
            uninstallTo(ObsOverlayLifecycleState.DISABLED);
            return;
        }
        if (state == ObsOverlayLifecycleState.FAILED) {
            return;
        }
        if (installation != null) {
            state = ObsOverlayLifecycleState.ACTIVE;
            return;
        }
        state = ObsOverlayLifecycleState.READY;
        installIfReady();
    }

    synchronized void onConfigUnloaded() {
        configLoaded = false;
        enabled = false;
        if (stopped) {
            return;
        }
        uninstallTo(ObsOverlayLifecycleState.WAITING_FOR_CONFIG);
    }

    synchronized void onClientStarted() {
        if (stopped || clientStarted) {
            return;
        }
        clientStarted = true;
        if (!configLoaded) {
            state = ObsOverlayLifecycleState.WAITING_FOR_CONFIG;
            return;
        }
        if (!enabled) {
            state = ObsOverlayLifecycleState.DISABLED;
            return;
        }
        if (state != ObsOverlayLifecycleState.FAILED) {
            state = ObsOverlayLifecycleState.READY;
            installIfReady();
        }
    }

    synchronized void fail(Throwable throwable) {
        if (stopped
                || (state != ObsOverlayLifecycleState.INSTALLING
                    && state != ObsOverlayLifecycleState.ACTIVE)) {
            return;
        }
        failure = Objects.requireNonNull(throwable, "throwable");
        state = ObsOverlayLifecycleState.FAILED;
    }

    synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        clientStarted = false;
        enabled = false;
        uninstallTo(ObsOverlayLifecycleState.DISABLED);
    }

    ObsOverlayLifecycleState state() {
        return state;
    }

    Throwable failure() {
        return failure;
    }

    boolean unsafeCaptureOrder() {
        ObsOverlayInstallation current = installation;
        return current != null && current.unsafeCaptureOrder();
    }

    synchronized boolean isObsCaptureLoaded() {
        return installation != null && installation.isObsCaptureLoaded();
    }

    void updateWindow(long glfwWindow) throws Exception {
        ObsOverlayInstallation current = installation;
        if (state == ObsOverlayLifecycleState.ACTIVE && current != null) {
            current.updateWindow(glfwWindow);
        }
    }

    boolean retainsInstallation() {
        return installation != null;
    }

    private void installIfReady() {
        if (!clientStarted
                || !configLoaded
                || !enabled
                || installation != null
                || state != ObsOverlayLifecycleState.READY) {
            return;
        }
        state = ObsOverlayLifecycleState.INSTALLING;
        try {
            ObsOverlayInstallation installed = Objects.requireNonNull(
                    installer.install(),
                    "OBS overlay installer returned null"
            );
            installation = installed;
            if (state == ObsOverlayLifecycleState.INSTALLING) {
                failure = null;
                state = ObsOverlayLifecycleState.ACTIVE;
            }
        } catch (Exception | LinkageError throwable) {
            if (throwable instanceof ObsOverlayInstallException installFailure) {
                installation = installFailure.retainedInstallation();
            }
            failure = throwable;
            state = ObsOverlayLifecycleState.FAILED;
        }
    }

    private void uninstallTo(ObsOverlayLifecycleState target) {
        if (installation == null) {
            failure = null;
            state = target;
            return;
        }
        state = ObsOverlayLifecycleState.UNINSTALLING;
        try {
            installation.uninstall();
            installation = null;
            failure = null;
            state = target;
        } catch (Exception | LinkageError throwable) {
            failure = throwable;
            state = ObsOverlayLifecycleState.FAILED;
        }
    }
}
