package net.easecation.clientsettings.profile.model;

/** Independent display controls; defaults preserve vanilla rendering. */
public record DroppedItemSettings(boolean physics, boolean rotation, boolean floating) {
    public static final DroppedItemSettings DEFAULT = new DroppedItemSettings(false, true, true);
}
