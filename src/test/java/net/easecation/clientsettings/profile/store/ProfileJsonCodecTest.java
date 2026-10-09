package net.easecation.clientsettings.profile.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.easecation.clientsettings.profile.model.DroppedItemSettings;
import net.easecation.clientsettings.profile.model.HeldItemInfoSettings;
import net.easecation.clientsettings.profile.model.HeldItemBackground;
import net.easecation.clientsettings.profile.model.ArgbColor;
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
    void potionSettingsAreIndependentAndOldProfilesGetCompleteDefaults() throws IOException {
        var settings = net.easecation.clientsettings.profile.model.PotionHudSettings.DEFAULT
                .withHideVanilla(true).withVanillaLevel(true).withVanillaTime(true)
                .withVanillaLevelColor(ArgbColor.parse("#80440011")).withVanillaTimeColor(ArgbColor.parse("#CC223344"))
                .withShowName(false).withShowLevel(false).withShowTime(false).withEffectNameColor(true)
                .withLevelColor(ArgbColor.parse("#88446688")).withTimeColor(ArgbColor.parse("#CC668899"))
                .withWarningEnabled(false).withWarningSeconds(35);
        var defaults = ProfileDefinition.defaults(false);
        var hud = defaults.features().hud().withPotions(settings).withEnabled(HudWidgetId.POTIONS, false);
        var profile = defaults.withFeatures(defaults.features().withHud(hud));
        assertEquals(profile, codec.decodeProfile(codec.encodeProfile(profile)));
        assertEquals(settings, hud.withSpeed(hud.speed()).withKeystrokes(hud.keystrokes()).potions());
        assertEquals(settings, hud.withEnabled(HudWidgetId.POTIONS, true).potions());
        JsonObject old = encodedProfile(profile);
        old.getAsJsonObject("features").getAsJsonObject("hud").getAsJsonObject("potions").remove("content");
        assertEquals(net.easecation.clientsettings.profile.model.PotionHudSettings.DEFAULT,
                codec.decodeProfile(bytes(old)).features().hud().potions());
        var partial = JsonParser.parseString("{\"showName\":false}").getAsJsonObject();
        old.getAsJsonObject("features").getAsJsonObject("hud").getAsJsonObject("potions").add("content", partial);
        assertEquals(net.easecation.clientsettings.profile.model.PotionHudSettings.DEFAULT.withShowName(false),
                codec.decodeProfile(bytes(old)).features().hud().potions());
        partial.addProperty("warningSeconds", 0);
        assertThrows(IOException.class, () -> codec.decodeProfile(bytes(old)));
    }

    @Test
    void speedSettingsRoundTripAndOldProfilesDefaultWithoutLosingOtherHudSettings() throws IOException {
        var speed = new net.easecation.clientsettings.profile.model.SpeedHudSettings(false, "速度 {speed}");
        var defaults = ProfileDefinition.defaults(false);
        var hud = defaults.features().hud().withSpeed(speed).withEnabled(HudWidgetId.SPEED, true)
                .withLayout(HudWidgetId.SPEED, 0.4, 0.6, 1.5);
        var profile = defaults.withFeatures(defaults.features().withHud(hud));
        assertEquals(profile, codec.decodeProfile(codec.encodeProfile(profile)));
        assertEquals(speed, hud.withKeystrokes(hud.keystrokes()).speed());
        JsonObject old = encodedProfile(profile);
        old.getAsJsonObject("features").getAsJsonObject("hud").remove("speed");
        var decoded = codec.decodeProfile(bytes(old)).features().hud();
        assertEquals(net.easecation.clientsettings.profile.model.SpeedHudSettings.DEFAULT, decoded.speed());
        assertEquals(HudSettings.DEFAULT.widget(HudWidgetId.SPEED), decoded.widget(HudWidgetId.SPEED));
        assertEquals(hud.widget(HudWidgetId.ARMOR), decoded.widget(HudWidgetId.ARMOR));
    }

    @Test
    void heldItemInformationPersistsAndMissingSettingsKeepVanilla() throws IOException {
        var settings = HeldItemInfoSettings.DEFAULT.withEnabled(true).withShowName(false)
                .withShowDescription(false).withShowEnchantments(false).withShowAdditional(false)
                .withShowOmitted(false).withMaxCharacters(17).withMaxLines(5).withMaxDescriptionLines(2)
                .withLineSpacing(12).withNameGap(4).withVerticalOffset(-10).withBaseSeconds(3)
                .withExtraLineSeconds(1).withBackground(HeldItemBackground.CUSTOM)
                .withBackgroundColor(ArgbColor.parse("#4000FF00")).withChroma(true)
                .withChromaSpeed(0.2).withChromaSaturation(0.5).withChromaBrightness(0.8).withChromaOpacity(0.6);
        var profile = ProfileDefinition.defaults(false).withFeatures(ProfileFeatures.DEFAULT.withHeldItemInfo(settings));
        assertEquals(profile, codec.decodeProfile(codec.encodeProfile(profile)));
        assertEquals(settings, profile.features().withDroppedItems(DroppedItemSettings.DEFAULT).heldItemInfo());
        JsonObject old = encodedProfile(ProfileDefinition.defaults(false));
        old.getAsJsonObject("features").remove("heldItemInfo");
        assertEquals(HeldItemInfoSettings.DEFAULT, codec.decodeProfile(bytes(old)).features().heldItemInfo());
        old.getAsJsonObject("features").add("heldItemInfo", JsonParser.parseString("{\"enabled\":true}").getAsJsonObject());
        assertEquals(HeldItemInfoSettings.DEFAULT.withEnabled(true), codec.decodeProfile(bytes(old)).features().heldItemInfo());
        old.getAsJsonObject("features").getAsJsonObject("heldItemInfo").addProperty("maxCharacters", 0);
        assertThrows(IOException.class, () -> codec.decodeProfile(bytes(old)));
    }

    @Test
    void droppedItemsRoundTripAllCombinationsAndOldProfilesDefaultToVanilla() throws IOException {
        for (boolean physics : new boolean[]{false, true}) {
            for (boolean rotation : new boolean[]{false, true}) {
                for (boolean floating : new boolean[]{false, true}) {
                    var settings = new DroppedItemSettings(physics, rotation, floating);
                    var profile = ProfileDefinition.defaults(false).withFeatures(
                            ProfileFeatures.DEFAULT.withDroppedItems(settings).withForceSprint(false));
                    assertEquals(settings, codec.decodeProfile(codec.encodeProfile(profile)).features().droppedItems());
                    assertEquals(settings, profile.features().withHud(HudSettings.DEFAULT).droppedItems());
                }
            }
        }
        JsonObject old = encodedProfile(ProfileDefinition.defaults(false));
        old.getAsJsonObject("features").remove("droppedItems");
        assertEquals(DroppedItemSettings.DEFAULT,
                codec.decodeProfile(bytes(old)).features().droppedItems());
        old.getAsJsonObject("features").add("droppedItems", JsonParser.parseString("{\"rotation\":false}").getAsJsonObject());
        assertEquals(new DroppedItemSettings(false, false, true),
                codec.decodeProfile(bytes(old)).features().droppedItems());
    }

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
        JsonObject legacyBlockOutline = encodedProfile(profile);
        JsonObject blockOutline = legacyBlockOutline.getAsJsonObject("features").getAsJsonObject("blockOutline");
        blockOutline.remove("fillEnabled");
        blockOutline.remove("fillColor");
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
        assertEquals(
                ProfileFeatures.DEFAULT.blockOutline(),
                codec.decodeProfile(bytes(legacyBlockOutline)).features().blockOutline()
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
