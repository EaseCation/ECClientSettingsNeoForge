package net.easecation.clientsettings.feature.hud.speed;

import net.easecation.clientsettings.feature.hud.widget.SpeedHudWidget;
import net.easecation.clientsettings.profile.model.SpeedHudSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpeedHudFormatTest {
    @Test void supportsLiteralTextUnicodeAndMultiplePlaceholders() {
        assertEquals("4.35 m/s", SpeedHudWidget.format(SpeedHudSettings.DEFAULT, 4.35));
        assertEquals("速度 4.35 / 4.35", SpeedHudWidget.format(new SpeedHudSettings(false, "速度 {speed} / {speed}"), 4.35));
        assertEquals("0.00 m/s", SpeedHudWidget.format(SpeedHudSettings.DEFAULT, -1));
        assertEquals("0.00 m/s", SpeedHudWidget.format(SpeedHudSettings.DEFAULT, Double.NaN));
        assertEquals("1000.00 m/s", SpeedHudWidget.format(SpeedHudSettings.DEFAULT, 1000));
        assertThrows(IllegalArgumentException.class, () -> new SpeedHudSettings(true, "a\nb"));
        assertThrows(IllegalArgumentException.class, () -> new SpeedHudSettings(true, "a".repeat(65)));
        assertDoesNotThrow(() -> new SpeedHudSettings(true, "😀".repeat(64)));
    }
}
