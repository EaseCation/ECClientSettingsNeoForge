package net.easecation.clientsettings.movement;

public final class SneakCameraTransition {

    private SneakCameraTransition() {
    }

    public static boolean shouldSnap(boolean animationEnabled, boolean localPlayerCamera) {
        return !animationEnabled && localPlayerCamera;
    }
}
