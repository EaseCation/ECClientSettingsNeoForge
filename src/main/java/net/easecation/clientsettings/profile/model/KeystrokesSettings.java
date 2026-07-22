package net.easecation.clientsettings.profile.model;

public record KeystrokesSettings(
        boolean showMovement,
        boolean showJump,
        boolean showMouseButtons,
        boolean showCps,
        int keySize,
        int gap,
        int cornerRadius,
        int keyBorderWidth,
        ArgbColor idleBackgroundColor,
        ArgbColor pressedBackgroundColor,
        ArgbColor idleBorderColor,
        ArgbColor pressedBorderColor,
        ArgbColor pressedTextColor,
        int pressAnimationMillis
) {

    public static final int MIN_KEY_SIZE = 16;
    public static final int MAX_KEY_SIZE = 36;
    public static final int MIN_GAP = 0;
    public static final int MAX_GAP = 8;
    public static final int MIN_CORNER_RADIUS = 0;
    public static final int MAX_CORNER_RADIUS = 6;
    public static final int MIN_KEY_BORDER_WIDTH = 0;
    public static final int MAX_KEY_BORDER_WIDTH = 3;
    public static final int MIN_PRESS_ANIMATION_MILLIS = 0;
    public static final int MAX_PRESS_ANIMATION_MILLIS = 500;

    public static final KeystrokesSettings DEFAULT = new KeystrokesSettings(
            true,
            true,
            true,
            false,
            20,
            2,
            0,
            1,
            ArgbColor.parse("#700C1015"),
            ArgbColor.parse("#FFFFFFFF"),
            ArgbColor.parse("#FFFFFFFF"),
            ArgbColor.parse("#FFFFFFFF"),
            ArgbColor.parse("#FF111418"),
            100
    );

    public KeystrokesSettings {
        keySize = requireRange(keySize, MIN_KEY_SIZE, MAX_KEY_SIZE, "hud.keystrokes.keySize");
        gap = requireRange(gap, MIN_GAP, MAX_GAP, "hud.keystrokes.gap");
        cornerRadius = requireRange(
                cornerRadius,
                MIN_CORNER_RADIUS,
                MAX_CORNER_RADIUS,
                "hud.keystrokes.cornerRadius"
        );
        keyBorderWidth = requireRange(
                keyBorderWidth,
                MIN_KEY_BORDER_WIDTH,
                MAX_KEY_BORDER_WIDTH,
                "hud.keystrokes.keyBorderWidth"
        );
        idleBackgroundColor = ProfileValidation.requireNonNull(
                idleBackgroundColor,
                "hud.keystrokes.idleBackgroundColor"
        );
        pressedBackgroundColor = ProfileValidation.requireNonNull(
                pressedBackgroundColor,
                "hud.keystrokes.pressedBackgroundColor"
        );
        idleBorderColor = ProfileValidation.requireNonNull(
                idleBorderColor,
                "hud.keystrokes.idleBorderColor"
        );
        pressedBorderColor = ProfileValidation.requireNonNull(
                pressedBorderColor,
                "hud.keystrokes.pressedBorderColor"
        );
        pressedTextColor = ProfileValidation.requireNonNull(
                pressedTextColor,
                "hud.keystrokes.pressedTextColor"
        );
        pressAnimationMillis = requireRange(
                pressAnimationMillis,
                MIN_PRESS_ANIMATION_MILLIS,
                MAX_PRESS_ANIMATION_MILLIS,
                "hud.keystrokes.pressAnimationMillis"
        );
    }

    private static int requireRange(int value, int minimum, int maximum, String field) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(field + " must be in " + minimum + ".." + maximum);
        }
        return value;
    }
}
