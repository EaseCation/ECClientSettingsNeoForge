package net.easecation.clientsettings.feature.hud.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinedCpsHudWidgetTest {

    @Test
    void formatsLeftAndRightCountsInOneCompactLabel() {
        assertEquals("CPS 5 | 0", CombinedCpsHudWidget.format(5, 0));
        assertEquals("CPS 0 | 99+", CombinedCpsHudWidget.format(-1, 100));
    }
}
