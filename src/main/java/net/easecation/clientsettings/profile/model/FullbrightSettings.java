package net.easecation.clientsettings.profile.model;

public record FullbrightSettings(FullbrightMode mode, double strength) {

    public static final FullbrightSettings DEFAULT = new FullbrightSettings(FullbrightMode.OFF, 1.0);

    public FullbrightSettings {
        mode = ProfileValidation.requireNonNull(mode, "fullbright.mode");
        strength = ProfileValidation.requireRange(strength, 0.0, 1.0, "fullbright.strength");
    }
}
