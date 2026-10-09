package net.easecation.clientsettings.feature.hud.potions;

import net.easecation.clientsettings.profile.model.PotionHudSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PotionWarningTest {
    @Test void warningUsesSecondsStrictThresholdAndExcludesInfiniteEffects() {
        var settings = PotionHudSettings.DEFAULT;
        assertFalse(PotionWarning.active(settings, 400, false, 20));
        assertTrue(PotionWarning.active(settings, 399, false, 20));
        assertFalse(PotionWarning.active(settings, 200, false, 10));
        assertTrue(PotionWarning.active(settings, 199, false, 10));
        assertFalse(PotionWarning.active(settings, -1, true, 20));
        assertFalse(PotionWarning.active(settings, 10, true, 20));
        assertFalse(PotionWarning.active(settings.withWarningEnabled(false), 1, false, 20));
        assertFalse(PotionWarning.active(settings, 1, false, 0));
    }
    @Test void disablingWarningIsFullyOpaqueAndAlphaNeverRevealsTransparentText() {
        var settings = PotionHudSettings.DEFAULT;
        assertEquals(1, PotionWarning.opacity(settings.withWarningEnabled(false), 1, false, 20, 10));
        assertEquals(1, PotionWarning.opacity(settings, 1000, false, 20, 10));
        assertEquals(1, PotionWarning.opacity(settings, 1, false, 20, 0));
        assertEquals(0.3, PotionWarning.opacity(settings, 1, false, 20, 10), 1e-6);
        assertEquals(0x00123456, PotionWarning.fade(0x00123456, 0.5F));
        assertEquals(0x40123456, PotionWarning.fade(0x80123456, 0.5F));
    }
}
