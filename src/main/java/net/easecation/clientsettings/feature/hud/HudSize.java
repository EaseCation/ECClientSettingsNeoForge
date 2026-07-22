package net.easecation.clientsettings.feature.hud;

public record HudSize(int width, int height) {

    public HudSize {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("HUD size must be positive");
        }
    }
}
