package net.easecation.clientsettings.profile.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileIndex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileJsonCodecTest {

    private final ProfileJsonCodec codec = new ProfileJsonCodec();

    @Test
    void roundTripsProfileAndIndex() throws IOException {
        ProfileDefinition defaults = ProfileDefinition.defaults(false);
        ProfileDefinition profile = defaults
                .withName("  Tournament  ")
                .withFeatures(defaults.features().withHud(
                        defaults.features().hud()
                                .withEnabled(HudWidgetId.ARMOR, true)
                                .withLayout(HudWidgetId.ARMOR, 0.25, 0.75, 1.25)
                ));
        ProfileIndex index = ProfileIndex.defaults();

        assertEquals(profile, codec.decodeProfile(codec.encodeProfile(profile)));
        assertEquals(index, codec.decodeIndex(codec.encodeIndex(index)));
    }

    @Test
    void requiresCurrentSchemaAndCompleteHudData() {
        JsonObject currentWithoutHud = encodedProfile(ProfileDefinition.defaults(false));
        currentWithoutHud.getAsJsonObject("features").remove("hud");
        JsonObject oldSchema = encodedProfile(ProfileDefinition.defaults(false));
        oldSchema.addProperty("schemaVersion", 2);

        assertThrows(IOException.class, () -> codec.decodeProfile(bytes(currentWithoutHud)));
        assertThrows(IOException.class, () -> codec.decodeProfile(bytes(oldSchema)));
    }

    @Test
    void rejectsOutOfRangeHudValues() {
        JsonObject profile = encodedProfile(ProfileDefinition.defaults(false));
        profile.getAsJsonObject("features")
                .getAsJsonObject("hud")
                .getAsJsonObject("fps")
                .addProperty("scale", 3.01);

        assertThrows(IOException.class, () -> codec.decodeProfile(bytes(profile)));
    }

    @Test
    void rejectsUnknownFieldsAndFractionalIntegers() {
        String unknown = new String(codec.encodeProfile(ProfileDefinition.defaults(true)), StandardCharsets.UTF_8)
                .replace("\"name\": \"Default\"", "\"name\": \"Default\", \"future\": true");
        String fractionalSchema = "{\"schemaVersion\":1.5,\"activeProfileId\":\"default\","
                + "\"profileOrder\":[\"default\"]}";

        assertThrows(IOException.class, () -> codec.decodeProfile(unknown.getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> codec.decodeIndex(fractionalSchema.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void distinguishesNewerSchemaFromCorruption() {
        String newer = "{\"schemaVersion\":5,"
                + "\"activeProfileId\":\"default\",\"profileOrder\":[\"default\"]}";

        UnsupportedProfileSchemaException exception = assertThrows(
                UnsupportedProfileSchemaException.class,
                () -> codec.decodeIndex(newer.getBytes(StandardCharsets.UTF_8))
        );
        assertEquals(5, exception.schemaVersion());
    }

    private JsonObject encodedProfile(ProfileDefinition profile) {
        return JsonParser.parseString(new String(codec.encodeProfile(profile), StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static byte[] bytes(JsonObject object) {
        return object.toString().getBytes(StandardCharsets.UTF_8);
    }
}
