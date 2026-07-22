package net.easecation.clientsettings.feature.hud;

public record HudBounds(int x, int y, int width, int height) {

    public HudBounds {
        if (x < 0 || y < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("HUD bounds must be non-negative");
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double pointX, double pointY) {
        return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
    }
}
