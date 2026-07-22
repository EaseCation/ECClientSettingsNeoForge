package net.easecation.clientsettings.profile.model;

public record HudWidgetStyle(
        boolean backgroundEnabled,
        ArgbColor backgroundColor,
        boolean borderEnabled,
        ArgbColor borderColor,
        int borderWidth,
        int padding,
        boolean textShadow,
        HudTextColorMode textColorMode,
        ArgbColor textColor,
        double animationSpeed,
        double rainbowSpread
) {

    public static final int MIN_BORDER_WIDTH = 1;
    public static final int MAX_BORDER_WIDTH = 3;
    public static final int MIN_PADDING = 0;
    public static final int MAX_PADDING = 12;
    public static final double MIN_ANIMATION_SPEED = 0.1;
    public static final double MAX_ANIMATION_SPEED = 2.0;
    public static final double MIN_RAINBOW_SPREAD = 0.01;
    public static final double MAX_RAINBOW_SPREAD = 0.25;

    public static final HudWidgetStyle DEFAULT = new HudWidgetStyle(
            true,
            ArgbColor.parse("#700C1015"),
            false,
            ArgbColor.parse("#40FFFFFF"),
            1,
            2,
            true,
            HudTextColorMode.FIXED,
            ArgbColor.parse("#FFFFFFFF"),
            0.6,
            0.08
    );

    public static HudWidgetStyle defaultsFor(HudWidgetId id) {
        ProfileValidation.requireNonNull(id, "hud.widgetId");
        if (id == HudWidgetId.KEYSTROKES) {
            return new HudWidgetStyle(
                    false,
                    DEFAULT.backgroundColor(),
                    false,
                    DEFAULT.borderColor(),
                    DEFAULT.borderWidth(),
                    0,
                    true,
                    DEFAULT.textColorMode(),
                    DEFAULT.textColor(),
                    DEFAULT.animationSpeed(),
                    DEFAULT.rainbowSpread()
            );
        }
        if (id == HudWidgetId.ARMOR || id == HudWidgetId.POTIONS) {
            return new HudWidgetStyle(
                    DEFAULT.backgroundEnabled(),
                    DEFAULT.backgroundColor(),
                    DEFAULT.borderEnabled(),
                    DEFAULT.borderColor(),
                    DEFAULT.borderWidth(),
                    1,
                    DEFAULT.textShadow(),
                    DEFAULT.textColorMode(),
                    DEFAULT.textColor(),
                    DEFAULT.animationSpeed(),
                    DEFAULT.rainbowSpread()
            );
        }
        return DEFAULT;
    }

    public HudWidgetStyle {
        backgroundColor = ProfileValidation.requireNonNull(backgroundColor, "hud.style.backgroundColor");
        borderColor = ProfileValidation.requireNonNull(borderColor, "hud.style.borderColor");
        textColorMode = ProfileValidation.requireNonNull(textColorMode, "hud.style.textColorMode");
        textColor = ProfileValidation.requireNonNull(textColor, "hud.style.textColor");
        borderWidth = requireRange(borderWidth, MIN_BORDER_WIDTH, MAX_BORDER_WIDTH, "hud.style.borderWidth");
        padding = requireRange(padding, MIN_PADDING, MAX_PADDING, "hud.style.padding");
        animationSpeed = ProfileValidation.requireRange(
                animationSpeed,
                MIN_ANIMATION_SPEED,
                MAX_ANIMATION_SPEED,
                "hud.style.animationSpeed"
        );
        rainbowSpread = ProfileValidation.requireRange(
                rainbowSpread,
                MIN_RAINBOW_SPREAD,
                MAX_RAINBOW_SPREAD,
                "hud.style.rainbowSpread"
        );
    }

    private static int requireRange(int value, int minimum, int maximum, String field) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(field + " must be in " + minimum + ".." + maximum);
        }
        return value;
    }
}
