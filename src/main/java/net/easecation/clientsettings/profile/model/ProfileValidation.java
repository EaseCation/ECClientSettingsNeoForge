package net.easecation.clientsettings.profile.model;

import java.util.Objects;

final class ProfileValidation {

    private ProfileValidation() {
    }

    static <T> T requireNonNull(T value, String field) {
        return Objects.requireNonNull(value, field + " must not be null");
    }

    static double requireRange(double value, double minimum, double maximum, String field) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(field + " must be finite and in " + minimum + ".." + maximum);
        }
        return value;
    }
}
