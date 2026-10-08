package net.easecation.clientsettings.feature.helditeminfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Splits by Unicode code points, preserving styles and explicit empty lines. */
public final class HeldItemTextWrap {
    private HeldItemTextWrap() {}
    public static List<Component> split(Component text, int maximumCharacters) {
        if (maximumCharacters < 1) throw new IllegalArgumentException("maximumCharacters must be positive");
        List<Component> output = new ArrayList<>();
        MutableComponent[] current = {Component.empty()};
        int[] count = {0};
        boolean[] endedWithNewline = {false};
        text.visit((style, segment) -> {
            for (int offset = 0; offset < segment.length();) {
                int codePoint = segment.codePointAt(offset);
                offset += Character.charCount(codePoint);
                if (codePoint == '\r') continue;
                if (codePoint == '\n') {
                    output.add(current[0]); current[0] = Component.empty(); count[0] = 0;
                    endedWithNewline[0] = true;
                    continue;
                }
                if (count[0] == maximumCharacters) {
                    output.add(current[0]); current[0] = Component.empty(); count[0] = 0;
                }
                current[0].append(Component.literal(new String(Character.toChars(codePoint))).setStyle(style));
                count[0]++; endedWithNewline[0] = false;
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (count[0] > 0 || output.isEmpty() || endedWithNewline[0]) output.add(current[0]);
        return List.copyOf(output);
    }
}
