package net.easecation.clientsettings.profile.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileModelTest {

    @Test
    void defaultsMatchTheApprovedSchema() {
        ProfileDefinition profile = ProfileDefinition.defaults(false);

        assertEquals(3, profile.schemaVersion());
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
    void hudDefaultsAreCompleteDisabledAndImmutable() {
        HudSettings hud = HudSettings.DEFAULT;

        assertEquals(HudWidgetId.values().length, hud.widgets().size());
        for (HudWidgetId id : HudWidgetId.values()) {
            assertFalse(hud.widget(id).enabled());
            assertEquals(1.0, hud.widget(id).scale());
        }
        assertEquals(new HudWidgetSettings(
                false, 1.0, 1.0, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.ARMOR)
        ), hud.widget(HudWidgetId.ARMOR));
        assertEquals(new HudWidgetSettings(
                false, 0.0, 0.25, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.POTIONS)
        ), hud.widget(HudWidgetId.POTIONS));
        assertEquals(new HudWidgetSettings(
                false, 1.0, 0.0, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.PING)
        ), hud.widget(HudWidgetId.PING));
        assertEquals(new HudWidgetSettings(
                false, 0.0, 0.0, 1.0, HudWidgetStyle.defaultsFor(HudWidgetId.FPS)
        ), hud.widget(HudWidgetId.FPS));
        assertThrows(
                UnsupportedOperationException.class,
                () -> hud.widgets().put(HudWidgetId.FPS, new HudWidgetSettings(true, 0.5, 0.5, 1.0))
        );
        assertThrows(IllegalArgumentException.class, () -> new HudSettings(Map.of()));
    }

    @Test
    void hudUpdatesOneWidgetAndProfileFeatureCopiesPreserveIt() {
        HudSettings original = HudSettings.DEFAULT;
        HudSettings updated = original
                .withEnabled(HudWidgetId.FPS, true)
                .withLayout(HudWidgetId.FPS, 0.5, 0.6, 1.5);

        assertFalse(original.widget(HudWidgetId.FPS).enabled());
        assertEquals(new HudWidgetSettings(true, 0.5, 0.6, 1.5), updated.widget(HudWidgetId.FPS));
        assertEquals(original.widget(HudWidgetId.ARMOR), updated.widget(HudWidgetId.ARMOR));

        ProfileFeatures features = ProfileFeatures.DEFAULT.withHud(updated);
        assertEquals(updated, features.withForceSprint(true).hud());
        assertEquals(updated, features.withBlockOutline(BlockOutlineSettings.DEFAULT).hud());
        assertEquals(updated, features.withLowFire(LowFireSettings.DEFAULT).hud());
        assertEquals(updated, features.withFullbright(FullbrightSettings.DEFAULT).hud());
        assertEquals(updated, features.withTimeChanger(TimeChangerSettings.DEFAULT).hud());
        assertEquals(updated, features.withZoom(ZoomSettings.DEFAULT).hud());
        assertEquals(updated, features.withHitColor(HitColorSettings.DEFAULT).hud());
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
        assertThrows(IllegalArgumentException.class, () -> new HudWidgetSettings(false, -0.01, 0.5, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new HudWidgetSettings(false, 0.5, 1.01, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new HudWidgetSettings(false, 0.5, 0.5, 0.49));
        assertThrows(IllegalArgumentException.class, () -> new HudWidgetSettings(false, 0.5, 0.5, 3.01));
        assertThrows(IllegalArgumentException.class, () -> new HudWidgetSettings(false, Double.NaN, 0.5, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new HudWidgetStyle(
                true, new ArgbColor(0), false, new ArgbColor(0), 4, 2, true,
                HudTextColorMode.FIXED, new ArgbColor(-1), 0.6, 0.08
        ));
        assertThrows(IllegalArgumentException.class, () -> new HudWidgetStyle(
                true, new ArgbColor(0), false, new ArgbColor(0), 1, 2, true,
                HudTextColorMode.RAINBOW_SWITCH, new ArgbColor(-1), 2.01, 0.08
        ));

        assertTrue(new LowFireSettings(true, 0.0).enabled());
        assertEquals(0.5, new LowFireSettings(true, 0.5).verticalOffset());
        assertEquals(32.0, new ZoomSettings(
                true, ZoomActivation.HOLD, 16.0, 32.0, 10.0, true, true, true
        ).maxDivisor());
        HudWidgetSettings minimum = new HudWidgetSettings(false, 0.0, 0.0, 0.5);
        HudWidgetSettings maximum = new HudWidgetSettings(false, 1.0, 1.0, 3.0);
        assertEquals(0.0, minimum.normalizedX());
        assertEquals(0.5, minimum.scale());
        assertEquals(1.0, maximum.normalizedY());
        assertEquals(3.0, maximum.scale());
    }
}
