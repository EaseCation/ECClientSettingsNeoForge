package net.easecation.clientsettings.profile.model;

public record SpeedHudSettings(boolean horizontalOnly, String textTemplate) {
    public static final SpeedHudSettings DEFAULT = new SpeedHudSettings(true, "{speed} m/s");
    public SpeedHudSettings {
        textTemplate = ProfileValidation.requireNonNull(textTemplate, "hud.speed.textTemplate");
        if (textTemplate.codePointCount(0, textTemplate.length()) > 64
                || textTemplate.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Speed HUD text must be a single line of at most 64 characters");
        }
    }
}
