package net.easecation.clientsettings.feature.helditeminfo;

import java.util.ArrayList;
import java.util.List;
import net.easecation.clientsettings.profile.model.HeldItemInfoSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import static net.easecation.clientsettings.feature.helditeminfo.HeldItemInfoContent.Kind;

public record HeldItemInfoLayout(List<Row> rows, int width, int height, int omittedLines) {
    public record Row(FormattedCharSequence text, Kind kind, int width) {}

    public static HeldItemInfoLayout build(List<HeldItemInfoContent.Line> content,
            HeldItemInfoSettings settings, Font font, int maxWidth, int maxHeight) {
        List<Row> candidates = new ArrayList<>();
        int descriptionLines = 0;
        int omitted = 0;
        for (var line : content) {
            for (Component part : HeldItemTextWrap.split(line.text(), settings.maxCharacters())) {
                List<FormattedCharSequence> visual = font.split(part, Math.max(1, maxWidth));
                if (visual.isEmpty()) visual = List.of(Component.empty().getVisualOrderText());
                for (var text : visual) {
                    if (line.kind() == Kind.DESCRIPTION && descriptionLines++ >= settings.maxDescriptionLines()) {
                        omitted++;
                    } else {
                        candidates.add(new Row(text, line.kind(), font.width(text)));
                    }
                }
            }
        }
        int capacity = Math.min(settings.maxLines(), Math.max(0,
                (maxHeight - settings.nameGap()) / settings.lineSpacing()));
        if (capacity == 0) return new HeldItemInfoLayout(List.of(), 0, 0, candidates.size() + omitted);
        if (candidates.size() > capacity) {
            omitted += candidates.size() - capacity;
            candidates = new ArrayList<>(candidates.subList(0, capacity));
        }
        if (omitted > 0 && settings.showOmitted()) {
            if (candidates.size() == capacity) { candidates.removeLast(); omitted++; }
            var summary = Component.translatable("format.ecclientsettings.held_item_info.omitted", omitted)
                    .withStyle(net.minecraft.ChatFormatting.GRAY).getVisualOrderText();
            // The summary is also bounded to the viewport, including long translations.
            if (font.width(summary) > maxWidth) {
                summary = font.split(Component.translatable("format.ecclientsettings.held_item_info.omitted", omitted),
                        Math.max(1, maxWidth)).getFirst();
            }
            candidates.add(new Row(summary, Kind.OTHER, font.width(summary)));
        }
        int width = candidates.stream().mapToInt(Row::width).max().orElse(0);
        int height = candidates.isEmpty() ? 0 : (candidates.size() - 1) * settings.lineSpacing() + font.lineHeight;
        boolean gap = candidates.stream().anyMatch(row -> row.kind() == Kind.NAME)
                && candidates.stream().anyMatch(row -> row.kind() != Kind.NAME);
        if (gap) height += settings.nameGap();
        return new HeldItemInfoLayout(List.copyOf(candidates), width, height, omitted);
    }
}
