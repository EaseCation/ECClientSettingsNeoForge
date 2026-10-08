package net.easecation.clientsettings.feature.helditeminfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeldItemTextWrapTest {
    @Test void preservesUnicodeColorAndBlankLines() {
        var input = Component.literal("金😀色").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("\n\n使用").withStyle(ChatFormatting.GREEN));
        var lines = HeldItemTextWrap.split(input, 2);
        assertEquals(List.of("金😀", "色", "", "使用"), lines.stream().map(Component::getString).toList());
        List<Integer> colors = new ArrayList<>();
        for (var line : lines) line.visit((style, text) -> {
            if (!text.isEmpty()) colors.add(style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals(List.of(0xFFAA00, 0xFFAA00, 0xFFAA00, 0x55FF55, 0x55FF55), colors);
    }

    @Test void exactlyFullLinesAndTrailingNewlinesHaveNoSpuriousRows() {
        assertEquals(List.of("ab", "cd"), HeldItemTextWrap.split(Component.literal("abcd"), 2).stream().map(Component::getString).toList());
        assertEquals(List.of("ab", ""), HeldItemTextWrap.split(Component.literal("ab\n"), 2).stream().map(Component::getString).toList());
    }
}
