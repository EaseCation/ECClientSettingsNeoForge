package net.easecation.clientsettings.profile.model;

public record BlockOutlineSettings(
        boolean enabled,
        ArgbColor color,
        boolean fillEnabled,
        ArgbColor fillColor
) {

    public static final ArgbColor DEFAULT_FILL_COLOR = ArgbColor.parse("#4DFFFFFF");
    public static final BlockOutlineSettings DEFAULT = new BlockOutlineSettings(
            false,
            ArgbColor.parse("#CCFFFFFF"),
            false,
            DEFAULT_FILL_COLOR
    );

    public BlockOutlineSettings(boolean enabled, ArgbColor color) {
        this(enabled, color, false, DEFAULT_FILL_COLOR);
    }

    public BlockOutlineSettings {
        color = ProfileValidation.requireNonNull(color, "blockOutline.color");
        fillColor = ProfileValidation.requireNonNull(fillColor, "blockOutline.fillColor");
    }
}
