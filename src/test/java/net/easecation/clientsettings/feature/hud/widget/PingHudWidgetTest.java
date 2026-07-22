package net.easecation.clientsettings.feature.hud.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingHudWidgetTest {

    @Test
    void missingOrZeroLatencyNeverLooksLikeARealMeasurement() {
        assertEquals("-- ms", PingHudWidget.formatLatency(-1));
        assertEquals("-- ms", PingHudWidget.formatLatency(0));
    }

    @Test
    void positiveLatencyIncludesTheUnit() {
        assertEquals("35 ms", PingHudWidget.formatLatency(35));
    }
}
