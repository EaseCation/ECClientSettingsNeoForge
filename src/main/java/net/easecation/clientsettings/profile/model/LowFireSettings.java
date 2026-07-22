package net.easecation.clientsettings.profile.model;

public record LowFireSettings(boolean enabled, double verticalOffset) {

    public static final LowFireSettings DEFAULT = new LowFireSettings(false, 0.2);

    public LowFireSettings {
        verticalOffset = ProfileValidation.requireRange(verticalOffset, 0.0, 0.5, "lowFire.verticalOffset");
    }
}
