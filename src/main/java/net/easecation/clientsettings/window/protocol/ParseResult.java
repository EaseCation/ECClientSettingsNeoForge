package net.easecation.clientsettings.window.protocol;

import java.util.Objects;

public record ParseResult<T>(T value, String error) {

    public ParseResult {
        if ((value == null) == (error == null)) {
            throw new IllegalArgumentException("Parse result must contain exactly one of value or error");
        }
    }

    public static <T> ParseResult<T> success(T value) {
        return new ParseResult<>(Objects.requireNonNull(value, "value"), null);
    }

    public static <T> ParseResult<T> failure(String error) {
        return new ParseResult<>(null, Objects.requireNonNull(error, "error"));
    }

    public boolean successful() {
        return error == null;
    }
}
