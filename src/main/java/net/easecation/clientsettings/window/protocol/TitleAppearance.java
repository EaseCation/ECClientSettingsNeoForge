package net.easecation.clientsettings.window.protocol;

import java.util.Objects;

public record TitleAppearance(Mode mode, String text) {

    public enum Mode {
        CUSTOM,
        DEFAULT
    }

    public TitleAppearance {
        Objects.requireNonNull(mode, "mode");
        if (mode == Mode.CUSTOM) {
            Objects.requireNonNull(text, "text");
        } else if (text != null) {
            throw new IllegalArgumentException("Default title must not contain text");
        }
    }

    public static TitleAppearance custom(String text) {
        return new TitleAppearance(Mode.CUSTOM, text);
    }

    public static TitleAppearance defaults() {
        return new TitleAppearance(Mode.DEFAULT, null);
    }
}
