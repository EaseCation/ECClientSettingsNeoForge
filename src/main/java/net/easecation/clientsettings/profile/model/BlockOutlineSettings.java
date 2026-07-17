package net.easecation.clientsettings.profile.model;

public record BlockOutlineSettings(boolean enabled, ArgbColor color) {

    public static final BlockOutlineSettings DEFAULT = new BlockOutlineSettings(false, ArgbColor.parse("#CCFFFFFF"));

    public BlockOutlineSettings {
        color = ProfileValidation.requireNonNull(color, "blockOutline.color");
    }
}
