package net.easecation.clientsettings.feature.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComfortableGuiScaleTest {
    @Test
    void usesTwoWhenFramebufferShortEdgeDoesNotExceed1440() {
        assertEquals(2, ComfortableGuiScale.resolve(0, 1708, 970, 4));
        assertEquals(2, ComfortableGuiScale.resolve(0, 1920, 1080, 4));
        assertEquals(2, ComfortableGuiScale.resolve(0, 1920, 1200, 4));
        assertEquals(2, ComfortableGuiScale.resolve(0, 2560, 1440, 6));
    }

    @Test
    void usesFourWhenFramebufferShortEdgeExceeds1440() {
        assertEquals(4, ComfortableGuiScale.resolve(0, 2560, 1600, 6));
        assertEquals(4, ComfortableGuiScale.resolve(0, 3456, 1990, 9));
        assertEquals(4, ComfortableGuiScale.resolve(0, 3840, 2160, 9));
        assertEquals(4, ComfortableGuiScale.resolve(0, 2160, 3840, 9));
    }

    @Test
    void ultrawideDisplaysUseTheirShortEdge() {
        assertEquals(2, ComfortableGuiScale.resolve(0, 5120, 1440, 6));
    }

    @Test
    void neverExceedsVanillaSafeScaleForSmallWindows() {
        assertEquals(1, ComfortableGuiScale.resolve(0, 600, 400, 1));
    }

    @Test
    void fixedScalesKeepVanillaResult() {
        for (int scale = 1; scale <= 4; scale++) {
            assertEquals(scale, ComfortableGuiScale.resolve(scale, 3840, 2160, scale));
        }
        assertEquals(4, ComfortableGuiScale.resolve(3, 3840, 2160, 4));
    }
}
