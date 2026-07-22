package net.easecation.clientsettings.profile.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.easecation.clientsettings.profile.model.ArgbColor;
import net.easecation.clientsettings.profile.model.BlockOutlineSettings;
import net.easecation.clientsettings.profile.model.ForceSprintSettings;
import net.easecation.clientsettings.profile.model.FullbrightMode;
import net.easecation.clientsettings.profile.model.FullbrightSettings;
import net.easecation.clientsettings.profile.model.HitColorSettings;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudTextColorMode;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.HudWidgetSettings;
import net.easecation.clientsettings.profile.model.HudWidgetStyle;
import net.easecation.clientsettings.profile.model.KeystrokesSettings;
import net.easecation.clientsettings.profile.model.LowFireSettings;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileFeatures;
import net.easecation.clientsettings.profile.model.ProfileIndex;
import net.easecation.clientsettings.profile.model.TimeChangerMode;
import net.easecation.clientsettings.profile.model.TimeChangerSettings;
import net.easecation.clientsettings.profile.model.ZoomActivation;
import net.easecation.clientsettings.profile.model.ZoomSettings;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProfileJsonCodec {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public byte[] encodeIndex(ProfileIndex index) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", index.schemaVersion());
        root.addProperty("activeProfileId", index.activeProfileId());
        JsonArray order = new JsonArray();
        index.profileOrder().forEach(order::add);
        root.add("profileOrder", order);
        return encode(root);
    }

    public ProfileIndex decodeIndex(byte[] serialized) throws IOException {
        JsonObject root = parseObject(serialized, "Profile index");
        checkSchema(root);
        requireOnly(root, "Profile index", Set.of("schemaVersion", "activeProfileId", "profileOrder"));
        JsonArray order = requireArray(root, "profileOrder");
        List<String> profileOrder = new ArrayList<>();
        for (JsonElement element : order) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw invalid("profileOrder must contain strings");
            }
            profileOrder.add(element.getAsString());
        }
        try {
            return new ProfileIndex(
                    ProfileDefinition.CURRENT_SCHEMA_VERSION,
                    requireString(root, "activeProfileId"),
                    profileOrder
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalid("Invalid Profile index", exception);
        }
    }

    public byte[] encodeProfile(ProfileDefinition profile) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", profile.schemaVersion());
        root.addProperty("id", profile.id());
        root.addProperty("name", profile.name());
        root.add("features", encodeFeatures(profile.features()));
        return encode(root);
    }

    public ProfileDefinition decodeProfile(byte[] serialized) throws IOException {
        JsonObject root = parseObject(serialized, "Profile");
        checkSchema(root);
        requireOnly(root, "Profile", Set.of("schemaVersion", "id", "name", "features"));
        try {
            return new ProfileDefinition(
                    ProfileDefinition.CURRENT_SCHEMA_VERSION,
                    requireString(root, "id"),
                    requireString(root, "name"),
                    decodeFeatures(requireObject(root, "features"))
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw invalid("Invalid Profile", exception);
        }
    }

    private JsonObject encodeFeatures(ProfileFeatures features) {
        JsonObject root = new JsonObject();
        root.add("forceSprint", object("enabled", features.forceSprint().enabled()));

        JsonObject blockOutline = object("enabled", features.blockOutline().enabled());
        blockOutline.addProperty("color", features.blockOutline().color().serialized());
        root.add("blockOutline", blockOutline);

        JsonObject lowFire = object("enabled", features.lowFire().enabled());
        lowFire.addProperty("verticalOffset", features.lowFire().verticalOffset());
        root.add("lowFire", lowFire);

        JsonObject fullbright = new JsonObject();
        fullbright.addProperty("mode", features.fullbright().mode().name());
        fullbright.addProperty("strength", features.fullbright().strength());
        root.add("fullbright", fullbright);

        JsonObject timeChanger = new JsonObject();
        timeChanger.addProperty("mode", features.timeChanger().mode().name());
        timeChanger.addProperty("customTime", features.timeChanger().customTime());
        root.add("timeChanger", timeChanger);

        ZoomSettings zoomSettings = features.zoom();
        JsonObject zoom = object("enabled", zoomSettings.enabled());
        zoom.addProperty("activation", zoomSettings.activation().name());
        zoom.addProperty("divisor", zoomSettings.divisor());
        zoom.addProperty("maxDivisor", zoomSettings.maxDivisor());
        zoom.addProperty("animationSpeed", zoomSettings.animationSpeed());
        zoom.addProperty("scrollAdjustment", zoomSettings.scrollAdjustment());
        zoom.addProperty("reduceSensitivity", zoomSettings.reduceSensitivity());
        zoom.addProperty("smoothCamera", zoomSettings.smoothCamera());
        root.add("zoom", zoom);

        JsonObject hitColor = object("enabled", features.hitColor().enabled());
        hitColor.addProperty("color", features.hitColor().color().serialized());
        root.add("hitColor", hitColor);
        root.add("hud", encodeHud(features.hud()));
        return root;
    }

    private ProfileFeatures decodeFeatures(JsonObject root) throws IOException {
        requireOnly(root, "features", Set.of(
                "forceSprint", "blockOutline", "lowFire", "fullbright", "timeChanger", "zoom", "hitColor", "hud"
        ));

        JsonObject forceSprint = requireObject(root, "forceSprint");
        requireOnly(forceSprint, "forceSprint", Set.of("enabled"));

        JsonObject blockOutline = requireObject(root, "blockOutline");
        requireOnly(blockOutline, "blockOutline", Set.of("enabled", "color"));

        JsonObject lowFire = requireObject(root, "lowFire");
        requireOnly(lowFire, "lowFire", Set.of("enabled", "verticalOffset"));

        JsonObject fullbright = requireObject(root, "fullbright");
        requireOnly(fullbright, "fullbright", Set.of("mode", "strength"));

        JsonObject timeChanger = requireObject(root, "timeChanger");
        requireOnly(timeChanger, "timeChanger", Set.of("mode", "customTime"));

        JsonObject zoom = requireObject(root, "zoom");
        requireOnly(zoom, "zoom", Set.of(
                "enabled", "activation", "divisor", "maxDivisor", "animationSpeed",
                "scrollAdjustment", "reduceSensitivity", "smoothCamera"
        ));

        JsonObject hitColor = requireObject(root, "hitColor");
        requireOnly(hitColor, "hitColor", Set.of("enabled", "color"));

        return new ProfileFeatures(
                new ForceSprintSettings(requireBoolean(forceSprint, "enabled")),
                new BlockOutlineSettings(
                        requireBoolean(blockOutline, "enabled"),
                        ArgbColor.parse(requireString(blockOutline, "color"))
                ),
                new LowFireSettings(
                        requireBoolean(lowFire, "enabled"),
                        requireDouble(lowFire, "verticalOffset")
                ),
                new FullbrightSettings(
                        requireEnum(fullbright, "mode", FullbrightMode.class),
                        requireDouble(fullbright, "strength")
                ),
                new TimeChangerSettings(
                        requireEnum(timeChanger, "mode", TimeChangerMode.class),
                        requireInt(timeChanger, "customTime")
                ),
                new ZoomSettings(
                        requireBoolean(zoom, "enabled"),
                        requireEnum(zoom, "activation", ZoomActivation.class),
                        requireDouble(zoom, "divisor"),
                        requireDouble(zoom, "maxDivisor"),
                        requireDouble(zoom, "animationSpeed"),
                        requireBoolean(zoom, "scrollAdjustment"),
                        requireBoolean(zoom, "reduceSensitivity"),
                        requireBoolean(zoom, "smoothCamera")
                ),
                new HitColorSettings(
                        requireBoolean(hitColor, "enabled"),
                        ArgbColor.parse(requireString(hitColor, "color"))
                ),
                decodeHud(requireObject(root, "hud"))
        );
    }

    private JsonObject encodeHud(HudSettings hud) {
        JsonObject root = new JsonObject();
        for (HudWidgetId id : HudWidgetId.values()) {
            HudWidgetSettings settings = hud.widget(id);
            JsonObject widget = object("enabled", settings.enabled());
            widget.addProperty("normalizedX", settings.normalizedX());
            widget.addProperty("normalizedY", settings.normalizedY());
            widget.addProperty("scale", settings.scale());
            widget.add("style", encodeHudStyle(settings.style()));
            if (id == HudWidgetId.KEYSTROKES) {
                widget.add("content", encodeKeystrokes(hud.keystrokes()));
            }
            root.add(id.serializedName(), widget);
        }
        return root;
    }

    private JsonObject encodeKeystrokes(KeystrokesSettings settings) {
        JsonObject root = new JsonObject();
        root.addProperty("showMovement", settings.showMovement());
        root.addProperty("showJump", settings.showJump());
        root.addProperty("showMouseButtons", settings.showMouseButtons());
        root.addProperty("showCps", settings.showCps());
        root.addProperty("keySize", settings.keySize());
        root.addProperty("gap", settings.gap());
        root.addProperty("cornerRadius", settings.cornerRadius());
        root.addProperty("keyBorderWidth", settings.keyBorderWidth());
        root.addProperty("idleBackgroundColor", settings.idleBackgroundColor().serialized());
        root.addProperty("pressedBackgroundColor", settings.pressedBackgroundColor().serialized());
        root.addProperty("idleBorderColor", settings.idleBorderColor().serialized());
        root.addProperty("pressedBorderColor", settings.pressedBorderColor().serialized());
        root.addProperty("pressedTextColor", settings.pressedTextColor().serialized());
        root.addProperty("pressAnimationMillis", settings.pressAnimationMillis());
        return root;
    }

    private JsonObject encodeHudStyle(HudWidgetStyle style) {
        JsonObject root = new JsonObject();
        root.addProperty("backgroundEnabled", style.backgroundEnabled());
        root.addProperty("backgroundColor", style.backgroundColor().serialized());
        root.addProperty("borderEnabled", style.borderEnabled());
        root.addProperty("borderColor", style.borderColor().serialized());
        root.addProperty("borderWidth", style.borderWidth());
        root.addProperty("padding", style.padding());
        root.addProperty("textShadow", style.textShadow());
        root.addProperty("textColorMode", style.textColorMode().name());
        root.addProperty("textColor", style.textColor().serialized());
        root.addProperty("animationSpeed", style.animationSpeed());
        root.addProperty("rainbowSpread", style.rainbowSpread());
        return root;
    }

    private HudSettings decodeHud(JsonObject root) throws IOException {
        requireOnly(root, "hud", Set.of("armor", "potions", "ping", "fps", "keystrokes"));
        Map<HudWidgetId, HudWidgetSettings> widgets = new EnumMap<>(HudWidgetId.class);
        KeystrokesSettings keystrokes = null;
        for (HudWidgetId id : HudWidgetId.values()) {
            String field = id.serializedName();
            JsonObject widget = requireObject(root, field);
            Set<String> fields = id == HudWidgetId.KEYSTROKES
                    ? Set.of("enabled", "normalizedX", "normalizedY", "scale", "style", "content")
                    : Set.of("enabled", "normalizedX", "normalizedY", "scale", "style");
            requireOnly(widget, "hud." + field, fields);
            boolean enabled = requireBoolean(widget, "enabled");
            double normalizedX = requireDouble(widget, "normalizedX");
            double normalizedY = requireDouble(widget, "normalizedY");
            double scale = requireDouble(widget, "scale");
            widgets.put(id, new HudWidgetSettings(
                    enabled, normalizedX, normalizedY, scale,
                    decodeHudStyle(requireObject(widget, "style"))
            ));
            if (id == HudWidgetId.KEYSTROKES) {
                keystrokes = decodeKeystrokes(requireObject(widget, "content"));
            }
        }
        return new HudSettings(widgets, keystrokes);
    }

    private KeystrokesSettings decodeKeystrokes(JsonObject root) throws IOException {
        requireOnly(root, "hud.keystrokes.content", Set.of(
                "showMovement", "showJump", "showMouseButtons", "showCps",
                "keySize", "gap", "cornerRadius", "keyBorderWidth",
                "idleBackgroundColor", "pressedBackgroundColor",
                "idleBorderColor", "pressedBorderColor", "pressedTextColor",
                "pressAnimationMillis"
        ));
        return new KeystrokesSettings(
                requireBoolean(root, "showMovement"),
                requireBoolean(root, "showJump"),
                requireBoolean(root, "showMouseButtons"),
                requireBoolean(root, "showCps"),
                requireInt(root, "keySize"),
                requireInt(root, "gap"),
                requireInt(root, "cornerRadius"),
                requireInt(root, "keyBorderWidth"),
                ArgbColor.parse(requireString(root, "idleBackgroundColor")),
                ArgbColor.parse(requireString(root, "pressedBackgroundColor")),
                ArgbColor.parse(requireString(root, "idleBorderColor")),
                ArgbColor.parse(requireString(root, "pressedBorderColor")),
                ArgbColor.parse(requireString(root, "pressedTextColor")),
                requireInt(root, "pressAnimationMillis")
        );
    }

    private HudWidgetStyle decodeHudStyle(JsonObject root) throws IOException {
        requireOnly(root, "hud.style", Set.of(
                "backgroundEnabled", "backgroundColor", "borderEnabled", "borderColor",
                "borderWidth", "padding", "textShadow", "textColorMode", "textColor",
                "animationSpeed", "rainbowSpread"
        ));
        return new HudWidgetStyle(
                requireBoolean(root, "backgroundEnabled"),
                ArgbColor.parse(requireString(root, "backgroundColor")),
                requireBoolean(root, "borderEnabled"),
                ArgbColor.parse(requireString(root, "borderColor")),
                requireInt(root, "borderWidth"),
                requireInt(root, "padding"),
                requireBoolean(root, "textShadow"),
                requireEnum(root, "textColorMode", HudTextColorMode.class),
                ArgbColor.parse(requireString(root, "textColor")),
                requireDouble(root, "animationSpeed"),
                requireDouble(root, "rainbowSpread")
        );
    }

    private void checkSchema(JsonObject root) throws IOException {
        int schemaVersion = requireInt(root, "schemaVersion");
        if (schemaVersion > ProfileDefinition.CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedProfileSchemaException(schemaVersion);
        }
        if (schemaVersion != ProfileDefinition.CURRENT_SCHEMA_VERSION) {
            throw invalid("Unsupported Profile schema version: " + schemaVersion);
        }
    }

    private static JsonObject parseObject(byte[] serialized, String description) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(new String(serialized, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw invalid(description + " root must be an object");
            }
            return parsed.getAsJsonObject();
        } catch (RuntimeException exception) {
            throw invalid(description + " is not valid JSON", exception);
        }
    }

    private static byte[] encode(JsonObject root) {
        return (GSON.toJson(root) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject object(String property, boolean value) {
        JsonObject object = new JsonObject();
        object.addProperty(property, value);
        return object;
    }

    private static void requireOnly(JsonObject object, String description, Set<String> allowed) throws IOException {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) {
                throw invalid(description + " contains unknown field: " + key);
            }
        }
        for (String key : allowed) {
            if (!object.has(key)) {
                throw invalid(description + " is missing field: " + key);
            }
        }
    }

    private static JsonObject requireObject(JsonObject object, String field) throws IOException {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonObject()) {
            throw invalid(field + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonObject object, String field) throws IOException {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonArray()) {
            throw invalid(field + " must be an array");
        }
        return value.getAsJsonArray();
    }

    private static String requireString(JsonObject object, String field) throws IOException {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw invalid(field + " must be a string");
        }
        return value.getAsString();
    }

    private static boolean requireBoolean(JsonObject object, String field) throws IOException {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw invalid(field + " must be a boolean");
        }
        return value.getAsBoolean();
    }

    private static int requireInt(JsonObject object, String field) throws IOException {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw invalid(field + " must be an integer");
        }
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalid(field + " must be an integer", exception);
        }
    }

    private static double requireDouble(JsonObject object, String field) throws IOException {
        JsonElement value = object.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw invalid(field + " must be a number");
        }
        try {
            BigDecimal decimal = value.getAsBigDecimal();
            return decimal.doubleValue();
        } catch (NumberFormatException exception) {
            throw invalid(field + " must be a number", exception);
        }
    }

    private static <E extends Enum<E>> E requireEnum(JsonObject object, String field, Class<E> type) throws IOException {
        String value = requireString(object, field);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalid(field + " has unsupported value: " + value, exception);
        }
    }

    private static IOException invalid(String message) {
        return new IOException(message);
    }

    private static IOException invalid(String message, Throwable cause) {
        return new IOException(message, cause);
    }
}
