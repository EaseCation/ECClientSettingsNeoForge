package net.easecation.clientsettings.feature.hud;

public record HudPosition(double normalizedX, double normalizedY) {

    public HudPosition {
        if (!Double.isFinite(normalizedX) || normalizedX < 0.0 || normalizedX > 1.0
                || !Double.isFinite(normalizedY) || normalizedY < 0.0 || normalizedY > 1.0) {
            throw new IllegalArgumentException("HUD position must be finite and normalized");
        }
    }
}
