package net.easecation.clientsettings.profile.model;

public record ZoomSettings(
        boolean enabled,
        ZoomActivation activation,
        double divisor,
        double maxDivisor,
        double animationSpeed,
        boolean scrollAdjustment,
        boolean reduceSensitivity,
        boolean smoothCamera
) {

    public static final ZoomSettings DEFAULT = new ZoomSettings(
            true,
            ZoomActivation.HOLD,
            4.0,
            16.0,
            7.5,
            false,
            true,
            false
    );

    public ZoomSettings {
        activation = ProfileValidation.requireNonNull(activation, "zoom.activation");
        divisor = ProfileValidation.requireRange(divisor, 1.0, 16.0, "zoom.divisor");
        maxDivisor = ProfileValidation.requireRange(maxDivisor, divisor, 32.0, "zoom.maxDivisor");
        animationSpeed = ProfileValidation.requireRange(animationSpeed, 1.0, 10.0, "zoom.animationSpeed");
    }
}
