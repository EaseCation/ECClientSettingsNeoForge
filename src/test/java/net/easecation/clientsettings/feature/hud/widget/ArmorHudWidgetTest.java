package net.easecation.clientsettings.feature.hud.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmorHudWidgetTest {

    @Test
    void durabilityColorsCoverHealthyWarningAndCriticalRanges() {
        assertEquals(0xFF55FF55, ArmorHudWidget.durabilityColor(60));
        assertEquals(0xFFFFFF55, ArmorHudWidget.durabilityColor(59));
        assertEquals(0xFFFFFF55, ArmorHudWidget.durabilityColor(25));
        assertEquals(0xFFFF5555, ArmorHudWidget.durabilityColor(24));
    }
}
