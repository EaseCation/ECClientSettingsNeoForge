package net.easecation.clientsettings.window.protocol;

import org.msgpack.value.MapValue;
import org.msgpack.value.Value;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public final class WindowAppearanceParser {

    public static final int MAX_TITLE_CODE_POINTS = 128;

    private WindowAppearanceParser() {
    }

    public static ParseResult<WindowAppearanceCommand> parseSet(Value data) {
        try {
            MapValue root = requireMap(data, "data");
            requireVersion(root);
            Value titleValue = field(root, "title");
            Value frameValue = field(root, "frame");
            TitleAppearance title = titleValue == null ? null : parseTitle(titleValue);
            FrameAppearance frame = frameValue == null ? null : parseFrame(frameValue);
            return ParseResult.success(new WindowAppearanceCommand(title, frame));
        } catch (ProtocolException exception) {
            return ParseResult.failure(exception.getMessage());
        } catch (RuntimeException exception) {
            return ParseResult.failure("Malformed window appearance data");
        }
    }

    public static ParseResult<Boolean> parseReset(Value data) {
        try {
            requireVersion(requireMap(data, "data"));
            return ParseResult.success(Boolean.TRUE);
        } catch (ProtocolException exception) {
            return ParseResult.failure(exception.getMessage());
        } catch (RuntimeException exception) {
            return ParseResult.failure("Malformed window appearance data");
        }
    }

    private static TitleAppearance parseTitle(Value value) throws ProtocolException {
        MapValue title = requireMap(value, "title");
        String mode = requireStringField(title, "mode").toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "custom" -> {
                String text = requireStringField(title, "text");
                validateTitle(text);
                yield TitleAppearance.custom(text);
            }
            case "default" -> {
                if (field(title, "text") != null) {
                    throw new ProtocolException("title.text is not allowed for default mode");
                }
                yield TitleAppearance.defaults();
            }
            default -> throw new ProtocolException("Unsupported title.mode: " + mode);
        };
    }

    private static FrameAppearance parseFrame(Value value) throws ProtocolException {
        MapValue frame = requireMap(value, "frame");
        String mode = requireStringField(frame, "mode").toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "rgb" -> FrameAppearance.rgb(
                    requireColor(frame, "background"),
                    optionalColor(frame, "text"),
                    parseFallback(requireStringField(frame, "fallback"))
            );
            case "dark" -> rejectRgbFields(frame, FrameAppearance.dark());
            case "system" -> rejectRgbFields(frame, FrameAppearance.system());
            default -> throw new ProtocolException("Unsupported frame.mode: " + mode);
        };
    }

    private static FrameAppearance rejectRgbFields(MapValue frame, FrameAppearance appearance) throws ProtocolException {
        if (field(frame, "background") != null || field(frame, "text") != null || field(frame, "fallback") != null) {
            throw new ProtocolException("RGB fields are only allowed for frame.mode=rgb");
        }
        return appearance;
    }

    private static FrameAppearance.Fallback parseFallback(String value) throws ProtocolException {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "dark" -> FrameAppearance.Fallback.DARK;
            case "system" -> FrameAppearance.Fallback.SYSTEM;
            default -> throw new ProtocolException("Unsupported frame.fallback: " + value);
        };
    }

    private static void requireVersion(MapValue root) throws ProtocolException {
        Value version = field(root, "version");
        if (version == null || !version.isIntegerValue() || version.asIntegerValue().toLong() != WindowAppearanceProtocol.VERSION) {
            throw new ProtocolException("Unsupported or missing protocol version");
        }
    }

    private static int requireColor(MapValue map, String name) throws ProtocolException {
        Value value = field(map, name);
        if (value == null || !value.isIntegerValue()) {
            throw new ProtocolException(name + " must be an RGB integer");
        }
        long color = value.asIntegerValue().toLong();
        if (color < 0 || color > 0xFFFFFF) {
            throw new ProtocolException(name + " must be between 0x000000 and 0xFFFFFF");
        }
        return (int) color;
    }

    private static Integer optionalColor(MapValue map, String name) throws ProtocolException {
        return field(map, name) == null ? null : requireColor(map, name);
    }

    private static String requireStringField(MapValue map, String name) throws ProtocolException {
        Value value = field(map, name);
        if (value == null) {
            throw new ProtocolException("Missing " + name);
        }
        return asString(value, name);
    }

    private static MapValue requireMap(Value value, String name) throws ProtocolException {
        if (value == null || !value.isMapValue()) {
            throw new ProtocolException(name + " must be a map");
        }
        return value.asMapValue();
    }

    private static Value field(MapValue map, String name) throws ProtocolException {
        for (Map.Entry<Value, Value> entry : map.map().entrySet()) {
            Value key = entry.getKey();
            if (isStringLike(key) && name.equals(asString(key, "map key"))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String asString(Value value, String name) throws ProtocolException {
        if (value.isStringValue()) {
            return value.asStringValue().asString();
        }
        if (value.isBinaryValue()) {
            try {
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(value.asBinaryValue().asByteArray()))
                        .toString();
            } catch (CharacterCodingException exception) {
                throw new ProtocolException(name + " is not valid UTF-8");
            }
        }
        throw new ProtocolException(name + " must be a string");
    }

    private static boolean isStringLike(Value value) {
        return value.isStringValue() || value.isBinaryValue();
    }

    private static void validateTitle(String title) throws ProtocolException {
        if (title.codePointCount(0, title.length()) > MAX_TITLE_CODE_POINTS) {
            throw new ProtocolException("title.text exceeds " + MAX_TITLE_CODE_POINTS + " code points");
        }
        for (int offset = 0; offset < title.length();) {
            int codePoint = title.codePointAt(offset);
            if (Character.isISOControl(codePoint) || isBidirectionalControl(codePoint)) {
                throw new ProtocolException("title.text contains a disallowed control character");
            }
            offset += Character.charCount(codePoint);
        }
    }

    private static boolean isBidirectionalControl(int codePoint) {
        return codePoint == 0x061C
                || codePoint == 0x200E
                || codePoint == 0x200F
                || codePoint >= 0x202A && codePoint <= 0x202E
                || codePoint >= 0x2066 && codePoint <= 0x2069;
    }

    private static final class ProtocolException extends Exception {
        private ProtocolException(String message) {
            super(message);
        }
    }
}
