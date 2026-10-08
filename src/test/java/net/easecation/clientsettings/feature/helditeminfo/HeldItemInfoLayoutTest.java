package net.easecation.clientsettings.feature.helditeminfo;

import java.util.List;
import net.easecation.clientsettings.profile.model.HeldItemInfoSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.easecation.clientsettings.feature.helditeminfo.HeldItemInfoContent.Kind;

class HeldItemInfoLayoutTest {
    private final Font font = new Font(id -> null, false) {
        @Override public int width(FormattedCharSequence text) {
            int[] count = {0}; text.accept((index, style, codePoint) -> { count[0]++; return true; }); return count[0];
        }
        @Override public List<FormattedCharSequence> split(FormattedText text, int width) {
            return HeldItemTextWrap.split((Component) text, width).stream().map(Component::getVisualOrderText).toList();
        }
    };

    @Test void descriptionAndGlobalCapsCountEveryOmittedLineIncludingSummarySpace() {
        var content = List.of(new HeldItemInfoContent.Line(Component.literal("Name"), Kind.NAME),
                new HeldItemInfoContent.Line(Component.literal("a\nb\nc\nd"), Kind.DESCRIPTION),
                new HeldItemInfoContent.Line(Component.literal("Extra"), Kind.OTHER));
        var settings = HeldItemInfoSettings.DEFAULT.withMaxLines(3).withMaxDescriptionLines(2);
        var layout = HeldItemInfoLayout.build(content, settings, font, 100, 200);
        assertEquals(3, layout.rows().size());
        assertEquals(4, layout.omittedLines());
        assertEquals(31, layout.height());
        assertEquals(Kind.NAME, layout.rows().getFirst().kind());
    }

    @Test void viewportBudgetAndNoSummaryAreRespected() {
        var content = List.of(new HeldItemInfoContent.Line(Component.literal("abcdefghij"), Kind.OTHER));
        var settings = HeldItemInfoSettings.DEFAULT.withMaxCharacters(2).withShowOmitted(false);
        var layout = HeldItemInfoLayout.build(content, settings, font, 2, 22);
        assertEquals(2, layout.rows().size());
        assertEquals(3, layout.omittedLines());
        assertTrue(layout.width() <= 2);
        assertTrue(layout.height() <= 22);
        assertTrue(HeldItemInfoLayout.build(content, settings, font, 2, 0).rows().isEmpty());
    }
}
