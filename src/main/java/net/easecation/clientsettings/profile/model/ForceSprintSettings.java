package net.easecation.clientsettings.profile.model;

public record ForceSprintSettings(boolean enabled) {

    public static final ForceSprintSettings DEFAULT = new ForceSprintSettings(true);
}
