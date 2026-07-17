package net.easecation.clientsettings.profile.model;

import java.util.Locale;
import java.util.regex.Pattern;

public record ArgbColor(int value) {

    private static final Pattern SERIALIZED_PATTERN = Pattern.compile("#[0-9A-Fa-f]{8}");

    public static ArgbColor parse(String serialized) {
        if (serialized == null || !SERIALIZED_PATTERN.matcher(serialized).matches()) {
            throw new IllegalArgumentException("color must use #AARRGGBB");
        }
        return new ArgbColor((int) Long.parseLong(serialized.substring(1), 16));
    }

    public String serialized() {
        return String.format(Locale.ROOT, "#%08X", Integer.toUnsignedLong(value));
    }

    @Override
    public String toString() {
        return serialized();
    }
}
