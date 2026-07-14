package net.easecation.clientsettings.window.protocol;

import org.junit.jupiter.api.Test;
import org.msgpack.value.Value;
import org.msgpack.value.ValueFactory;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WindowAppearanceParserTest {

    @Test
    void matchesOnlyJavaEditionWindowSystem() {
        assertTrue(WindowAppearanceProtocol.isWindowSystem("ECJavaEditionClientMod", "ECClientWindowSystem"));
        assertFalse(WindowAppearanceProtocol.isWindowSystem("ECNukkitClientMod", "ECClientWindowSystem"));
        assertFalse(WindowAppearanceProtocol.isWindowSystem("ECJavaEditionClientMod", "OtherSystem"));
    }

    @Test
    void parsesCompleteAppearance() {
        ParseResult<WindowAppearanceCommand> result = WindowAppearanceParser.parseSet(map(
                "version", 1,
                "title", map("mode", "custom", "text", "EaseCation"),
                "frame", map("mode", "rgb", "background", 0x181818, "text", 0xFFFFFF, "fallback", "dark")
        ));

        assertTrue(result.successful(), result.error());
        assertEquals(TitleAppearance.custom("EaseCation"), result.value().title());
        assertEquals(FrameAppearance.rgb(0x181818, 0xFFFFFF, FrameAppearance.Fallback.DARK), result.value().frame());
    }

    @Test
    void acceptsBinaryKeysAndValues() {
        Value data = ValueFactory.newMap(
                binary("version"), ValueFactory.newInteger(1),
                binary("title"), ValueFactory.newMap(
                        binary("mode"), binary("custom"),
                        binary("text"), binary("JE Client")
                )
        );

        ParseResult<WindowAppearanceCommand> result = WindowAppearanceParser.parseSet(data);

        assertTrue(result.successful(), result.error());
        assertEquals("JE Client", result.value().title().text());
    }

    @Test
    void supportsPartialAndDefaultUpdates() {
        ParseResult<WindowAppearanceCommand> title = WindowAppearanceParser.parseSet(map(
                "version", 1,
                "title", map("mode", "default")
        ));
        ParseResult<WindowAppearanceCommand> frame = WindowAppearanceParser.parseSet(map(
                "version", 1,
                "frame", map("mode", "system")
        ));

        assertEquals(TitleAppearance.defaults(), title.value().title());
        assertNull(title.value().frame());
        assertNull(frame.value().title());
        assertEquals(FrameAppearance.system(), frame.value().frame());
    }

    @Test
    void choosesReadableTextColorWhenOmitted() {
        assertEquals(0xFFFFFF, FrameAppearance.rgb(0x181818, null, FrameAppearance.Fallback.DARK).effectiveTextColor());
        assertEquals(0x000000, FrameAppearance.rgb(0xEEEEEE, null, FrameAppearance.Fallback.SYSTEM).effectiveTextColor());
    }

    @Test
    void acceptsResetWithUnknownForwardCompatibleFields() {
        ParseResult<Boolean> result = WindowAppearanceParser.parseReset(map(
                "version", 1,
                "future", "ignored"
        ));

        assertTrue(result.successful(), result.error());
    }

    @Test
    void rejectsUnknownVersionAndEmptyCommand() {
        assertFalse(WindowAppearanceParser.parseSet(map("version", 2, "title", map("mode", "default"))).successful());
        assertFalse(WindowAppearanceParser.parseSet(map("version", 1, "future", true)).successful());
    }

    @Test
    void rejectsInvalidRgbFields() {
        assertFalse(WindowAppearanceParser.parseSet(map(
                "version", 1,
                "frame", map("mode", "rgb", "background", 0x181818)
        )).successful());
        assertFalse(WindowAppearanceParser.parseSet(map(
                "version", 1,
                "frame", map("mode", "rgb", "background", 0x1000000, "fallback", "dark")
        )).successful());
        assertFalse(WindowAppearanceParser.parseSet(map(
                "version", 1,
                "frame", map("mode", "dark", "background", 0)
        )).successful());
    }

    @Test
    void rejectsUnsafeAndOverlongTitles() {
        String longTitle = "x".repeat(WindowAppearanceParser.MAX_TITLE_CODE_POINTS + 1);
        assertFalse(parseTitle("line one\nline two").successful());
        assertFalse(parseTitle("safe\u202Eunsafe").successful());
        assertFalse(parseTitle(longTitle).successful());
    }

    private static ParseResult<WindowAppearanceCommand> parseTitle(String title) {
        return WindowAppearanceParser.parseSet(map(
                "version", 1,
                "title", map("mode", "custom", "text", title)
        ));
    }

    private static Value map(Object... entries) {
        Value[] values = new Value[entries.length];
        for (int index = 0; index < entries.length; index++) {
            values[index] = value(entries[index]);
        }
        return ValueFactory.newMap(values);
    }

    private static Value value(Object value) {
        if (value instanceof Value msgpackValue) {
            return msgpackValue;
        }
        if (value instanceof String string) {
            return ValueFactory.newString(string);
        }
        if (value instanceof Integer integer) {
            return ValueFactory.newInteger(integer);
        }
        if (value instanceof Boolean bool) {
            return ValueFactory.newBoolean(bool);
        }
        throw new IllegalArgumentException("Unsupported test value: " + value);
    }

    private static Value binary(String value) {
        return ValueFactory.newBinary(value.getBytes(StandardCharsets.UTF_8));
    }
}
