package net.easecation.clientsettings.profile.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.easecation.clientsettings.profile.model.HudSettings;
import net.easecation.clientsettings.profile.model.HudWidgetId;
import net.easecation.clientsettings.profile.model.ProfileDefinition;
import net.easecation.clientsettings.profile.model.ProfileFeatures;
import net.easecation.clientsettings.profile.model.ProfileIndex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void requiresCurrentSchemaButDefaultsMissingFeatureData() throws IOException {
        ProfileDefinition profile = ProfileDefinition.defaults(false);
        JsonObject currentWithoutHud = encodedProfile(profile);
        currentWithoutHud.getAsJsonObject("features").remove("hud");
        JsonObject currentWithoutFeatures = encodedProfile(profile);
        currentWithoutFeatures.remove("features");
        JsonObject oldSchema = encodedProfile(ProfileDefinition.defaults(false));
        oldSchema.addProperty("schemaVersion", 2);

        assertEquals(profile, codec.decodeProfile(bytes(currentWithoutHud)));
        assertEquals(
                new ProfileDefinition(
                        ProfileDefinition.CURRENT_SCHEMA_VERSION,
                        "default",
                        "Default",
                        ProfileFeatures.DEFAULT
                ),
                codec.decodeProfile(bytes(currentWithoutFeatures))
        );
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
    void fillsDefaultsRecursivelyWhilePreservingSparseConfiguredValues() throws IOException {
        ProfileDefinition profile = ProfileDefinition.defaults(false);
        JsonObject encoded = encodedProfile(profile);
        JsonObject features = new JsonObject();
        JsonObject hud = new JsonObject();
        JsonObject combinedCps = new JsonObject();
        combinedCps.addProperty("enabled", true);
        hud.add("combined_cps", combinedCps);
        features.add("hud", hud);
        encoded.add("features", features);

        ProfileDefinition decoded = codec.decodeProfile(bytes(encoded));

        assertEquals(ProfileFeatures.DEFAULT.forceSprint(), decoded.features().forceSprint());
        assertEquals(HudSettings.DEFAULT.widget(HudWidgetId.FPS),
                decoded.features().hud().widget(HudWidgetId.FPS));
        assertTrue(decoded.features().hud().widget(HudWidgetId.COMBINED_CPS).enabled());
        assertEquals(HudSettings.DEFAULT.widget(HudWidgetId.COMBINED_CPS).style(),
                decoded.features().hud().widget(HudWidgetId.COMBINED_CPS).style());
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
    void stillRejectsUnknownNestedFieldsAndInvalidConfiguredTypes() {
        JsonObject unknown = encodedProfile(ProfileDefinition.defaults(false));
        unknown.getAsJsonObject("features").getAsJsonObject("hud").addProperty("combined_cpss", true);
        JsonObject wrongType = encodedProfile(ProfileDefinition.defaults(false));
        wrongType.getAsJsonObject("features").addProperty("hud", true);

        assertThrows(IOException.class, () -> codec.decodeProfile(bytes(unknown)));
        assertThrows(IOException.class, () -> codec.decodeProfile(bytes(wrongType)));
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
