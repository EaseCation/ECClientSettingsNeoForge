package net.easecation.clientsettings.profile.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileModelTest {

    @Test
    void defaultsMatchTheApprovedSchema() {
        ProfileDefinition profile = ProfileDefinition.defaults(false);

        assertEquals("default", profile.id());
        assertFalse(profile.features().forceSprint().enabled());
        assertEquals("#CCFFFFFF", profile.features().blockOutline().color().serialized());
        assertEquals(0.2, profile.features().lowFire().verticalOffset());
        assertEquals(FullbrightMode.OFF, profile.features().fullbright().mode());
        assertEquals(TimeChangerMode.FOLLOW_SERVER, profile.features().timeChanger().mode());
        assertEquals(ZoomActivation.HOLD, profile.features().zoom().activation());
        assertEquals("#80FF0000", profile.features().hitColor().color().serialized());
    }

    @Test
    void colorParsingNormalizesAndPreservesUnsignedArgb() {
        assertEquals("#80ABCDEF", ArgbColor.parse("#80abcdef").serialized());
        assertEquals(0x80ABCDEF, ArgbColor.parse("#80ABCDEF").value());
        assertThrows(IllegalArgumentException.class, () -> ArgbColor.parse("80ABCDEF"));
        assertThrows(IllegalArgumentException.class, () -> ArgbColor.parse("#ABCDEF"));
    }

    @Test
    void validatesNamesAndCanonicalIds() {
        String sixtyFourEmoji = "\uD83D\uDE80".repeat(64);
        assertEquals(sixtyFourEmoji, ProfileDefinition.normalizeName("  " + sixtyFourEmoji + "  "));
        assertThrows(IllegalArgumentException.class, () -> ProfileDefinition.normalizeName("\uD83D\uDE80".repeat(65)));
        assertThrows(IllegalArgumentException.class, () -> ProfileDefinition.normalizeName("   "));
        assertThrows(IllegalArgumentException.class, () -> ProfileDefinition.validateId(
                "62BD28B8-35EE-4CF4-A0EA-BD6637FCA074"
        ));
        assertEquals(
                "62bd28b8-35ee-4cf4-a0ea-bd6637fca074",
                ProfileDefinition.validateId("62bd28b8-35ee-4cf4-a0ea-bd6637fca074")
        );
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeFeatureValues() {
        assertThrows(IllegalArgumentException.class, () -> new LowFireSettings(true, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new LowFireSettings(true, 0.51));
        assertThrows(IllegalArgumentException.class, () -> new FullbrightSettings(FullbrightMode.GAMMA, -0.1));
        assertThrows(IllegalArgumentException.class, () -> new TimeChangerSettings(TimeChangerMode.CUSTOM, 24_000));
        assertThrows(IllegalArgumentException.class, () -> new ZoomSettings(
                true, ZoomActivation.HOLD, 4.0, 3.0, 7.5, false, true, false
        ));
        assertThrows(IllegalArgumentException.class, () -> new ZoomSettings(
                true, ZoomActivation.HOLD, 4.0, 16.0, Double.POSITIVE_INFINITY, false, true, false
        ));

        assertTrue(new LowFireSettings(true, 0.0).enabled());
        assertEquals(32.0, new ZoomSettings(
                true, ZoomActivation.HOLD, 16.0, 32.0, 10.0, true, true, true
        ).maxDivisor());
    }
}
