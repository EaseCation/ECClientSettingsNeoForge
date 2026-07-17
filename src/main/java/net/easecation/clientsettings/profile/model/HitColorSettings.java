package net.easecation.clientsettings.profile.model;

public record HitColorSettings(boolean enabled, ArgbColor color) {

    public static final HitColorSettings DEFAULT = new HitColorSettings(false, ArgbColor.parse("#80FF0000"));

    public HitColorSettings {
        color = ProfileValidation.requireNonNull(color, "hitColor.color");
    }
}
