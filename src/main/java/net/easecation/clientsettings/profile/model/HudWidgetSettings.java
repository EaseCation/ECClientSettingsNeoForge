package net.easecation.clientsettings.profile.model;

public record HudWidgetSettings(
        boolean enabled,
        double normalizedX,
        double normalizedY,
        double scale,
        HudWidgetStyle style
) {

    public HudWidgetSettings(boolean enabled, double normalizedX, double normalizedY, double scale) {
        this(enabled, normalizedX, normalizedY, scale, HudWidgetStyle.DEFAULT);
    }

    public HudWidgetSettings {
        normalizedX = ProfileValidation.requireRange(normalizedX, 0.0, 1.0, "hud.normalizedX");
        normalizedY = ProfileValidation.requireRange(normalizedY, 0.0, 1.0, "hud.normalizedY");
        scale = ProfileValidation.requireRange(scale, 0.5, 3.0, "hud.scale");
        style = ProfileValidation.requireNonNull(style, "hud.style");
    }
}
