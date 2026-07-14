package net.easecation.clientsettings.window.protocol;

import java.util.Objects;

public record FrameAppearance(Mode mode, Integer background, Integer text, Fallback fallback) {

    public enum Mode {
        RGB,
        DARK,
        SYSTEM
    }

    public enum Fallback {
        DARK,
        SYSTEM
    }

    public FrameAppearance {
        Objects.requireNonNull(mode, "mode");
        if (mode == Mode.RGB) {
            validateColor(background, "background");
            if (text != null) {
                validateColor(text, "text");
            }
            Objects.requireNonNull(fallback, "fallback");
        } else if (background != null || text != null || fallback != null) {
            throw new IllegalArgumentException("Only RGB frame appearance accepts colors and fallback");
        }
    }

    public static FrameAppearance rgb(int background, Integer text, Fallback fallback) {
        return new FrameAppearance(Mode.RGB, background, text, fallback);
    }

    public static FrameAppearance dark() {
        return new FrameAppearance(Mode.DARK, null, null, null);
    }

    public static FrameAppearance system() {
        return new FrameAppearance(Mode.SYSTEM, null, null, null);
    }

    public int effectiveTextColor() {
        if (text != null) {
            return text;
        }
        int red = background >> 16 & 0xFF;
        int green = background >> 8 & 0xFF;
        int blue = background & 0xFF;
        int brightness = (red * 299 + green * 587 + blue * 114) / 1000;
        return brightness >= 128 ? 0x000000 : 0xFFFFFF;
    }

    private static void validateColor(Integer color, String name) {
        if (color == null || color < 0 || color > 0xFFFFFF) {
            throw new IllegalArgumentException(name + " must be an RGB color");
        }
    }
}
